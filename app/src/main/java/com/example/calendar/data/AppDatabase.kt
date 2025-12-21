package com.example.calendar.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.firstOrNull

@Database(
    entities = [Event::class, Reminder::class, Subscription::class, SubscriptionEvent::class],
    version = 3,
    exportSchema = true
)
@TypeConverters(EventTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun eventDao(): EventDao
    abstract fun reminderDao(): ReminderDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun subscriptionEventDao(): SubscriptionEventDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "calendar_db"
                )
                    .fallbackToDestructiveMigration() // 允许在开发阶段破坏性迁移（开发时使用）
                    .build().also { INSTANCE = it }
            }
        }
    }
}

// 简单帮助方法：在后台线程中一次性获取单个事件（用于提醒通知）
suspend fun AppDatabase.getEventByIdOnce(id: Long): Event? {
    return eventDao().getEventById(id).firstOrNull()
}

