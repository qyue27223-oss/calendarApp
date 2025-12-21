package com.example.calendar.data

/**
 * 日程类型枚举
 */
enum class EventType(val displayName: String) {
    NORMAL("普通日程"),
    BIRTHDAY("生日"),
    ANNIVERSARY("纪念日"),
    OTHER("其他")
}

