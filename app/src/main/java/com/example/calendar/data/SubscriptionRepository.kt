package com.example.calendar.data

import android.content.Context
import com.example.calendar.network.HuangliApiService
import com.example.calendar.network.RetrofitClient
import com.example.calendar.network.WeatherApiService
import com.example.calendar.util.LocationHelper
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import retrofit2.HttpException
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
     * 优化：只同步需要同步的订阅（基于最后更新时间）
     */
    suspend fun syncAllEnabledSubscriptions(): List<SyncResult> {
        val results = mutableListOf<SyncResult>()
        val subscriptionList = subscriptionDao.getAllSubscriptions().firstOrNull() ?: emptyList()
        subscriptionList.filter { it.enabled }.forEach { subscription ->
            // 检查是否需要同步（基于最后更新时间，默认24小时）
            if (shouldSync(subscription, syncIntervalHours = 24)) {
                val result = syncSubscription(subscription)
                results.add(result)
            } else {
                // 不需要同步，返回跳过结果
                results.add(
                    SyncResult(
                        success = true,
                        message = "跳过同步（数据未过期）",
                        updatedCount = 0
                    )
                )
            }
        }
        return results
    }

    /**
     * 同步天气订阅
     * 使用天气API获取天气预报
     */
    private suspend fun syncWeatherSubscription(subscription: Subscription): SyncResult {
        try {
            // 获取城市代码（邮编）- 使用同步方法
            val cityCode = if (context != null) {
                LocationHelper.getCityCode(context)
            } else {
                "101010100" // 默认北京城市代码
            }
            
            // 调用天气API获取天气预报
            val response = weatherApiService.getWeatherForecast(cityCode)
            
            if (response.status != 200 || response.data?.forecast == null || response.data.forecast.isEmpty()) {
                return SyncResult(
                    success = false,
                    message = "获取天气数据失败: status=${response.status}",
                    updatedCount = 0
                )
            }

            val events = mutableListOf<SubscriptionEvent>()
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            
            // 处理天气预报（包括今天和未来几天）
            response.data.forecast.forEachIndexed { index, forecast ->
                val dateStr = forecast.ymd ?: return@forEachIndexed
                val date = try {
                    LocalDate.parse(dateStr, dateFormatter)
                } catch (e: Exception) {
                    // 如果解析失败，使用今天+索引天数
                    LocalDate.now().plusDays(index.toLong())
                }
                
                val dateMillis = date.atStartOfDay(ZoneId.systemDefault())
                    .toInstant().toEpochMilli()
                
                // 从high和low字符串中提取温度数值
                // high格式："高温 3℃"，low格式："低温 -5℃"
                val tempMax = forecast.high?.replace("高温", "")?.replace("℃", "")?.trim() ?: ""
                val tempMin = forecast.low?.replace("低温", "")?.replace("℃", "")?.trim() ?: ""
                
                // 构建天气数据内容（保持与UI层的兼容性）
                val content = gson.toJson(mapOf(
                    "type" to if (index == 0) "current" else "forecast",
                    "date" to dateStr,
                    "fxDate" to dateStr,  // 使用ymd作为日期
                    "tempMax" to tempMax,
                    "tempMin" to tempMin,
                    "textDay" to (forecast.type ?: ""),  // 使用type作为天气类型
                    "textNight" to (forecast.type ?: ""),  // 新API没有单独的夜间天气，使用type
                    "windDirDay" to (forecast.fx ?: ""),  // 风向
                    "windScaleDay" to (forecast.fl ?: ""),  // 风力等级
                    "humidity" to (response.data.shidu ?: ""),  // 湿度
                    "weather" to (forecast.type ?: ""),  // 天气类型（兼容UI层）
                    "high" to tempMax,  // 兼容UI层
                    "low" to tempMin,  // 兼容UI层
                    "week" to (forecast.week ?: ""),  // 星期
                    "notice" to (forecast.notice ?: ""),  // 提示信息
                    "quality" to (response.data.quality ?: ""),  // 空气质量
                    "aqi" to (forecast.aqi?.toString() ?: "")  // 空气质量指数
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
        } catch (e: HttpException) {
            // HTTP错误，尝试获取响应体
            val errorBody = try {
                e.response()?.errorBody()?.string() ?: "无响应体"
            } catch (ex: Exception) {
                "无法读取错误响应体: ${ex.message}"
            }
            return SyncResult(
                success = false,
                message = "同步失败: HTTP ${e.code()} - ${errorBody.take(100)}",
                updatedCount = 0
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
     * 检查指定月份的数据是否完整（所有天数都有数据）
     * @param subscriptionId 订阅ID
     * @param yearMonth 目标月份
     * @return true 如果该月份所有天数都有数据，false 否则
     */
    suspend fun hasCompleteMonthData(subscriptionId: Long, yearMonth: LocalDate): Boolean {
        val firstDayOfMonth = yearMonth.withDayOfMonth(1)
        val lastDayOfMonth = yearMonth.withDayOfMonth(yearMonth.lengthOfMonth())
        val expectedDays = yearMonth.lengthOfMonth() // 该月的天数
        
        val startDateMillis = firstDayOfMonth.atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        val endDateMillis = lastDayOfMonth.plusDays(1).atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        
        val events = subscriptionEventDao.getEventsBetween(startDateMillis, endDateMillis, subscriptionId)
            .firstOrNull() ?: emptyList()
        
        // 如果事件数量少于该月的天数，说明数据不完整
        return events.size >= expectedDays
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

