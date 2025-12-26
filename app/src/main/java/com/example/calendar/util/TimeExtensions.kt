package com.example.calendar.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * 与时间、时区相关的通用扩展函数，集中管理，避免在各个 UI 文件中重复实现。
 */

/**
 * 安全地解析时区ID字符串，如果无效则使用系统默认时区
 * @param timezoneId 时区ID字符串，如 "Asia/Shanghai"
 * @return ZoneId对象
 */
fun String.toZoneIdSafe(): ZoneId {
    return try {
        ZoneId.of(this)
    } catch (_: Exception) {
        ZoneId.systemDefault()
    }
}

fun Long.toLocalTime(timezoneId: String): LocalTime {
    val zoneId = timezoneId.toZoneIdSafe()
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
    val zoneId = timezoneId.toZoneIdSafe()
    return this.atZone(zoneId).toInstant().toEpochMilli()
}

/**
 * 将时间戳转换为LocalDate，使用指定的时区ID字符串
 * @param timezoneId 时区ID字符串
 * @return LocalDate对象
 */
fun Long.toLocalDate(timezoneId: String): java.time.LocalDate {
    val zoneId = timezoneId.toZoneIdSafe()
    return Instant.ofEpochMilli(this)
        .atZone(zoneId)
        .toLocalDate()
}

/**
 * 计算一周的开始日期（周日为一周开始）
 */
fun LocalDate.startOfWeek(): LocalDate {
    val diff = when (this.dayOfWeek) {
        DayOfWeek.SUNDAY -> 0
        DayOfWeek.MONDAY -> -1
        DayOfWeek.TUESDAY -> -2
        DayOfWeek.WEDNESDAY -> -3
        DayOfWeek.THURSDAY -> -4
        DayOfWeek.FRIDAY -> -5
        DayOfWeek.SATURDAY -> -6
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

/**
 * 计算日期所在的一年中的周数
 * 使用周日作为一周的开始，确保跨年时正确显示当前年份的周数
 */
fun LocalDate.getWeekNumber(): Int {
    // 获取该日期所在周的起始日期（周日）
    val weekStart = this.startOfWeek()
    
    // 找到当前年的第一个周日（1月1日所在周的周日）
    val yearStart = LocalDate.of(this.year, 1, 1)
    val firstSundayOfYear = yearStart.startOfWeek()
    
    // 如果周起始日期属于上一年，说明这是当前年的第1周
    if (weekStart.year < this.year) {
        return 1
    }
    
    // 如果周起始日期属于下一年，说明这是当前年的最后一周
    if (weekStart.year > this.year) {
        // 需要计算该年总共有多少周
        // 找到该年最后一个属于该年的周日（12月31日所在周的起始日期如果在下一年，则减去7天）
        val yearEnd = LocalDate.of(this.year, 12, 31)
        val lastWeekStart = yearEnd.startOfWeek()
        val lastSundayOfYear = if (lastWeekStart.year > this.year) {
            lastWeekStart.minusDays(7)
        } else {
            lastWeekStart
        }
        
        // 计算从第一个周日到最后一个周日的总周数
        val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(firstSundayOfYear, lastSundayOfYear).toInt()
        return (daysBetween / 7) + 1
    }
    
    // 正常情况下（weekStart.year == this.year），计算从该年第一个周日到当前周起始日期的周数
    val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(firstSundayOfYear, weekStart).toInt()
    // daysBetween应该总是非负的，因为weekStart >= firstSundayOfYear
    val weekNumber = (daysBetween / 7) + 1
    return weekNumber.coerceAtLeast(1)
}

