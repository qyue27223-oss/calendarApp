package com.example.calendar

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.RingtoneManager
import android.os.Build
import com.example.calendar.data.AppDatabase
import com.example.calendar.data.SubscriptionRepository
import com.example.calendar.data.SubscriptionSyncManager
import com.example.calendar.reminder.ReminderReceiver
import com.example.calendar.reminder.ReminderScheduler
import com.example.calendar.data.EventRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class CalendarApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: AppDatabase
        private set

    lateinit var reminderScheduler: ReminderScheduler
        private set

    lateinit var eventRepository: EventRepository
        private set

    lateinit var subscriptionRepository: SubscriptionRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(applicationContext)
        reminderScheduler = ReminderScheduler(applicationContext)
        eventRepository = EventRepository(
            eventDao = database.eventDao(),
            reminderDao = database.reminderDao(),
            reminderScheduler = reminderScheduler
        )
        subscriptionRepository = SubscriptionRepository(
            subscriptionDao = database.subscriptionDao(),
            subscriptionEventDao = database.subscriptionEventDao(),
            context = applicationContext
        )

        // 创建通知渠道（必须在应用启动时创建，确保 ReminderReceiver 能使用）
        createNotificationChannels()

        // 启动定时同步任务
        SubscriptionSyncManager.startPeriodicSync(applicationContext)

        // 应用启动时检查并同步订阅数据（超过24小时则同步）
        appScope.launch {
            syncSubscriptionsOnStartup(subscriptionRepository)
        }
    }

    private fun createNotificationChannels() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        
        // Android 8.0+ 需要创建通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 如果渠道已存在，先删除它们（因为渠道的声音设置一旦创建就无法通过代码修改）
            val existingChannel = notificationManager.getNotificationChannel(ReminderReceiver.REMINDER_CHANNEL_ID)
            if (existingChannel != null) {
                notificationManager.deleteNotificationChannel(ReminderReceiver.REMINDER_CHANNEL_ID)
            }
            val existingSilentChannel = notificationManager.getNotificationChannel(ReminderReceiver.REMINDER_SILENT_CHANNEL_ID)
            if (existingSilentChannel != null) {
                notificationManager.deleteNotificationChannel(ReminderReceiver.REMINDER_SILENT_CHANNEL_ID)
            }
            
            // 创建有响铃的提醒渠道（启用声音）
            val channel = NotificationChannel(ReminderReceiver.REMINDER_CHANNEL_ID, "日程提醒（响铃）", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "带声音的日程提醒"
                // 使用系统自带的通知铃声
                val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                setSound(defaultSoundUri, android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION) // 使用通知类型
                    .build())
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(false)
            }
            notificationManager.createNotificationChannel(channel)
            
            // 创建静音提醒渠道（无声音）
            val silentChannel = NotificationChannel(ReminderReceiver.REMINDER_SILENT_CHANNEL_ID, "日程提醒（静音）", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "静音日程提醒"
                setSound(null, null) // 禁用声音
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(false)
            }
            notificationManager.createNotificationChannel(silentChannel)
        }
    }

    private suspend fun syncSubscriptionsOnStartup(
        repository: SubscriptionRepository
    ) {
        val subscriptions = repository.getAllSubscriptions().firstOrNull() ?: emptyList()
        subscriptions.filter { it.enabled }.forEach { subscription ->
            if (repository.shouldSync(subscription, syncIntervalHours = 24)) {
                repository.syncSubscription(subscription)
            }
        }
    }
}

