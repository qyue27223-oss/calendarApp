package com.example.calendar.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 与时间、时区相关的通用扩展函数，集中管理，避免在各个 UI 文件中重复实现。
 */

fun Long.toLocalTime(timezoneId: String): LocalTime {
    val zoneId = try {
        ZoneId.of(timezoneId)
    } catch (_: Exception) {
        ZoneId.systemDefault()
    }
    return Instant.ofEpochMilli(this)
        .atZone(zoneId)
        .toLocalTime()
}

fun LocalDateTime.toMillis(zoneId: ZoneId = ZoneId.systemDefault()): Long {
    return this.atZone(zoneId).toInstant().toEpochMilli()
}

/**
 * 将 LocalDateTime 转换为时间戳（毫秒），使用指定的时区ID字符串
 * @param timezoneId 时区ID，如 "Asia/Shanghai"，如果为空或无效则使用系统默认时区
 */
fun LocalDateTime.toMillis(timezoneId: String): Long {
    val zoneId = try {
        ZoneId.of(timezoneId)
    } catch (_: Exception) {
        ZoneId.systemDefault()
    }
    return this.atZone(zoneId).toInstant().toEpochMilli()
}

/**
 * 计算一周的开始日期（周一为一周开始）
 */
fun LocalDate.startOfWeek(): LocalDate {
    val diff = when (this.dayOfWeek) {
        DayOfWeek.MONDAY -> 0
        DayOfWeek.TUESDAY -> -1
        DayOfWeek.WEDNESDAY -> -2
        DayOfWeek.THURSDAY -> -3
        DayOfWeek.FRIDAY -> -4
        DayOfWeek.SATURDAY -> -5
        DayOfWeek.SUNDAY -> -6
    }
    return this.plusDays(diff.toLong())
}

/**
 * 统一的时间格式化器常量
 */
object TimeFormatters {
    val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
    val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
}

/**
 * 格式化时间为 HH:mm
 */
fun LocalDateTime.formatTime(): String = this.format(TimeFormatters.TIME_FORMATTER)

/**
 * 格式化日期为 yyyy/MM/dd
 */
fun LocalDateTime.formatDate(): String = this.format(TimeFormatters.DATE_FORMATTER)

/**
 * 格式化日期为 yyyy/MM/dd
 */
fun LocalDate.formatDate(): String = this.format(TimeFormatters.DATE_FORMATTER)

/**
 * 格式化中文日期（如"今天12月25日"）
 */
fun LocalDate.formatChineseDate(): String {
    val today = LocalDate.now()
    return when {
        this == today -> "今天${this.monthValue}月${this.dayOfMonth}日"
        else -> "${this.year}年${this.monthValue}月${this.dayOfMonth}日"
    }
}

