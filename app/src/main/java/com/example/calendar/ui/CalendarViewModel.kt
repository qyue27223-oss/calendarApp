package com.example.calendar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendar.data.Event
import com.example.calendar.data.EventRepository
import com.example.calendar.data.ImportResult
import com.example.calendar.data.SubscriptionEvent
import com.example.calendar.data.SubscriptionRepository
import com.example.calendar.data.SubscriptionType
import com.example.calendar.util.IcsExporter
import com.example.calendar.util.toLocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate

enum class CalendarViewMode {
    MONTH, WEEK, DAY
}

data class CalendarUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val viewMode: CalendarViewMode = CalendarViewMode.MONTH,
    val isEditing: Boolean = false,
    val editingEvent: Event? = null,
    val importResult: ImportResult? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    private val repository: EventRepository,
    private val subscriptionRepository: SubscriptionRepository? = null
) : ViewModel() {

    // 记录正在同步的日期，避免并发重复请求
    private val syncingDates = mutableSetOf<LocalDate>()

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    val allEvents: StateFlow<List<Event>> =
        repository.getAllEvents()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    val eventsForSelectedDate: StateFlow<List<Event>> =
        combine(allEvents, uiState) { events, state ->
            events.filter { event ->
                val date = event.dtStart.toLocalDate(event.timezone)
                date == state.selectedDate
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // 获取选中日期的订阅事件
    val subscriptionEventsForSelectedDate: StateFlow<List<Pair<SubscriptionEvent, SubscriptionType>>> =
        getSubscriptionEventsFlow { selectedDate ->
            val dateMillis = selectedDate.atStartOfDay(
                java.time.ZoneId.systemDefault()
            ).toInstant().toEpochMilli()
            return@getSubscriptionEventsFlow { subscription -> 
                subscriptionRepository?.getEventsByDate(dateMillis, subscription.id) 
                    ?: flowOf(emptyList())
            }
        }

    // 获取未来5天的订阅事件（用于天气卡片显示5日预报）
    val subscriptionEventsForNext5Days: StateFlow<List<Pair<SubscriptionEvent, SubscriptionType>>> =
        getSubscriptionEventsFlow { selectedDate ->
            val startDateMillis = selectedDate.atStartOfDay(
                java.time.ZoneId.systemDefault()
            ).toInstant().toEpochMilli()
            val endDateMillis = selectedDate.plusDays(5).atStartOfDay(
                java.time.ZoneId.systemDefault()
            ).toInstant().toEpochMilli()
            return@getSubscriptionEventsFlow { subscription -> 
                subscriptionRepository?.getEventsBetween(startDateMillis, endDateMillis, subscription.id)
                    ?: flowOf(emptyList())
            }
        }

    /**
     * 通用的订阅事件获取逻辑，减少代码重复
     * 优化：使用 distinctUntilChanged 确保只在 selectedDate 真正改变时才重新查询
     */
    private fun getSubscriptionEventsFlow(
        getEventFlow: (LocalDate) -> (com.example.calendar.data.Subscription) -> Flow<List<SubscriptionEvent>>
    ): StateFlow<List<Pair<SubscriptionEvent, SubscriptionType>>> {
        return if (subscriptionRepository != null) {
            combine(
                subscriptionRepository.getAllSubscriptions(),
                uiState.map { it.selectedDate }.distinctUntilChanged() // 只在日期真正改变时才触发
            ) { subscriptions: List<com.example.calendar.data.Subscription>, selectedDate: LocalDate ->
                val enabledSubscriptions = subscriptions.filter { it.enabled }
                if (enabledSubscriptions.isEmpty()) {
                    flowOf<List<Pair<SubscriptionEvent, SubscriptionType>>>(emptyList())
                } else {
                    val eventFlowGetter = getEventFlow(selectedDate)
                    val eventFlows: List<Flow<List<Pair<SubscriptionEvent, SubscriptionType>>>> = 
                        enabledSubscriptions.map { subscription ->
                            eventFlowGetter(subscription)
                                .map { events ->
                                    events.map { event -> Pair(event, subscription.type) }
                                }
                        }
                    combine(eventFlows) { arrays: Array<List<Pair<SubscriptionEvent, SubscriptionType>>> ->
                        arrays.flatMap { it }
                    }
                }
            }.flatMapLatest { it }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )
        } else {
            MutableStateFlow<List<Pair<SubscriptionEvent, SubscriptionType>>>(emptyList()).asStateFlow()
        }
    }

    fun startCreateEvent() {
        _uiState.value = _uiState.value.copy(
            isEditing = true,
            editingEvent = null
        )
    }

    fun startEditEvent(event: Event) {
        _uiState.value = _uiState.value.copy(
            isEditing = true,
            editingEvent = event
        )
    }

    fun dismissEditor() {
        _uiState.value = _uiState.value.copy(
            isEditing = false,
            editingEvent = null
        )
    }

    fun changeViewMode(mode: CalendarViewMode) {
        _uiState.value = _uiState.value.copy(viewMode = mode)
    }

    fun selectDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
        // 检查是否需要同步该日期的数据
        checkAndSyncDateIfNeeded(date)
    }

    fun goToToday() {
        val today = LocalDate.now()
        _uiState.value = _uiState.value.copy(selectedDate = today)
        checkAndSyncDateIfNeeded(today)
    }

    fun goToPrevious() {
        val state = _uiState.value
        val newDate = when (state.viewMode) {
            CalendarViewMode.MONTH -> state.selectedDate.minusMonths(1)
            CalendarViewMode.WEEK -> state.selectedDate.minusWeeks(1)
            CalendarViewMode.DAY -> state.selectedDate.minusDays(1)
        }
        _uiState.value = state.copy(selectedDate = newDate)
        // 检查是否需要同步该日期的数据
        checkAndSyncDateIfNeeded(newDate)
    }

    fun goToNext() {
        val state = _uiState.value
        val newDate = when (state.viewMode) {
            CalendarViewMode.MONTH -> state.selectedDate.plusMonths(1)
            CalendarViewMode.WEEK -> state.selectedDate.plusWeeks(1)
            CalendarViewMode.DAY -> state.selectedDate.plusDays(1)
        }
        _uiState.value = state.copy(selectedDate = newDate)
        // 检查是否需要同步该日期的数据
        checkAndSyncDateIfNeeded(newDate)
    }

    /**
     * 检查并同步指定日期的数据（如果不存在）
     */
    private fun checkAndSyncDateIfNeeded(date: LocalDate) {
        val subscriptionRepository = subscriptionRepository ?: return
        
        // 如果该日期正在同步中，跳过
        if (syncingDates.contains(date)) {
            return
        }

        viewModelScope.launch {
            val subscriptions = subscriptionRepository.getAllSubscriptions()
                .firstOrNull() ?: emptyList()
            
            subscriptions.filter { it.enabled && it.type == SubscriptionType.HUANGLI }
                .forEach { subscription ->
                    // 标记该日期正在同步
                    syncingDates.add(date)
                    try {
                        // 使用智能同步方法：先检查数据是否存在，不存在才同步
                        subscriptionRepository.syncHuangliSubscriptionIfNeeded(subscription, date)
                    } finally {
                        // 同步完成后，移除标记
                        syncingDates.remove(date)
                    }
                }
        }
    }

    /**
     * 强制刷新当前选中日期的订阅数据
     * 用于从订阅页面返回主页面时，确保新订阅的数据能够立即显示
     */
    fun refreshSubscriptionDataIfNeeded() {
        val subscriptionRepository = subscriptionRepository ?: return
        val selectedDate = _uiState.value.selectedDate
        
        // 如果该日期正在同步中，跳过
        if (syncingDates.contains(selectedDate)) {
            return
        }
        
        viewModelScope.launch {
            val subscriptions = subscriptionRepository.getAllSubscriptions()
                .firstOrNull() ?: emptyList()
            
            val huangliSubscriptions = subscriptions.filter { it.enabled && it.type == SubscriptionType.HUANGLI }
            if (huangliSubscriptions.isNotEmpty()) {
                // 标记该日期正在同步
                syncingDates.add(selectedDate)
                try {
                    huangliSubscriptions.forEach { subscription ->
                        // 使用智能同步方法：先检查数据是否存在，不存在才同步
                        subscriptionRepository.syncHuangliSubscriptionIfNeeded(subscription, selectedDate)
                    }
                } finally {
                    // 同步完成后，移除标记
                    syncingDates.remove(selectedDate)
                }
            }
        }
    }

    fun saveEvent(event: Event, reminderMinutes: Int?, repeatCount: Int = 0) {
        viewModelScope.launch {
            try {
                repository.upsertEventWithReminder(event, reminderMinutes, repeatCount)
            } catch (e: Exception) {
                // 静默处理错误，避免应用崩溃
            }
        }
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch {
            repository.deleteEventWithReminders(event)
        }
    }

    /**
     * 导出"当前选中日期"的所有事件为 iCalendar(.ics) 文本。
     *
     * 注意：该方法是同步的，直接使用 StateFlow 当前值，不会触发额外数据库读取。
     */
    fun exportSelectedDateEventsAsIcs(): String {
        return exportEvents(eventsForSelectedDate.value)
    }

    /**
     * 导出指定日期范围的事件。
     */
    fun exportEventsAsIcs(startDate: LocalDate, endDate: LocalDate): String {
        val systemZoneId = java.time.ZoneId.systemDefault()
        val startTime = startDate.atStartOfDay(systemZoneId).toInstant().toEpochMilli()
        val endTime = endDate.plusDays(1).atStartOfDay(systemZoneId).toInstant().toEpochMilli()
        
        val events = allEvents.value.filter { event ->
            event.dtStart >= startTime && event.dtStart < endTime
        }
        return exportEvents(events)
    }

    /**
     * 导出所有事件。
     */
    fun exportAllEventsAsIcs(): String {
        return exportEvents(allEvents.value)
    }

    /**
     * 从 ICS 内容导入事件。
     */
    fun importEventsFromIcs(icsContent: String, onConflict: Boolean = true) {
        viewModelScope.launch {
            val result = repository.importEventsFromIcs(icsContent, onConflict)
            _uiState.value = _uiState.value.copy(importResult = result)
        }
    }

    /**
     * 清除导入结果。
     */
    fun clearImportResult() {
        _uiState.value = _uiState.value.copy(importResult = null)
    }
    
    // 同步错误回调
    var onSyncError: ((String) -> Unit)? = null

    /**
     * 同步天气订阅（用于城市切换后重新获取天气数据）
     */
    fun syncWeatherSubscription() {
        val subscriptionRepository = subscriptionRepository ?: return
        viewModelScope.launch {
            val subscriptions = subscriptionRepository.getAllSubscriptions()
                .firstOrNull() ?: emptyList()
            
            subscriptions.filter { it.enabled && it.type == SubscriptionType.WEATHER }
                .forEach { subscription ->
                    val result = subscriptionRepository.syncSubscription(subscription)
                    if (!result.success) {
                        onSyncError?.invoke("同步失败，请稍后重试")
                    }
                }
        }
    }

    /**
     * 公共导出方法，避免重复调用逻辑。
     */
    private fun exportEvents(events: List<Event>): String {
        return IcsExporter.export(events)
    }
}


