package com.example.calendar.util

import com.example.calendar.data.Event
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

/**
 * 基于 RFC5545 的 iCalendar 导入工具。
 * 
 * 支持解析 .ics 文件中的 VEVENT 组件，提取核心字段并转换为 Event 实体。
 * 处理时区转换、文本转义字符解析等。
 */
object IcsImporter {

    private val utcFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'", Locale.US)
            .withZone(ZoneId.of("UTC"))

    private val localFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss", Locale.US)

    /**
     * 解析 iCalendar 文本内容，提取所有 VEVENT 组件。
     * 
     * @param icsContent .ics 文件的文本内容
     * @return 解析后的事件列表
     * @throws IcsParseException 当解析失败时抛出异常
     */
    fun parse(icsContent: String): List<Event> {
        val events = mutableListOf<Event>()
        val lines = icsContent.lines()
        
        var i = 0
        while (i < lines.size) {
            // 查找 BEGIN:VEVENT
            if (lines[i].trim().equals("BEGIN:VEVENT", ignoreCase = true)) {
                val eventLines = mutableListOf<String>()
                i++ // 跳过 BEGIN:VEVENT
                
                // 收集直到 END:VEVENT 的所有行
                while (i < lines.size && !lines[i].trim().equals("END:VEVENT", ignoreCase = true)) {
                    // 处理 RFC5545 的续行规则（以空格或制表符开头的行是上一行的续行）
                    val line = lines[i].trim()
                    if (line.isNotEmpty()) {
                        if (i > 0 && eventLines.isNotEmpty() && 
                            (lines[i].startsWith(" ") || lines[i].startsWith("\t"))) {
                            // 续行：追加到上一行
                            eventLines[eventLines.size - 1] += line.substring(1)
                        } else {
                            eventLines.add(line)
                        }
                    }
                    i++
                }
                
                // 解析这个 VEVENT
                try {
                    val event = parseEvent(eventLines)
                    events.add(event)
                } catch (e: Exception) {
                    // 记录错误但继续解析其他事件
                }
            }
            i++
        }
        
        return events
    }

    /**
     * 解析单个 VEVENT 组件。
     */
    private fun parseEvent(lines: List<String>): Event {
        var uid: String? = null
        var summary: String? = null
        var description: String? = null
        var location: String? = null
        var dtStart: Long? = null
        var dtEnd: Long? = null
        var created: Long? = null
        var lastModified: Long? = null
        var timezone: String = java.time.ZoneId.systemDefault().id
        var reminderMinutes: Int? = null

        // 解析 VALARM 组件中的提醒信息
        var inValarm = false
        var valarmLines = mutableListOf<String>()

        for (line in lines) {
            val trimmedLine = line.trim()
            
            // 检测 VALARM 开始和结束
            if (trimmedLine.equals("BEGIN:VALARM", ignoreCase = true)) {
                inValarm = true
                valarmLines.clear()
                continue
            }
            if (trimmedLine.equals("END:VALARM", ignoreCase = true)) {
                inValarm = false
                // 解析 VALARM 中的提醒信息
                reminderMinutes = parseValarm(valarmLines)
                valarmLines.clear()
                continue
            }
            
            if (inValarm) {
                valarmLines.add(trimmedLine)
                continue
            }

            val colonIndex = line.indexOf(':')
            if (colonIndex <= 0) continue
            
            val key = line.substring(0, colonIndex).uppercase()
            val value = line.substring(colonIndex + 1)

            when {
                key.startsWith("UID") -> {
                    uid = unescapeText(value)
                }
                key.startsWith("SUMMARY") -> {
                    summary = unescapeText(value)
                }
                key.startsWith("DESCRIPTION") -> {
                    description = unescapeText(value)
                }
                key.startsWith("LOCATION") -> {
                    location = unescapeText(value)
                }
                key.startsWith("DTSTART") -> {
                    dtStart = parseDateTime(key, value)
                    // 从 DTSTART 中提取时区信息
                    if (key.contains("TZID=")) {
                        val tzidStart = key.indexOf("TZID=") + 5
                        val tzidEnd = key.indexOf(":", tzidStart)
                        if (tzidEnd > tzidStart) {
                            timezone = key.substring(tzidStart, tzidEnd)
                        }
                    }
                }
                key.startsWith("DTEND") -> {
                    dtEnd = parseDateTime(key, value)
                }
                key.startsWith("CREATED") -> {
                    created = parseDateTime(key, value)
                }
                key.startsWith("LAST-MODIFIED") -> {
                    lastModified = parseDateTime(key, value)
                }
            }
        }

        // 验证必需字段
        if (uid == null || uid.isBlank()) {
            uid = UUID.randomUUID().toString()
        }
        if (summary == null || summary.isBlank()) {
            throw IcsParseException("SUMMARY is required")
        }
        if (dtStart == null) {
            throw IcsParseException("DTSTART is required")
        }
        if (dtEnd == null) {
            // 如果没有 DTEND，使用 DTSTART + 1小时作为默认值
            dtEnd = dtStart + 3600_000L
        }

        val now = System.currentTimeMillis()
        return Event(
            uid = uid,
            summary = summary,
            description = description?.takeIf { it.isNotBlank() },
            dtStart = dtStart,
            dtEnd = dtEnd,
            location = location?.takeIf { it.isNotBlank() },
            timezone = timezone,
            reminderMinutes = reminderMinutes, // 从 VALARM 中解析的提醒信息
            created = created ?: now,
            lastModified = lastModified ?: now
        )
    }

