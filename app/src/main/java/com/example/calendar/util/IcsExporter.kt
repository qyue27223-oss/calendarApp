package com.example.calendar.util

import com.example.calendar.data.Event
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 基于 RFC5545 的简化版 iCalendar 导出工具。
 *
 * 当前版本仅导出 VEVENT 的核心字段：UID、DTSTART、DTEND、SUMMARY、DESCRIPTION、LOCATION、
 * CREATED、LAST-MODIFIED，时间统一以 UTC（带 Z）导出，便于跨平台解析。
 */
object IcsExporter {

    private val utcFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'", Locale.US)
            .withZone(ZoneId.of("UTC"))

    private fun formatEpochMillisUtc(millis: Long): String {
        return utcFormatter.format(Instant.ofEpochMilli(millis))
    }

    private fun escapeText(value: String?): String? {
        if (value.isNullOrBlank()) return null
        // RFC5545 要求对反斜杠、逗号、分号和换行进行转义
        return value
            .replace("\\", "\\\\")
            .replace("\n", "\\n")
            .replace(",", "\\,")
            .replace(";", "\\;")
    }

    /**
     * 将一组事件导出为完整的 VCALENDAR 文本。
     */
    fun export(events: List<Event>, productId: String = "-//Calendar App//RFC5545 Demo//CN"): String {
        val builder = StringBuilder()
        builder.appendLine("BEGIN:VCALENDAR")
        builder.appendLine("PRODID:$productId")
        builder.appendLine("VERSION:2.0")
        builder.appendLine("CALSCALE:GREGORIAN")
        builder.appendLine("METHOD:PUBLISH")

        events.forEach { event ->
            builder.appendLine("BEGIN:VEVENT")
            // UID
            builder.appendLine("UID:${escapeText(event.uid)}")
            // 时间：统一导出为 UTC
            builder.appendLine("DTSTART:${formatEpochMillisUtc(event.dtStart)}")
            builder.appendLine("DTEND:${formatEpochMillisUtc(event.dtEnd)}")
            // SUMMARY
            builder.appendLine("SUMMARY:${escapeText(event.summary)}")
            // DESCRIPTION / LOCATION
            escapeText(event.description)?.let { desc ->
                builder.appendLine("DESCRIPTION:$desc")
            }
            escapeText(event.location)?.let { loc ->
                builder.appendLine("LOCATION:$loc")
            }
            // CREATED / LAST-MODIFIED
            builder.appendLine("CREATED:${formatEpochMillisUtc(event.created)}")
            builder.appendLine("LAST-MODIFIED:${formatEpochMillisUtc(event.lastModified)}")
            builder.appendLine("END:VEVENT")
        }

        builder.appendLine("END:VCALENDAR")
        return builder.toString()
    }
}


