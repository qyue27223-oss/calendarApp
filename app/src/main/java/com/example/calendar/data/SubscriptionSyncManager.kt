package com.example.calendar.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * 订阅同步管理器
 * 负责配置和管理后台同步任务
 */
object SubscriptionSyncManager {
    private const val SYNC_WORK_NAME = "subscription_sync_work"
    
    /**
     * 启动定时同步任务
     * 每24小时执行一次，仅在网络连接时执行
     */
    fun startPeriodicSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // 需要网络连接
            .build()

        val syncWorkRequest = PeriodicWorkRequestBuilder<SubscriptionSyncWorker>(
            24, TimeUnit.HOURS // 每24小时执行一次
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // 如果任务已存在，保持现有任务
            syncWorkRequest
        )
    }

    /**
     * 取消定时同步任务
     */
    fun cancelPeriodicSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(SYNC_WORK_NAME)
    }
}

