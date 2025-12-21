package com.example.calendar.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

/**
 * 重复类型枚举
 */
enum class RepeatType(val days: Int) {
    NONE(0),      // 仅一次
    DAILY(1),     // 每天
    WEEKLY(7),    // 每周
    MONTHLY(30)   // 每月
}

@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uid: String,              // RFC5545: UID
    val summary: String,          // RFC5545: SUMMARY (标题)
    val description: String? = null, // RFC5545: DESCRIPTION (描述)
    val dtStart: Long,            // RFC5545: DTSTART (开始时间，时间戳)
    val dtEnd: Long,              // RFC5545: DTEND (结束时间，时间戳)
    val location: String? = null, // RFC5545: LOCATION (地点)
    val timezone: String = "Asia/Shanghai", // 时区
    val reminderMinutes: Int? = null, // 提醒时间（提前多少分钟）
    val eventType: EventType = EventType.NORMAL, // 日程类型
    val repeatType: RepeatType = RepeatType.NONE, // 重复类型
    val hasAlarm: Boolean = false, // 是否响铃提醒
    val created: Long = System.currentTimeMillis(), // RFC5545: CREATED
    val lastModified: Long = System.currentTimeMillis() // RFC5545: LAST-MODIFIED
)


