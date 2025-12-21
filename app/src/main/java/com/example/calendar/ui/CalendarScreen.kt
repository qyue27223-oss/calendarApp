package com.example.calendar.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CalendarScreen(
    uiState: CalendarUiState,
    onViewModeChange: (CalendarViewMode) -> Unit,
    onDateSelected: (java.time.LocalDate) -> Unit = {},
    dayEvents: List<com.example.calendar.data.Event> = emptyList(),
    allEvents: List<com.example.calendar.data.Event> = emptyList(),
    subscriptionEvents: List<Pair<com.example.calendar.data.SubscriptionEvent, com.example.calendar.data.SubscriptionType>> = emptyList(),
    subscriptionEventsForNext5Days: List<Pair<com.example.calendar.data.SubscriptionEvent, com.example.calendar.data.SubscriptionType>> = emptyList(),
    onDayEventClick: (com.example.calendar.data.Event) -> Unit = {},
    contentPadding: PaddingValues
) {
    val tabs = listOf(
        CalendarViewMode.MONTH to "月",
        CalendarViewMode.WEEK to "周",
        CalendarViewMode.DAY to "日"
    )

    val selectedIndex = tabs.indexOfFirst { it.first == uiState.viewMode }.coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        TabRow(selectedTabIndex = selectedIndex) {
            tabs.forEachIndexed { index, (mode, label) ->
                Tab(
                    selected = index == selectedIndex,
                    onClick = { onViewModeChange(mode) },
                    text = { Text(text = label) }
                )
            }
        }

        AnimatedContent(
            targetState = uiState.viewMode,
            transitionSpec = {
                val slideDirection = if (targetState.ordinal > initialState.ordinal) {
                    -1 // 向右滑动（新视图从右边进入）
                } else {
                    1 // 向左滑动（新视图从左边进入）
                }
                (fadeIn(animationSpec = tween(300)) +
                        slideInHorizontally(
                            animationSpec = tween(300),
                            initialOffsetX = { it * slideDirection }
                        )) togetherWith
                        (fadeOut(animationSpec = tween(300)) +
                                slideOutHorizontally(
                                    animationSpec = tween(300),
                                    targetOffsetX = { -it * slideDirection }
                                ))
            },
            label = "view_mode_transition"
        ) { targetMode ->
            when (targetMode) {
                CalendarViewMode.MONTH -> {
                    MonthView(
                        month = java.time.YearMonth.of(
                            uiState.selectedDate.year,
                            uiState.selectedDate.month
                        ),
                        selectedDate = uiState.selectedDate,
                        onDateSelected = onDateSelected,
                        allEvents = allEvents,
                        dayEvents = dayEvents,
                        subscriptionEvents = subscriptionEvents,
                        subscriptionEventsForNext5Days = subscriptionEventsForNext5Days,
                        onEventClick = onDayEventClick,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 8.dp)
                    )
                }
                CalendarViewMode.WEEK -> {
                    WeekView(
                        selectedDate = uiState.selectedDate,
                        onDateSelected = onDateSelected,
                        allEvents = allEvents,
                        dayEvents = dayEvents,
                        subscriptionEvents = subscriptionEvents,
                        subscriptionEventsForNext5Days = subscriptionEventsForNext5Days,
                        onEventClick = onDayEventClick,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 8.dp)
                    )
                }
                CalendarViewMode.DAY -> {
                    DayView(
                        date = uiState.selectedDate,
                        events = dayEvents,
                        subscriptionEvents = subscriptionEvents,
                        allSubscriptionEvents = subscriptionEventsForNext5Days,
                        contentPadding = PaddingValues(0.dp),
                        onEventClick = onDayEventClick
                    )
                }
            }
        }
    }
}
