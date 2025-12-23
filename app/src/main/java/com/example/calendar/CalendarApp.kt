package com.example.calendar

import android.app.Application
import com.example.calendar.data.AppDatabase
import com.example.calendar.data.SubscriptionRepository
import com.example.calendar.data.SubscriptionSyncManager
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

        // 启动定时同步任务
        SubscriptionSyncManager.startPeriodicSync(applicationContext)

        // 应用启动时检查并同步订阅数据（超过24小时则同步）
        appScope.launch {
            syncSubscriptionsOnStartup(subscriptionRepository)
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

