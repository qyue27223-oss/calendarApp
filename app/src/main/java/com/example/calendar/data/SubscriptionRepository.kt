package com.example.calendar.data

import android.content.Context
import com.example.calendar.network.HuangliApiService
import com.example.calendar.network.RetrofitClient
import com.example.calendar.network.WeatherApiService
import com.example.calendar.util.LocationHelper
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 订阅数据仓库
 * 管理订阅配置和订阅事件的 CRUD 操作，以及数据同步逻辑
 */
class SubscriptionRepository(
    private val subscriptionDao: SubscriptionDao,
    private val subscriptionEventDao: SubscriptionEventDao,
    private val weatherApiService: WeatherApiService = RetrofitClient.weatherApiService,
    private val huangliApiService: HuangliApiService = RetrofitClient.huangliApiService,
    private val context: Context? = null
) {
    private val gson = Gson()
    private val WEATHER_API_KEY = "65e83959cb1c46f0bcf7ce72489075c7"

    // ========== 订阅配置管理 ==========

    fun getAllSubscriptions(): Flow<List<Subscription>> = subscriptionDao.getAllSubscriptions()

    fun getSubscriptionById(id: Long): Flow<Subscription?> = subscriptionDao.getSubscriptionById(id)

    fun getEnabledSubscriptionsByType(type: SubscriptionType): Flow<List<Subscription>> =
        subscriptionDao.getEnabledSubscriptionsByType(type)

    suspend fun insertSubscription(subscription: Subscription): Long =
        subscriptionDao.insertSubscription(subscription)

    suspend fun updateSubscription(subscription: Subscription) {
        subscriptionDao.updateSubscription(subscription)
    }

    suspend fun deleteSubscription(subscription: Subscription) {
        // 删除订阅时，同时删除所有关联的订阅事件
        subscriptionEventDao.deleteBySubscriptionId(subscription.id)
        subscriptionDao.deleteSubscription(subscription)
    }

    // ========== 订阅事件管理 ==========

    fun getEventsBySubscription(subscriptionId: Long): Flow<List<SubscriptionEvent>> =
        subscriptionEventDao.getEventsBySubscription(subscriptionId)

    fun getEventsBetween(startDate: Long, endDate: Long, subscriptionId: Long): Flow<List<SubscriptionEvent>> =
        subscriptionEventDao.getEventsBetween(startDate, endDate, subscriptionId)

    fun getEventsByDate(date: Long, subscriptionId: Long): Flow<List<SubscriptionEvent>> {
        // 计算当天的开始和结束时间戳（毫秒）
        val startOfDay = date
        val endOfDay = date + 24 * 60 * 60 * 1000L // 加一天
        return subscriptionEventDao.getEventsByDate(startOfDay, endOfDay, subscriptionId)
    }

    // ========== 数据同步 ==========

    /**
     * 同步指定订阅的数据
     */
    suspend fun syncSubscription(subscription: Subscription): SyncResult {
        return try {
            when (subscription.type) {
                SubscriptionType.WEATHER -> syncWeatherSubscription(subscription)
                SubscriptionType.HUANGLI -> syncHuangliSubscription(subscription)
            }
        } catch (e: Exception) {
            SyncResult(
                success = false,
                message = "同步失败: ${e.message}",
                updatedCount = 0
            )
        }
    }

    /**
     * 同步所有启用的订阅
     */
    suspend fun syncAllEnabledSubscriptions(): List<SyncResult> {
        val results = mutableListOf<SyncResult>()
        val subscriptionList = subscriptionDao.getAllSubscriptions().firstOrNull() ?: emptyList()
        subscriptionList.filter { it.enabled }.forEach { subscription ->
            val result = syncSubscription(subscription)
            results.add(result)
        }
        return results
    }

    /**
     * 同步天气订阅
     * 使用和风天气API获取7天天气预报
     */
    private suspend fun syncWeatherSubscription(subscription: Subscription): SyncResult {
        try {
            // 获取当前位置
            val location = context?.let { LocationHelper.getCurrentLocation(it) } 
                ?: "116.41,39.92" // 默认北京位置
            
            // 调用和风天气API获取7天天气预报
            val response = weatherApiService.get7DayForecast(
                key = WEATHER_API_KEY,
                location = location
            )
            
            if (response.code != "200" || response.daily == null || response.daily.isEmpty()) {
                return SyncResult(
                    success = false,
                    message = "获取天气数据失败: code=${response.code}",
                    updatedCount = 0
                )
            }

            val events = mutableListOf<SubscriptionEvent>()
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            
            // 处理7天天气预报
            response.daily.forEachIndexed { index, forecast ->
                val dateStr = forecast.fxDate ?: return@forEachIndexed
                val date = try {
                    LocalDate.parse(dateStr, dateFormatter)
                } catch (e: Exception) {
                    // 如果解析失败，使用今天+索引天数
                    LocalDate.now().plusDays(index.toLong())
                }
                
                val dateMillis = date.atStartOfDay(ZoneId.systemDefault())
                    .toInstant().toEpochMilli()
                
                // 构建天气数据内容
                val content = gson.toJson(mapOf(
                    "type" to if (index == 0) "current" else "forecast",
                    "date" to dateStr,
                    "fxDate" to forecast.fxDate,
                    "tempMax" to (forecast.tempMax ?: ""),
                    "tempMin" to (forecast.tempMin ?: ""),
                    "textDay" to (forecast.textDay ?: ""),
                    "textNight" to (forecast.textNight ?: ""),
                    "windDirDay" to (forecast.windDirDay ?: ""),
                    "windScaleDay" to (forecast.windScaleDay ?: ""),
                    "humidity" to (forecast.humidity ?: ""),
                    "uvIndex" to (forecast.uvIndex ?: "")
                ))
                
                events.add(
                    SubscriptionEvent(
                        subscriptionId = subscription.id,
                        date = dateMillis,
                        content = content
                    )
                )
            }

            // 删除旧的订阅事件并插入新的
            subscriptionEventDao.deleteBySubscriptionId(subscription.id)
            subscriptionEventDao.insertEvents(events)

            // 更新订阅的最后更新时间
            subscriptionDao.updateSubscription(
                subscription.copy(lastUpdateTime = System.currentTimeMillis())
            )

            return SyncResult(
                success = true,
                message = "同步成功",
                updatedCount = events.size
            )
        } catch (e: Exception) {
            return SyncResult(
                success = false,
                message = "同步失败: ${e.message}",
                updatedCount = 0
            )
        }
    }

    /**
     * 同步指定月份的黄历订阅数据
     * @param subscription 订阅信息
     * @param targetMonth 目标月份，如果为null则同步当前月份
     */
    suspend fun syncHuangliSubscriptionForMonth(
        subscription: Subscription,
        targetMonth: LocalDate? = null
    ): SyncResult {
        try {
            val events = mutableListOf<SubscriptionEvent>()
            val targetDate = targetMonth ?: LocalDate.now()
            
            // 获取目标月份的第一天和最后一天
            val firstDayOfMonth = targetDate.withDayOfMonth(1)
            val lastDayOfMonth = targetDate.withDayOfMonth(targetDate.lengthOfMonth())
            
            // API密钥
            val apiKey = "1ad714beb42ded25b15a429d6ba64168"
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            
            // 遍历目标月份的所有天数
            var currentDate = firstDayOfMonth
            while (!currentDate.isAfter(lastDayOfMonth)) {
                val dateStr = currentDate.format(dateFormatter)
                
                try {
                    val response = huangliApiService.getHuangli(apiKey, dateStr)
                    
                    // error_code为0表示成功
                    if (response.errorCode == 0 && response.result != null) {
                        val dateMillis = currentDate.atStartOfDay(ZoneId.systemDefault())
                            .toInstant().toEpochMilli()
                        val content = gson.toJson(response.result)
                        events.add(
                            SubscriptionEvent(
                                subscriptionId = subscription.id,
                                date = dateMillis,
                                content = content
                            )
                        )
                    }
                } catch (e: Exception) {
                    // 单个日期失败不影响其他日期
                    println("获取 ${dateStr} 的黄历信息失败: ${e.message}")
                }
                
                // 移动到下一天
                currentDate = currentDate.plusDays(1)
            }

            // 只删除目标月份的数据，保留其他月份的数据（支持跨月查看）
            val startDateMillis = firstDayOfMonth.atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
            val endDateMillis = lastDayOfMonth.plusDays(1).atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
            subscriptionEventDao.deleteBySubscriptionIdAndDateRange(
                subscription.id,
                startDateMillis,
                endDateMillis
            )
            // 使用 INSERT OR REPLACE 策略，如果数据已存在则更新
            subscriptionEventDao.insertEvents(events)

            // 更新订阅的最后更新时间
            subscriptionDao.updateSubscription(
                subscription.copy(lastUpdateTime = System.currentTimeMillis())
            )

            return SyncResult(
                success = true,
                message = "同步成功",
                updatedCount = events.size
            )
        } catch (e: Exception) {
            return SyncResult(
                success = false,
                message = "同步失败: ${e.message}",
                updatedCount = 0
            )
        }
    }

    /**
     * 同步黄历订阅
     * 获取当前月份的所有天数的黄历信息
     */
    private suspend fun syncHuangliSubscription(subscription: Subscription): SyncResult {
        return syncHuangliSubscriptionForMonth(subscription, null)
    }

    /**
     * 检查指定月份的数据是否存在
     */
    suspend fun hasMonthData(subscriptionId: Long, yearMonth: LocalDate): Boolean {
        val firstDayOfMonth = yearMonth.withDayOfMonth(1)
        val lastDayOfMonth = yearMonth.withDayOfMonth(yearMonth.lengthOfMonth())
        val startDateMillis = firstDayOfMonth.atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        val endDateMillis = lastDayOfMonth.plusDays(1).atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        val events = subscriptionEventDao.getEventsBetween(startDateMillis, endDateMillis, subscriptionId)
            .firstOrNull() ?: emptyList()
        return events.isNotEmpty()
    }

    /**
     * 检查是否需要同步（基于最后更新时间）
     * @param subscription 订阅信息
     * @param syncIntervalHours 同步间隔（小时），默认24小时
     */
    suspend fun shouldSync(subscription: Subscription, syncIntervalHours: Long = 24): Boolean {
        if (!subscription.enabled) return false
        if (subscription.lastUpdateTime == 0L) return true // 从未同步过
        
        val now = System.currentTimeMillis()
        val syncIntervalMillis = syncIntervalHours * 60 * 60 * 1000
        return (now - subscription.lastUpdateTime) >= syncIntervalMillis
    }

    /**
     * 清理过期的订阅事件（保留最近30天）
     */
    suspend fun cleanupOldEvents() {
        val thirtyDaysAgo = LocalDate.now()
            .minusDays(30)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        subscriptionEventDao.deleteEventsBefore(thirtyDaysAgo)
    }
}

/**
 * 同步结果
 */
data class SyncResult(
    val success: Boolean,
    val message: String,
    val updatedCount: Int
)

