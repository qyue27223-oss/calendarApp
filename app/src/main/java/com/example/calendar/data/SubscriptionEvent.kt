package com.example.calendar.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 订阅事件实体。
 * 用于存储从网络订阅获取的事件数据（天气、黄历等）。
 */
@Entity(
    tableName = "subscription_events",
    foreignKeys = [
        ForeignKey(
            entity = Subscription::class,
            parentColumns = ["id"],
            childColumns = ["subscriptionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["subscriptionId", "date"]), Index(value = ["date"])]
)
data class SubscriptionEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subscriptionId: Long,
    val date: Long,  // 日期时间戳（毫秒），只取日期部分
    val content: String,  // JSON 格式的内容，根据订阅类型不同而不同
    val createdAt: Long = System.currentTimeMillis()
)