    /**
     * 解析 VALARM 组件，提取提醒时间。
     * 支持 RFC5545 的 TRIGGER 格式，如 -PT15M（提前15分钟）、-PT1H（提前1小时）等。
     * 
     * @param valarmLines VALARM 组件的行列表
     * @return 提醒提前的分钟数，如果解析失败或没有提醒则返回 null
     */
    private fun parseValarm(valarmLines: List<String>): Int? {
        for (line in valarmLines) {
            val colonIndex = line.indexOf(':')
            if (colonIndex <= 0) continue
            
            val key = line.substring(0, colonIndex).uppercase()
            val value = line.substring(colonIndex + 1)
            
            // 查找 TRIGGER 字段
            if (key.startsWith("TRIGGER")) {
                return parseTrigger(value)
            }
        }
        return null
    }

    /**
     * 解析 TRIGGER 值，转换为提前的分钟数。
     * 支持格式：
     * - -PT15M (提前15分钟)
     * - -PT1H (提前1小时)
     * - -P1D (提前1天)
     * - -PT1H30M (提前1小时30分钟)
     */
    private fun parseTrigger(trigger: String): Int? {
        try {
            // TRIGGER 格式：-PT15M 或 -PT1H 或 -P1D
            // 负号表示提前，P 表示周期，T 表示时间部分
            if (!trigger.startsWith("-")) {
                // 如果不是提前（负值），则忽略
                return null
            }
            
            val triggerValue = trigger.substring(1) // 去掉负号
            
            // 解析 ISO 8601 持续时间格式
            // 格式：PT15M, PT1H, P1D, PT1H30M
            var totalMinutes = 0
            
            // 提取天数
            val dayMatch = Regex("P(\\d+)D").find(triggerValue)
            if (dayMatch != null) {
                val days = dayMatch.groupValues[1].toInt()
                totalMinutes += days * 24 * 60
            }
            
            // 提取小时（必须在 T 之后，H 之前）
            val hourMatch = Regex("T(\\d+)H").find(triggerValue)
            if (hourMatch != null) {
                val hours = hourMatch.groupValues[1].toInt()
                totalMinutes += hours * 60
            }
            
            // 提取分钟（必须在 T 之后，且不在 H 之后）
            // 使用更精确的正则：T 之后，H 之后（如果有），然后是数字和 M
            val minuteMatch = if (hourMatch != null) {
                // 如果有小时，分钟应该在 H 之后
                Regex("H(\\d+)M").find(triggerValue)
            } else {
                // 如果没有小时，分钟应该在 T 之后
                Regex("T(\\d+)M").find(triggerValue)
            }
            if (minuteMatch != null) {
                val minutes = minuteMatch.groupValues[1].toInt()
                totalMinutes += minutes
            }
            
            return if (totalMinutes > 0) totalMinutes else null
        } catch (e: Exception) {
            // 解析失败时返回 null
            return null
        }
    }

    /**
     * 解析日期时间字符串。
     * 支持 UTC 格式（带 Z）和本地时间格式。
     */
    private fun parseDateTime(key: String, value: String): Long {
        return try {
            // 检查是否包含时区信息
            if (key.contains("TZID=")) {
                // 提取时区ID，例如：DTSTART;TZID=Asia/Shanghai:20231219T140000
                val tzidStart = key.indexOf("TZID=") + 5
                val tzidEnd = key.indexOf(":", tzidStart)
                if (tzidEnd > tzidStart) {
                    val tzid = key.substring(tzidStart, tzidEnd)
                    val zoneId = ZoneId.of(tzid)
                    val localDateTime = LocalDateTime.parse(value, localFormatter)
                    val zonedDateTime = ZonedDateTime.of(localDateTime, zoneId)
                    zonedDateTime.toInstant().toEpochMilli()
                } else {
                    // 回退到系统默认时区
                    val localDateTime = LocalDateTime.parse(value, localFormatter)
                    localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                }
            } else if (value.endsWith("Z")) {
                // UTC 时间
                utcFormatter.parse(value, Instant::from).toEpochMilli()
            } else {
                // 本地时间（假设为系统默认时区）
                val localDateTime = LocalDateTime.parse(value, localFormatter)
                localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        } catch (e: Exception) {
            throw IcsParseException("Failed to parse datetime: $value", e)
        }
    }

    /**
     * 反转义文本字段。
     * RFC5545 要求对反斜杠、逗号、分号和换行进行转义。
     */
    private fun unescapeText(value: String?): String? {
        if (value.isNullOrBlank()) return null
        return value
            .replace("\\n", "\n")
            .replace("\\,", ",")
            .replace("\\;", ";")
            .replace("\\\\", "\\")
    }

    /**
     * 导入异常类。
     */
    class IcsParseException(message: String, cause: Throwable? = null) : Exception(message, cause)
}

