package com.example.calendar.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
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
    onCityChanged: (() -> Unit)? = null, // 城市切换后的回调
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
                // 标签顺序：月(0) -> 周(1) -> 日(2)，从左到右
                // 切换到右侧标签（ordinal 增大）：新视图从右边进入，旧视图向左滑出
                // 切换到左侧标签（ordinal 减小）：新视图从左边进入，旧视图向右滑出
                val slideDirection = if (targetState.ordinal > initialState.ordinal) {
                    -1 // 切换到右侧：新视图从右边进入（initialOffsetX = -it）
                } else {
                    1 // 切换到左侧：新视图从左边进入（initialOffsetX = it）
                }
                (fadeIn(animationSpec = tween(300)) +
                        slideInHorizontally(
                            animationSpec = tween(300),
                            initialOffsetX = { it * slideDirection }
                        )) togetherWith
                        (fadeOut(animationSpec = tween(300)) +
                                slideOutHorizontally(
                                    animationSpec = tween(300),
                                    targetOffsetX = { it * slideDirection }
                                ))
            },
            label = "view_mode_transition"
        ) { targetMode ->
            val swipeModifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp)
                .addViewModeSwipe(
                    currentMode = targetMode,
                    modes = tabs.map { it.first },
                    onModeChange = onViewModeChange
                )
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
                        onCityChanged = onCityChanged,
                        modifier = swipeModifier
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
                        onCityChanged = onCityChanged,
                        modifier = swipeModifier
                    )
                }
                CalendarViewMode.DAY -> {
                    DayView(
                        date = uiState.selectedDate,
                        events = dayEvents,
                        subscriptionEvents = subscriptionEvents,
                        allSubscriptionEvents = subscriptionEventsForNext5Days,
                        contentPadding = PaddingValues(0.dp),
                        onEventClick = onDayEventClick,
                        onCityChanged = onCityChanged,
                        modifier = swipeModifier
                    )
                }
            }
        }
    }
}

/**
 * 为日/周/月视图添加左右滑动切换能力，按 tabs 顺序切换。
 */
private fun Modifier.addViewModeSwipe(
    currentMode: CalendarViewMode,
    modes: List<CalendarViewMode>,
    onModeChange: (CalendarViewMode) -> Unit,
    swipeThreshold: Dp = 48.dp
): Modifier = composed {
    // 使用 remember 记录阈值，避免多次计算
    val density = LocalDensity.current
    val thresholdPx = remember(swipeThreshold, density) {
        with(density) { swipeThreshold.toPx() }
    }
    pointerInput(currentMode, modes, thresholdPx) {
        var totalDrag = 0f
        detectHorizontalDragGestures(
            onHorizontalDrag = { _, dragAmount ->
                totalDrag += dragAmount
            },
            onDragCancel = {
                totalDrag = 0f
            },
            onDragEnd = {
                val currentIndex = modes.indexOf(currentMode).coerceAtLeast(0)
                when {
                    // 向左滑：切换到左侧（前一个）Tab
                    totalDrag <= -thresholdPx && currentIndex > 0 -> {
                        onModeChange(modes[currentIndex - 1])
                    }
                    // 向右滑：切换到右侧（后一个）Tab
                    totalDrag >= thresholdPx && currentIndex < modes.lastIndex -> {
                        onModeChange(modes[currentIndex + 1])
                    }
                }
                totalDrag = 0f
            }
        )
    }
}
