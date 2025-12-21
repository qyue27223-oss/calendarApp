package com.example.calendar.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.firstOrNull

/**
 * 订阅数据同步的后台任务
 * 使用 WorkManager 定期执行
 */
class SubscriptionSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val database = AppDatabase.getInstance(applicationContext)
            val repository = SubscriptionRepository(
                subscriptionDao = database.subscriptionDao(),
                subscriptionEventDao = database.subscriptionEventDao(),
                context = applicationContext
            )

            // 同步所有启用的订阅
            val results = repository.syncAllEnabledSubscriptions()
            
            // 如果所有同步都成功，返回成功
            if (results.all { it.success }) {
                Result.success()
            } else {
                // 如果有部分失败，返回重试（WorkManager会自动重试）
                Result.retry()
            }
        } catch (e: Exception) {
            // 发生异常，返回重试
            Result.retry()
        }
    }
}

