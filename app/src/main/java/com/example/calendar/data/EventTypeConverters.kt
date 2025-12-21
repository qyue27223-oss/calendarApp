package com.example.calendar.data

import androidx.room.TypeConverter

/**
 * Room 类型转换器，用于将枚举类型转换为数据库存储的字符串
 */
class EventTypeConverters {
    
    @TypeConverter
    fun fromEventType(value: EventType): String {
        return value.name
    }
    
    @TypeConverter
    fun toEventType(value: String): EventType {
        return EventType.valueOf(value)
    }
    
    @TypeConverter
    fun fromRepeatType(value: RepeatType): String {
        return value.name
    }
    
    @TypeConverter
    fun toRepeatType(value: String): RepeatType {
        return RepeatType.valueOf(value)
    }
}

