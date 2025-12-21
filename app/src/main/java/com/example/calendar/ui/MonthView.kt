package com.example.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.calendar.data.Event
import com.example.calendar.util.LunarCalendarUtil
import com.example.calendar.util.startOfWeek
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun MonthView(
    month: YearMonth,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    allEvents: List<Event> = emptyList(),
    dayEvents: List<Event> = emptyList(),
    subscriptionEvents: List<Pair<com.example.calendar.data.SubscriptionEvent, com.example.calendar.data.SubscriptionType>> = emptyList(),
    subscriptionEventsForNext5Days: List<Pair<com.example.calendar.data.SubscriptionEvent, com.example.calendar.data.SubscriptionType>> = emptyList(),
    onEventClick: (Event) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
    ) {
        // 星期标题行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val weekDays = listOf("一", "二", "三", "四", "五", "六", "日")
            weekDays.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        // 日期网格
        val firstOfMonth = month.atDay(1)
        val firstDayOfWeek = firstOfMonth.startOfWeek()
        val daysInMonth = month.lengthOfMonth()
        val offsetDays = java.time.temporal.ChronoUnit.DAYS.between(firstDayOfWeek, firstOfMonth).toInt()
        val totalCells = ((offsetDays + daysInMonth + 6) / 7) * 7

        Column {
            for (row in 0 until totalCells / 7) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - offsetDays + 1
                        if (dayNumber in 1..daysInMonth) {
                            val date = month.atDay(dayNumber)
                            val isSelected = date == selectedDate
                            val isToday = date == LocalDate.now()
                            // 检查该日期是否有事件
                            val hasEvents = allEvents.any { event ->
                                val eventDate = java.time.Instant.ofEpochMilli(event.dtStart)
                                    .atZone(java.time.ZoneId.of(event.timezone))
                                    .toLocalDate()
                                eventDate == date
                            }

                            DayCell(
                                date = date,
                                day = dayNumber,
                                isSelected = isSelected,
                                isToday = isToday,
                                hasEvents = hasEvents,
                                onClick = { onDateSelected(date) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            DayCellEmpty(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        
        // 选中日期的日程列表
        if (dayEvents.isNotEmpty() || subscriptionEvents.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            CalendarEventList(
                events = dayEvents,
                subscriptionEvents = subscriptionEvents,
                onEventClick = onEventClick,
                allSubscriptionEvents = subscriptionEventsForNext5Days
            )
        }
        
        // 底部信息栏
        Spacer(modifier = Modifier.height(16.dp))
        CalendarViewFooter(selectedDate = selectedDate)
    }
}

@Composable
internal fun DayCell(
    date: LocalDate,
    day: Int,
    isSelected: Boolean,
    isToday: Boolean,
    hasEvents: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // 选中日期使用圆形背景
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day.toString(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else if (isToday) {
            // 今天 - 使用浅色背景突出显示
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 日期数字 - 使用主色调加粗显示
                Text(
                    text = day.toString(),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                
                // 事件标记（蓝色小圆点）- 显示在日期数字和农历/节日之间
                if (hasEvents) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2196F3))
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                
                // 显示农历日期或节日
                val lunarDate = LunarCalendarUtil.solarToLunar(date)
                val solarTerm = LunarCalendarUtil.isSolarTerm(date)
                val festival = LunarCalendarUtil.getSolarFestival(date) 
                    ?: LunarCalendarUtil.getLunarFestival(lunarDate)
                
                val displayText = when {
                    festival != null -> festival
                    solarTerm != null -> solarTerm
                    else -> LunarCalendarUtil.formatLunarDayShort(lunarDate)
                }
                
                Text(
                    text = displayText,
                    color = if (festival != null || solarTerm != null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = if (hasEvents) 0.dp else 2.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            // 普通日期
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 日期数字
                Text(
                    text = day.toString(),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
                )
                
                // 事件标记（蓝色小圆点）- 显示在日期数字和农历/节日之间
                if (hasEvents) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2196F3))
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                
                // 显示农历日期或节日
                val lunarDate = LunarCalendarUtil.solarToLunar(date)
                val solarTerm = LunarCalendarUtil.isSolarTerm(date)
                val festival = LunarCalendarUtil.getSolarFestival(date) 
                    ?: LunarCalendarUtil.getLunarFestival(lunarDate)
                
                // 如果有节日或节气，只显示节日/节气，不显示农历日期
                val displayText = when {
                    festival != null -> festival
                    solarTerm != null -> solarTerm
                    else -> {
                        // 没有节日时显示农历日期
                        LunarCalendarUtil.formatLunarDayShort(lunarDate)
                    }
                }
                
                Text(
                    text = displayText,
                    color = if (festival != null || solarTerm != null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = if (hasEvents) 0.dp else 2.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun DayCellEmpty(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {}
}

