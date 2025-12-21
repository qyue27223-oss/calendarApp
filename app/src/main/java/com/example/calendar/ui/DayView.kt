package com.example.calendar.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.calendar.data.Event
import com.example.calendar.data.SubscriptionEvent
import com.example.calendar.util.LunarCalendarUtil
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DayView(
    date: LocalDate,
    events: List<Event>,
    subscriptionEvents: List<Pair<SubscriptionEvent, com.example.calendar.data.SubscriptionType>> = emptyList(),
    contentPadding: PaddingValues,
    onEventClick: (Event) -> Unit,
    allSubscriptionEvents: List<Pair<SubscriptionEvent, com.example.calendar.data.SubscriptionType>> = emptyList(),
    modifier: Modifier = Modifier
        .fillMaxSize()
) {
    val formatter = rememberTimeFormatter()

    Column(
        modifier = modifier
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "${date.year}年${date.monthValue}月${date.dayOfMonth}日",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // 显示农历信息
        LunarInfoCard(date = date)

        Spacer(modifier = Modifier.height(8.dp))

        if (events.isEmpty() && subscriptionEvents.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "今天没有日程",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn {
                // 订阅事件
                items(
                    items = subscriptionEvents,
                    key = { it.first.id }
                ) { (subscriptionEvent, subscriptionType) ->
                    SubscriptionEventItem(
                        subscriptionEvent = subscriptionEvent,
                        subscriptionType = subscriptionType,
                        allSubscriptionEvents = allSubscriptionEvents
                    )
                }

                // 普通日程事件
                items(
                    items = events,
                    key = { it.id }
                ) { event ->
                    EventItemCard(
                        event = event,
                        formatter = formatter,
                        onClick = { onEventClick(event) },
                        modifier = Modifier.padding(vertical = 6.dp),
                        showDetails = true
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberTimeFormatter(): DateTimeFormatter {
    // 使用统一的时间格式化器
    return remember {
        com.example.calendar.util.TimeFormatters.TIME_FORMATTER
    }
}

/**
 * 农历信息卡片
 */
@Composable
private fun LunarInfoCard(date: LocalDate) {
    val lunarDate = LunarCalendarUtil.solarToLunar(date)
    val lunarDateStr = LunarCalendarUtil.formatLunarDate(lunarDate, showYear = true)
    val ganzhi = LunarCalendarUtil.calculateGanzhi(date.year, date.monthValue, date.dayOfMonth)
    val weekday = LunarCalendarUtil.getWeekdayChinese(date)
    val solarTerm = LunarCalendarUtil.isSolarTerm(date)
    val festival = LunarCalendarUtil.getSolarFestival(date) 
        ?: LunarCalendarUtil.getLunarFestival(lunarDate)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF3E5F5) // 淡紫色背景
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 农历日期（第一行）
            Text(
                text = "农历 $lunarDateStr",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 天干地支信息（第二行）
            Text(
                text = "${ganzhi.year} ${ganzhi.month} ${ganzhi.day} $weekday",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // 节气或节日（第三行，紫色显示）
            (solarTerm ?: festival)?.let { info ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = info,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
