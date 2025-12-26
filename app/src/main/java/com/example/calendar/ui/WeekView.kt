package com.example.calendar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.calendar.data.Event
import com.example.calendar.util.startOfWeek
import com.example.calendar.util.toLocalDate
import java.time.LocalDate

@Composable
fun WeekView(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    allEvents: List<Event> = emptyList(),
    dayEvents: List<Event> = emptyList(),
    subscriptionEvents: List<Pair<com.example.calendar.data.SubscriptionEvent, com.example.calendar.data.SubscriptionType>> = emptyList(),
    subscriptionEventsForNext5Days: List<Pair<com.example.calendar.data.SubscriptionEvent, com.example.calendar.data.SubscriptionType>> = emptyList(),
    onEventClick: (Event) -> Unit = {},
    onCityChanged: (() -> Unit)? = null
) {
    val startOfWeek = selectedDate.startOfWeek()

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
    ) {
        // 星期标题行 - 与月视图保持一致
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val weekDays = listOf("日", "一", "二", "三", "四", "五", "六")
            weekDays.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        // 日期行 - 使用与月视图相同的DayCell组件
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (i in 0 until 7) {
                val date = startOfWeek.plusDays(i.toLong())
                val isSelected = date == selectedDate
                val isToday = date == LocalDate.now()
                // 检查该日期是否有事件
                val hasEvents = allEvents.any { event ->
                    val eventDate = event.dtStart.toLocalDate(event.timezone)
                    eventDate == date
                }

                DayCell(
                    date = date,
                    day = date.dayOfMonth,
                    isSelected = isSelected,
                    isToday = isToday,
                    hasEvents = hasEvents,
                    onClick = { onDateSelected(date) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // 选中日期的日程列表
        if (dayEvents.isNotEmpty() || subscriptionEvents.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            CalendarEventList(
                events = dayEvents,
                subscriptionEvents = subscriptionEvents,
                onEventClick = onEventClick,
                allSubscriptionEvents = subscriptionEventsForNext5Days,
                onCityChanged = onCityChanged
            )
        }
        
        // 底部信息栏 - 与月视图保持一致
        Spacer(modifier = Modifier.height(16.dp))
        CalendarViewFooter(selectedDate = selectedDate)
    }
}

