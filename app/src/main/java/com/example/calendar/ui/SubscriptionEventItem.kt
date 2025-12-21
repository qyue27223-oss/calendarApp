package com.example.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.calendar.data.SubscriptionEvent
import com.example.calendar.data.SubscriptionType
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.time.LocalDate

/**
 * 订阅事件卡片组件
 * 支持天气和黄历两种卡片样式
 */
@Composable
fun SubscriptionEventItem(
    subscriptionEvent: SubscriptionEvent,
    subscriptionType: SubscriptionType,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    allSubscriptionEvents: List<Pair<SubscriptionEvent, SubscriptionType>> = emptyList() // 所有订阅事件，用于获取完整的5天预报
) {
    // 对于天气类型，检查是否在11天范围内
    if (subscriptionType == SubscriptionType.WEATHER) {
        val gson = Gson()
        val content = try {
            gson.fromJson(subscriptionEvent.content, JsonObject::class.java)
        } catch (e: Exception) {
            null
        }
        val fxDate = content?.get("fxDate")?.asString ?: ""
        
        // 检查日期是否在从今天开始的11天范围内
        try {
            val eventDate = LocalDate.parse(fxDate)
            val today = LocalDate.now()
            val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(today, eventDate)
            
            // 如果不在0-10天范围内（包含今天一共11天），隐藏卡片
            if (daysDiff < 0 || daysDiff > 10) {
                return
            }
        } catch (e: Exception) {
            // 如果日期解析失败，隐藏卡片
            return
        }
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 左侧蓝色竖线标识（仅天气卡片显示）
            if (subscriptionType == SubscriptionType.WEATHER) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .background(Color(0xFF2196F3))
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                when (subscriptionType) {
                    SubscriptionType.WEATHER -> {
                        WeatherEventContent(subscriptionEvent, allSubscriptionEvents)
                    }
                    SubscriptionType.HUANGLI -> {
                        HuangliEventContent(subscriptionEvent)
                    }
                }
            }
        }
    }
}

/**
 * 天气事件内容
 * 顶部显示标签和标题，然后是当前温度和天气状况，底部显示5天预报
 */
