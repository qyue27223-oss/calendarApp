package com.example.calendar.data

import com.example.calendar.reminder.ReminderScheduler
import com.example.calendar.util.IcsImporter
import com.example.calendar.util.toZoneIdSafe
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class ImportResult(
    val total: Int,
    val imported: Int,
    val skipped: Int,
    val errors: List<String>
)

class EventRepository(
    private val eventDao: EventDao,
    private val reminderDao: ReminderDao,
    private val reminderScheduler: ReminderScheduler
) {
    
    /**
     * 提取事件的 UID 前缀（用于识别重复事件组）
     */
    private fun getBaseUid(uid: String): String {
        return uid.split("_").firstOrNull() ?: uid
    }

    fun getAllEvents(): Flow<List<Event>> = eventDao.getAllEvents()

    fun getEventById(id: Long): Flow<Event?> = eventDao.getEventById(id)

    fun getEventsBetween(startTime: Long, endTime: Long): Flow<List<Event>> =
        eventDao.getEventsBetween(startTime, endTime)

    fun getRemindersForEvent(eventId: Long): Flow<List<Reminder>> =
        reminderDao.getRemindersForEvent(eventId)

    /**
     * 保存或更新事件，支持重复日程生成
     * 
     * @param event 事件对象
     * @param reminderMinutes 提醒提前分钟数
     * @param repeatCount 重复次数（0=仅一次，1=每天，7=每周，30=每月）
     * @return 主事件ID
     */
    suspend fun upsertEventWithReminder(
        event: Event,
        reminderMinutes: Int?,
        repeatCount: Int = 0
    ): Long {
        val zoneId = event.timezone.toZoneIdSafe()
        val startDateTime = Instant.ofEpochMilli(event.dtStart).atZone(zoneId).toLocalDateTime()
        val endDateTime = Instant.ofEpochMilli(event.dtEnd).atZone(zoneId).toLocalDateTime()
        val duration = ChronoUnit.MINUTES.between(startDateTime, endDateTime)
        
        // 确定重复类型
        val repeatType = when (repeatCount) {
            0 -> RepeatType.NONE
            1 -> RepeatType.DAILY
            7 -> RepeatType.WEEKLY
            30 -> RepeatType.MONTHLY
            else -> RepeatType.NONE
        }
        
        val eventWithRepeat = event.copy(repeatType = repeatType)
        
        // 如果是编辑模式，先删除所有相关的重复事件
        if (event.id != 0L) {
            // 查找所有相关的重复事件（通过 UID 前缀匹配）
            val existingEvents = eventDao.getAllEventsOnce()
            val baseUid = getBaseUid(event.uid)
            val relatedEvents = existingEvents.filter { 
                (it.uid == event.uid || it.uid.startsWith("${baseUid}_")) && it.id != event.id
            }
            relatedEvents.forEach { relatedEvent ->
                reminderDao.deleteByEventId(relatedEvent.id)
                reminderScheduler.cancelReminder(relatedEvent.id)
                eventDao.deleteEvent(relatedEvent)
            }
        }
        
        // 生成重复事件列表
        val eventsToSave = if (repeatType == RepeatType.NONE) {
            listOf(eventWithRepeat)
        } else {
            generateRepeatEvents(eventWithRepeat, startDateTime, duration, repeatType, zoneId)
        }
        
        // 保存所有事件并获取保存后的事件对象
        val savedEvents = mutableListOf<Event>()
        
        if (event.id != 0L) {
            // 编辑模式：更新主事件
            val updatedEvent = eventsToSave[0].copy(id = event.id)
            eventDao.updateEvent(updatedEvent)
            savedEvents.add(updatedEvent)
            
            // 插入新的重复事件（如果有）
            if (eventsToSave.size > 1) {
                val newEvents = eventsToSave.drop(1).map { it.copy(id = 0L) }
                val insertedIds = eventDao.insertEvents(newEvents)
                // 将插入的事件添加到保存列表（使用插入后的ID）
                newEvents.forEachIndexed { index, newEvent ->
                    savedEvents.add(newEvent.copy(id = insertedIds[index]))
                }
            }
        } else {
            // 新建模式：插入所有事件
            val insertedIds = eventDao.insertEvents(eventsToSave)
            // 将插入的事件添加到保存列表（使用插入后的ID）
            eventsToSave.forEachIndexed { index, eventToSave ->
                savedEvents.add(eventToSave.copy(id = insertedIds[index]))
            }
        }
        
        // 为每个事件设置提醒
        savedEvents.forEach { savedEvent ->
            val eventId = savedEvent.id
            // 清理旧的提醒记录与系统闹钟
            reminderDao.deleteByEventId(eventId)
            reminderScheduler.cancelReminder(eventId)
            
            // 设置提醒
            if (reminderMinutes != null) {
                val triggerTime = savedEvent.dtStart - reminderMinutes * 60_000L
                val reminder = Reminder(
                    eventId = eventId,
                    reminderTime = triggerTime
                )
                reminderDao.insertReminder(reminder)
                
                // 调度系统提醒
                reminderScheduler.scheduleReminder(savedEvent, triggerTime)
            }
        }
        
        return savedEvents[0].id
    }
    
    /**
     * 生成重复事件列表
     * 
     * @param baseEvent 基础事件
     * @param startDateTime 开始时间
     * @param duration 持续时间（分钟）
     * @param repeatType 重复类型
     * @param zoneId 时区
     * @return 重复事件列表（最多生成365天的重复事件）
     */
    private fun generateRepeatEvents(
        baseEvent: Event,
        startDateTime: LocalDateTime,
        duration: Long,
        repeatType: RepeatType,
        zoneId: ZoneId
    ): List<Event> {
        val events = mutableListOf<Event>()
        val baseUid = getBaseUid(baseEvent.uid)
        var currentDateTime = startDateTime
        val maxEvents = 365 // 最多生成365个重复事件
        
        repeat(maxEvents) { index ->
            val startMillis = currentDateTime.atZone(zoneId).toInstant().toEpochMilli()
            val endMillis = currentDateTime.plusMinutes(duration).atZone(zoneId).toInstant().toEpochMilli()
            
            val event = baseEvent.copy(
                uid = if (index == 0) baseEvent.uid else "${baseUid}_${index}",
                dtStart = startMillis,
                dtEnd = endMillis,
                created = if (index == 0) baseEvent.created else System.currentTimeMillis(),
                lastModified = System.currentTimeMillis()
            )
            events.add(event)
            
            // 计算下一个重复时间
            currentDateTime = when (repeatType) {
                RepeatType.DAILY -> currentDateTime.plusDays(1)
                RepeatType.WEEKLY -> currentDateTime.plusWeeks(1)
                RepeatType.MONTHLY -> currentDateTime.plusMonths(1)
                RepeatType.NONE -> return@repeat // 不会执行到这里
            }
        }
        
        return events
    }

    suspend fun deleteEventWithReminders(event: Event) {
        // 如果是重复事件，删除所有相关的重复事件
        // 通过 UID 前缀匹配找到所有相关的重复事件
        val baseUid = getBaseUid(event.uid)
        val allEvents = eventDao.getAllEventsOnce()
        
        // 找到所有相关的重复事件（UID 相同或以前缀开头）
        val relatedEvents = allEvents.filter { 
            getBaseUid(it.uid) == baseUid
        }
        
        // 删除所有相关事件的提醒和系统闹钟
        relatedEvents.forEach { relatedEvent ->
            reminderDao.deleteByEventId(relatedEvent.id)
            reminderScheduler.cancelReminder(relatedEvent.id)
            eventDao.deleteEvent(relatedEvent)
        }
    }

    /**
     * 从 ICS 内容导入事件。
     * 
     * @param icsContent ICS 文件内容
     * @param onConflict 冲突处理策略：true 表示覆盖，false 表示跳过
     * @return 导入结果
     */
    suspend fun importEventsFromIcs(
        icsContent: String,
        onConflict: Boolean = true
    ): ImportResult {
        val errors = mutableListOf<String>()
        var imported = 0
        var skipped = 0

        try {
            val parsedEvents = IcsImporter.parse(icsContent)
            
            for (event in parsedEvents) {
                try {
                    // 检查 UID 冲突
                    val existingEvent = eventDao.getEventByUid(event.uid)
                    
                    if (existingEvent != null) {
                        if (onConflict) {
                            // 覆盖：更新现有事件（保留原有的 id）
                            val updatedEvent = event.copy(id = existingEvent.id)
                            eventDao.updateEvent(updatedEvent)
                            imported++
                        } else {
                            // 跳过：不导入
                            skipped++
                        }
                    } else {
                        // 新事件：直接插入
                        eventDao.insertEvent(event)
                        imported++
                    }
                } catch (e: Exception) {
                    errors.add("导入事件失败 (UID: ${event.uid}): ${e.message}")
                }
            }

            return ImportResult(
                total = parsedEvents.size,
                imported = imported,
                skipped = skipped,
                errors = errors
            )
        } catch (e: IcsImporter.IcsParseException) {
            errors.add("ICS 解析失败: ${e.message}")
            return ImportResult(
                total = 0,
                imported = 0,
                skipped = 0,
                errors = errors
            )
        } catch (e: Exception) {
            errors.add("导入失败: ${e.message}")
            return ImportResult(
                total = 0,
                imported = 0,
                skipped = 0,
                errors = errors
            )
        }
    }
}


