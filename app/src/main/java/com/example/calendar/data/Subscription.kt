package com.example.calendar.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SubscriptionType {
    WEATHER,    // 天气订阅
    HUANGLI     // 黄历订阅
}

@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: SubscriptionType,
    val name: String,
    val url: String,
    val enabled: Boolean = true,
    val lastUpdateTime: Long = 0L
)

