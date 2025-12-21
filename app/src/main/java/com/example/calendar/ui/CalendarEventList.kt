package com.example.calendar.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.calendar.data.Event
import com.example.calendar.data.SubscriptionEvent
import com.example.calendar.data.SubscriptionType
import com.example.calendar.util.TimeFormatters
import java.time.format.DateTimeFormatter

/**
 * 日历事件列表组件，用于在月视图和周视图中显示选中日期的事件
 * 
 * @param events 普通日程事件列表
 * @param subscriptionEvents 订阅事件列表
 * @param onEventClick 事件点击回调
 * @param formatter 时间格式化器，如果为null则使用默认的 HH:mm 格式
 * @param allSubscriptionEvents 所有订阅事件列表（用于获取未来5天的预报数据）
 */
@Composable
fun CalendarEventList(
    events: List<Event>,
    subscriptionEvents: List<Pair<SubscriptionEvent, SubscriptionType>>,
    onEventClick: (Event) -> Unit,
    formatter: DateTimeFormatter? = null,
    allSubscriptionEvents: List<Pair<SubscriptionEvent, SubscriptionType>> = emptyList(),
    onCityChanged: (() -> Unit)? = null
) {
    val timeFormatter = formatter ?: TimeFormatters.TIME_FORMATTER
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // 订阅事件
        subscriptionEvents.forEach { (subscriptionEvent, subscriptionType) ->
            SubscriptionEventItem(
                subscriptionEvent = subscriptionEvent,
                subscriptionType = subscriptionType,
                allSubscriptionEvents = allSubscriptionEvents,
                modifier = Modifier.padding(vertical = 4.dp),
                onCityChanged = onCityChanged
            )
        }
        
        // 普通日程事件
        events.forEach { event ->
            EventItemCard(
                event = event,
                formatter = timeFormatter,
                onClick = { onEventClick(event) },
                modifier = Modifier.padding(vertical = 4.dp),
                showDetails = false
            )
        }
    }
}

