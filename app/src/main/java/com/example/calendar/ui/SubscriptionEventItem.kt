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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * 参考图片样式，天气和黄历卡片不同样式
 */
@Composable
fun SubscriptionEventItem(
    subscriptionEvent: SubscriptionEvent,
    subscriptionType: SubscriptionType,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    forecastEvents: List<SubscriptionEvent> = emptyList() // 用于显示5日预报
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
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
                        WeatherEventContent(subscriptionEvent, forecastEvents)
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
 */
@Composable
private fun WeatherEventContent(
    event: SubscriptionEvent,
    forecastEvents: List<SubscriptionEvent> = emptyList()
) {
    val gson = Gson()
    val content = try {
        gson.fromJson(event.content, JsonObject::class.java)
    } catch (e: Exception) {
        null
    }

    val type = content?.get("type")?.asString ?: ""
    val city = content?.get("city")?.asString ?: ""

    if (type == "current") {
        // 当前天气 - 参考图片样式
        val temp = content?.get("temp")?.asString ?: ""
        val weather = content?.get("weather")?.asString ?: ""
        val aqi = content?.get("aqi")?.asString ?: ""
        val aqiLevel = content?.get("aqiLevel")?.asString ?: ""

        // 顶部：地区 | 15日天气 >
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 天气图标
                Text(
                    text = "☁️",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = city.ifEmpty { "天气" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = " | ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "15日天气 >",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 温度、天气状况、空气质量
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${temp}°",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = weather,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (aqi.isNotEmpty()) {
                    Text(
                        text = "$aqiLevel $aqi",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 5日天气预报横向显示
        data class ForecastData(val date: String, val low: String, val high: String, val weather: String)
        
        val fiveDayForecasts = forecastEvents
            .take(5) // 只取前5天
            .mapNotNull { forecastEvent ->
                try {
                    val forecastContent = gson.fromJson(forecastEvent.content, JsonObject::class.java)
                    if (forecastContent?.get("type")?.asString == "forecast") {
                        ForecastData(
                            date = forecastContent.get("date")?.asString ?: "",
                            low = forecastContent.get("low")?.asString ?: "",
                            high = forecastContent.get("high")?.asString ?: "",
                            weather = forecastContent.get("weather")?.asString ?: ""
                        )
                    } else null
                } catch (e: Exception) {
                    null
                }
            }
        
        if (fiveDayForecasts.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                fiveDayForecasts.forEach { forecast ->
                    val date = forecast.date
                    val low = forecast.low
                    val high = forecast.high
                    val weather = forecast.weather
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        // 日期（今天显示"今天"，其他显示月/日）
                        val displayDate = if (date.contains("今天") || date.contains(LocalDate.now().toString())) {
                            "今天"
                        } else {
                            // 尝试解析日期并格式化
                            try {
                                val dateParts = date.split("-")
                                if (dateParts.size >= 2) {
                                    "${dateParts[1]}/${dateParts[2]}"
                                } else {
                                    date
                                }
                            } catch (e: Exception) {
                                date
                            }
                        }
                        Text(
                            text = displayDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // 天气图标（简化显示）
                        Text(
                            text = "☀️", // 可以根据weather字段选择不同图标
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // 温度范围
                        Text(
                            text = "$low°/$high°",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    } else {
        // 天气预报（单独显示某一天的预报）
        val date = content?.get("date")?.asString ?: ""
        val high = content?.get("high")?.asString ?: ""
        val low = content?.get("low")?.asString ?: ""
        val weather = content?.get("weather")?.asString ?: ""

        Text(
            text = date,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$low° / $high°",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = weather,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
    
    // 从yinli中提取农历日期部分（年月日）
    // 格式如"甲午(马)年八月十八"，需要提取"八月十八"
    val lunarDateDisplay = extractLunarDate(yinli)
    
    // 从yinli中提取年份和天干地支信息
    val yearInfo = extractYearInfo(yinli)

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // "黄历"标题和图标 - 参考图片样式
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            // 黄色背景的图标框（参考图片）
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
                // 农历日期（大字体）- 参考图片样式
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
            
            // 右侧：宜忌事项 - 上下排列，参考图片样式
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 宜（绿色圆形按钮）- 上方
                if (yiStr.isNotEmpty()) {
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
                            text = yiStr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 忌（红色圆形按钮）- 下方
                if (jiStr.isNotEmpty()) {
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
                            text = jiStr,
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