@Composable
private fun WeatherEventContent(
    event: SubscriptionEvent,
    allSubscriptionEvents: List<Pair<SubscriptionEvent, SubscriptionType>> = emptyList()
) {
    val gson = Gson()
    val content = try {
        gson.fromJson(event.content, JsonObject::class.java)
    } catch (e: Exception) {
        null
    }

    val type = content?.get("type")?.asString ?: ""
    
    if (type == "current" || type == "forecast") {
        val fxDate = content?.get("fxDate")?.asString ?: ""
        val eventDate = try {
            LocalDate.parse(fxDate)
        } catch (e: Exception) {
            return
        }
        val today = LocalDate.now()
        val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(today, eventDate)
        
        // 只显示从今天开始的11天内的数据
        if (daysDiff < 0 || daysDiff > 10) {
            return
        }
        
        // 当前温度
        val tempMax = content?.get("tempMax")?.asString ?: ""
        val tempMin = content?.get("tempMin")?.asString ?: ""
        val textDay = content?.get("textDay")?.asString ?: content?.get("weather")?.asString ?: ""
        val quality = content?.get("quality")?.asString ?: ""  // 空气质量
        val aqi = content?.get("aqi")?.asString ?: ""  // 空气质量指数
        
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 顶部标签和标题 - 参考黄历卡片样式
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                // 橙色/黄色背景的图标框
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color(0xFFFFB74D), shape = RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "☀️",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "天气",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 所有日期都显示完整的5天预报样式
            // 获取从当前事件日期开始的5天数据
            data class ForecastData(val date: String, val low: String, val high: String, val weather: String)
            
            // 当前事件的预报数据
            val currentForecast = ForecastData(
                date = fxDate,
                low = tempMin,
                high = tempMax,
                weather = textDay
            )
            
            // 从所有订阅事件中获取从当前日期开始的未来4天数据
            val futureForecasts = allSubscriptionEvents
                .filter { 
                    it.second == SubscriptionType.WEATHER && 
                    it.first.id != event.id && // 排除当前事件
                    it.first.date > event.date // 只取未来的事件
                }
                .map { it.first }
                .sortedBy { it.date }
                .take(4) // 只取未来4天
                .mapNotNull { forecastEvent ->
                    try {
                        val forecastContent = gson.fromJson(forecastEvent.content, JsonObject::class.java)
                        val forecastType = forecastContent?.get("type")?.asString ?: ""
                        if (forecastType == "forecast" || forecastType == "current") {
                            ForecastData(
                                date = forecastContent.get("fxDate")?.asString ?: forecastContent.get("date")?.asString ?: "",
                                low = forecastContent.get("tempMin")?.asString ?: forecastContent.get("low")?.asString ?: "",
                                high = forecastContent.get("tempMax")?.asString ?: forecastContent.get("high")?.asString ?: "",
                                weather = forecastContent.get("textDay")?.asString ?: forecastContent.get("weather")?.asString ?: ""
                            )
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                }
            
            val fiveDayForecasts = listOf(currentForecast) + futureForecasts
            
            // 如果数据不足5天，不显示卡片（确保所有显示的卡片都是完整的5天预报）
            if (fiveDayForecasts.size < 5) {
                return
            }
            
            // 显示完整的5天预报
            // 当前温度和天气状况
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧：当前温度（大号字体）
                    Text(
                        text = "${tempMax}°",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // 右侧：天气图标 + 天气状况 + 空气质量
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 天气图标（根据天气类型选择）
                        val weatherIcon = getWeatherIcon(textDay)
                        Text(
                            text = weatherIcon,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = textDay.ifEmpty { "晴" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black
                        )
                        if (quality.isNotEmpty() || aqi.isNotEmpty()) {
                            Text(
                                text = " | ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Black
                            )
                            // 显示空气质量：优先使用quality，如果有aqi也显示
                            val qualityText = if (quality.isNotEmpty()) {
                                if (aqi.isNotEmpty()) {
                                    "$quality $aqi"
                                } else {
                                    quality
                                }
                            } else if (aqi.isNotEmpty()) {
                                "优 $aqi"
                            } else {
                                ""
                            }
                            if (qualityText.isNotEmpty()) {
                                Text(
                                    text = qualityText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 底部：5天天气预报横向显示
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    fiveDayForecasts.forEachIndexed { index, forecast ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            // 日期（第一天显示"今天"或日期，其他显示月/日）
                            val displayDate = if (index == 0 && daysDiff == 0L) {
                                "今天"
                            } else {
                                try {
                                    val dateParts = forecast.date.split("-")
                                    if (dateParts.size >= 3) {
                                        "${dateParts[1]}/${dateParts[2]}"
                                    } else {
                                        forecast.date
                                    }
                                } catch (e: Exception) {
                                    forecast.date
                                }
                            }
                            Text(
                                text = displayDate,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            // 天气图标
                            val weatherIcon = getWeatherIcon(forecast.weather)
                            Text(
                                text = weatherIcon,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            // 温度范围（低/高）
                            Text(
                                text = "${forecast.low}°/${forecast.high}°",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
        }
    }
}

/**
 * 根据天气类型返回对应的图标
 */
private fun getWeatherIcon(weather: String): String {
    return when {
        weather.contains("晴") -> "☀️"
        weather.contains("云") || weather.contains("多云") -> "⛅"
        weather.contains("阴") -> "☁️"
        weather.contains("雨") -> "🌧️"
        weather.contains("雪") -> "❄️"
        weather.contains("雾") -> "🌫️"
        else -> "☀️"
    }
}

/**
 * 黄历事件内容
 * 解析聚合数据API返回的黄历数据
 */
@Composable
private fun HuangliEventContent(event: SubscriptionEvent) {
    val gson = Gson()
    val content = try {
        gson.fromJson(event.content, JsonObject::class.java)
    } catch (e: Exception) {
        null
    }

    // 解析新的API格式
    val yinli = content?.get("yinli")?.asString ?: ""  // 阴历，如"甲午(马)年八月十八"
    val yiStr = content?.get("yi")?.asString ?: ""  // 宜（字符串）
    val jiStr = content?.get("ji")?.asString ?: ""  // 忌（字符串）
    
    // 解析宜和忌，限制最多显示6项
    val yiItems = parseAndLimitItems(yiStr, maxItems = 6)
    val jiItems = parseAndLimitItems(jiStr, maxItems = 6)
    
    // 从yinli中提取农历日期部分（年月日）
    // 格式如"甲午(马)年八月十八"，需要提取"八月十八"
    val lunarDateDisplay = extractLunarDate(yinli)
    
    // 从yinli中提取年份和天干地支信息
    val yearInfo = extractYearInfo(yinli)

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // "黄历"标题和图标
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            // 黄色背景的图标框
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Color(0xFFFFB74D), shape = RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📖",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "黄历",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // 主内容区域：左右布局，左侧日期信息，右侧宜忌事项
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 左侧：日期信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 农历日期（大字体）
                if (lunarDateDisplay.isNotEmpty()) {
                    Text(
                        text = lunarDateDisplay,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // 年份信息
                if (yearInfo.isNotEmpty()) {
                    Text(
                        text = yearInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
            
            // 右侧：宜忌事项 - 上下排列
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 宜（绿色圆形按钮）- 上方
                if (yiItems.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Color(0xFFF5F5F5), // 浅灰色背景
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        // 绿色圆形背景，白色文字
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(Color(0xFF4CAF50), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "宜",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = yiItems.joinToString(" "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 忌（红色圆形按钮）- 下方
                if (jiItems.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Color(0xFFF5F5F5), // 浅灰色背景
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        // 红色圆形背景，白色文字
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(Color(0xFFF44336), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "忌",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = jiItems.joinToString(" "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * 从yinli字符串中提取农历日期部分
 * 例如："甲午(马)年八月十八" -> "八月十八"
 */
private fun extractLunarDate(yinli: String): String {
    if (yinli.isEmpty()) return ""
    
    // 查找"年"字之后的内容
    val yearIndex = yinli.indexOf("年")
    if (yearIndex >= 0 && yearIndex < yinli.length - 1) {
        return yinli.substring(yearIndex + 1)
    }
    
    return yinli
}

/**
 * 从yinli字符串中提取年份信息
 * 例如："甲午(马)年八月十八" -> "甲午(马)年"
 */
private fun extractYearInfo(yinli: String): String {
    if (yinli.isEmpty()) return ""
    
    val yearIndex = yinli.indexOf("年")
    if (yearIndex > 0) {
        return yinli.substring(0, yearIndex + 1)
    }
    
    return ""
}

/**
 * 解析并限制显示的项目数量
 * @param itemsStr 项目字符串，用空格分隔
 * @param maxItems 最多显示的项目数量，默认6个
 * @return 限制后的项目列表
 */
private fun parseAndLimitItems(itemsStr: String, maxItems: Int = 6): List<String> {
    if (itemsStr.isEmpty()) return emptyList()
    
    // 按空格分隔，过滤空字符串，限制数量
    return itemsStr.split(" ")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .take(maxItems)
}

