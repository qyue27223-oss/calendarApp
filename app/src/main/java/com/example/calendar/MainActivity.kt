package com.example.calendar

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.temporal.WeekFields
import java.util.Locale
import com.example.calendar.data.AppDatabase
import com.example.calendar.data.EventRepository
import com.example.calendar.data.SubscriptionRepository
import com.example.calendar.data.SubscriptionSyncManager
import com.example.calendar.reminder.ReminderScheduler
import com.example.calendar.ui.CalendarScreen
import com.example.calendar.ui.CalendarViewModel
import com.example.calendar.ui.EventEditorDialog
import com.example.calendar.ui.SubscriptionScreen
import com.example.calendar.ui.SubscriptionViewModel
import com.example.calendar.ui.theme.CalendarTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import java.io.InputStream

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        createNotificationChannel()

        // 简单的手动依赖注入
        val database = AppDatabase.getInstance(applicationContext)
        val scheduler = ReminderScheduler(applicationContext)
        val eventRepository = EventRepository(
            eventDao = database.eventDao(),
            reminderDao = database.reminderDao(),
            reminderScheduler = scheduler
        )
        val subscriptionRepository = SubscriptionRepository(
            subscriptionDao = database.subscriptionDao(),
            subscriptionEventDao = database.subscriptionEventDao(),
            context = applicationContext
        )

        // 启动定时同步任务
        SubscriptionSyncManager.startPeriodicSync(applicationContext)

        // 应用启动时检查并同步订阅数据
        syncSubscriptionsOnStartup(subscriptionRepository)

        setContent {
            CalendarTheme {
                val vm: CalendarViewModel = viewModel(
                    factory = CalendarViewModelFactory(eventRepository, subscriptionRepository)
                )
                val subscriptionVm: SubscriptionViewModel = viewModel(
                    factory = SubscriptionViewModelFactory(subscriptionRepository)
                )
                val uiState by vm.uiState.collectAsState()
                val allEvents by vm.allEvents.collectAsState()
                val eventsForSelectedDate by vm.eventsForSelectedDate.collectAsState()
                val subscriptionEventsForSelectedDate by vm.subscriptionEventsForSelectedDate.collectAsState()
                val subscriptionEventsForNext5Days by vm.subscriptionEventsForNext5Days.collectAsState()
                val (showSubscriptionScreen, setShowSubscriptionScreen) = remember { mutableStateOf(false) }
                val (icsDialogVisible, setIcsDialogVisible) = remember { mutableStateOf(false) }
                val (icsText, setIcsText) = remember { mutableStateOf("") }
                val (importConfirmDialogVisible, setImportConfirmDialogVisible) = remember { mutableStateOf(false) }
                val (pendingIcsContent, setPendingIcsContent) = remember { mutableStateOf<String?>(null) }
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                val context = LocalContext.current
                
                // 订阅同步结果提示已移除（使用自动同步机制）

                // 文件选择器（用于导入）
                val filePickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->
                    uri?.let {
                        try {
                            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                            inputStream?.use { stream ->
                                val content = stream.bufferedReader().use { it.readText() }
                                setPendingIcsContent(content)
                                // 解析并显示将要导入的事件数量
                                try {
                                    val events = com.example.calendar.util.IcsImporter.parse(content)
                                    if (events.isNotEmpty()) {
                                        setImportConfirmDialogVisible(true)
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("文件中没有找到有效的事件")
                                        }
                                    }
                                } catch (e: Exception) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("文件格式错误: ${e.message}")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            scope.launch {
                                snackbarHostState.showSnackbar("读取文件失败: ${e.message}")
                            }
                        }
                    }
                }

                // 文件保存器（用于导出）
                val fileSaverLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("text/calendar")
                ) { uri ->
                    uri?.let {
                        try {
                            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                                outputStream.bufferedWriter().use { writer ->
                                    writer.write(icsText)
                                }
                            }
                            scope.launch {
                                snackbarHostState.showSnackbar("文件已保存")
                            }
                        } catch (e: Exception) {
                            scope.launch {
                                snackbarHostState.showSnackbar("保存文件失败: ${e.message}")
                            }
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        // 根据是否显示订阅管理界面来显示不同的头部
                        if (showSubscriptionScreen) {
                            // 订阅管理头部
                            TopAppBar(
                                title = { 
                                    Text(
                                        text = "订阅服务",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                },
                                navigationIcon = {
                                    IconButton(onClick = { setShowSubscriptionScreen(false) }) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "返回"
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors()
                            )
                        } else {
                            // 日历主页面头部
                            TopAppBar(
                                title = {
                                    // 根据视图模式显示不同的标题
                                    when (uiState.viewMode) {
                                    com.example.calendar.ui.CalendarViewMode.MONTH -> {
                                        Row(
                                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            // 左箭头
                                            IconButton(
                                                onClick = { vm.goToPrevious() },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = "上一月",
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            
                                            // 年月和周数
                                            Column {
                                                Text(
                                                    text = "${uiState.selectedDate.year}年${uiState.selectedDate.monthValue}月",
                                                    style = MaterialTheme.typography.titleLarge
                                                )
                                                val weekNumber = getWeekNumber(uiState.selectedDate)
                                                Text(
                                                    text = "第${weekNumber}周",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            
                                            // 右箭头
                                            IconButton(
                                                onClick = { vm.goToNext() },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.ArrowForward,
                                                    contentDescription = "下一月",
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                    com.example.calendar.ui.CalendarViewMode.WEEK -> {
                                        Row(
                                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            // 左箭头
                                            IconButton(
                                                onClick = { vm.goToPrevious() },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = "上一周",
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            
                                            // 年月和周数
                                            Column {
                                                Text(
                                                    text = "${uiState.selectedDate.year}年${uiState.selectedDate.monthValue}月",
                                                    style = MaterialTheme.typography.titleLarge
                                                )
                                                val weekNumber = getWeekNumber(uiState.selectedDate)
                                                Text(
                                                    text = "第${weekNumber}周",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            
                                            // 右箭头
                                            IconButton(
                                                onClick = { vm.goToNext() },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.ArrowForward,
                                                    contentDescription = "下一周",
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                    com.example.calendar.ui.CalendarViewMode.DAY -> {
                                        Row(
                                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            // 左箭头
                                            IconButton(
                                                onClick = { vm.goToPrevious() },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = "上一天",
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            
                                            // 日期
                                            Text(
                                                text = "${uiState.selectedDate.year}年${uiState.selectedDate.monthValue}月${uiState.selectedDate.dayOfMonth}日",
                                                style = MaterialTheme.typography.titleLarge
                                            )
                                            
                                            // 右箭头
                                            IconButton(
                                                onClick = { vm.goToNext() },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.ArrowForward,
                                                    contentDescription = "下一天",
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            actions = {
                                // 快速选中今天按钮（只在月视图和周视图显示）
                                if (uiState.viewMode == com.example.calendar.ui.CalendarViewMode.MONTH || 
                                    uiState.viewMode == com.example.calendar.ui.CalendarViewMode.WEEK) {
                                    IconButton(onClick = { vm.goToToday() }) {
                                        Icon(
                                            Icons.Filled.Today,
                                            contentDescription = "今天",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                
                                // 右侧三个点菜单 - 显示下拉菜单
                                var showMenu by remember { mutableStateOf(false) }
                                
                                Box {
                                    IconButton(onClick = { showMenu = true }) {
                                        Icon(Icons.Filled.MoreVert, contentDescription = "更多")
                                    }
                                    
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("导入 ICS") },
                                            onClick = {
                                                showMenu = false
                                                filePickerLauncher.launch("text/calendar")
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("导出为 ICS") },
                                            onClick = {
                                                showMenu = false
                                                setIcsText(vm.exportSelectedDateEventsAsIcs())
                                                setIcsDialogVisible(true)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("订阅管理") },
                                            onClick = {
                                                showMenu = false
                                                setShowSubscriptionScreen(true)
                                            }
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors()
                        )
                    }
                    },
                    floatingActionButton = {
                        // 只在日历主视图显示日程添加按钮（不在订阅管理界面显示）
                        if (!showSubscriptionScreen) {
                            FloatingActionButton(
                                onClick = { vm.startCreateEvent() },
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "添加日程")
                            }
                        }
                    }
                ) { innerPadding ->
                    // Snackbar 用于显示提示信息
                    SnackbarHost(hostState = snackbarHostState)
                    
                    // 订阅管理界面 - 使用 Box 确保正确显示和返回
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (showSubscriptionScreen) {
                            SubscriptionScreen(
                                viewModel = subscriptionVm,
                                onBack = { setShowSubscriptionScreen(false) },
                                contentPadding = innerPadding
                            )
                        } else {
                            // 日历界面
                            CalendarScreen(
                                uiState = uiState,
                                onViewModeChange = { mode -> vm.changeViewMode(mode) },
                                onDateSelected = { date -> vm.selectDate(date) },
                                dayEvents = eventsForSelectedDate,
                                allEvents = allEvents,
                                subscriptionEvents = subscriptionEventsForSelectedDate,
                                subscriptionEventsForNext5Days = subscriptionEventsForNext5Days,
                                onDayEventClick = { event -> vm.startEditEvent(event) },
                                onCityChanged = { vm.syncWeatherSubscription() },
                                contentPadding = innerPadding
                            )
                        }
                    }
                    
                    // 导入确认对话框
                    if (importConfirmDialogVisible && pendingIcsContent != null) {
                        val eventCount = try {
                            com.example.calendar.util.IcsImporter.parse(pendingIcsContent).size
                        } catch (e: Exception) {
                            0
                        }
                        AlertDialog(
                            onDismissRequest = { 
                                setImportConfirmDialogVisible(false)
                                setPendingIcsContent(null)
                            },
                            title = { Text(text = "确认导入") },
                            text = { Text(text = "将导入 $eventCount 个事件，是否继续？") },
                            confirmButton = {
                                TextButton(onClick = {
                                    pendingIcsContent?.let { content ->
                                        vm.importEventsFromIcs(content, onConflict = true)
                                    }
                                    setImportConfirmDialogVisible(false)
                                    setPendingIcsContent(null)
                                }) {
                                    Text("导入")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = {
                                    setImportConfirmDialogVisible(false)
                                    setPendingIcsContent(null)
                                }) {
                                    Text("取消")
                                }
                            }
                        )
                    }

                    // 导入结果对话框
                    uiState.importResult?.let { result ->
                        AlertDialog(
                            onDismissRequest = { vm.clearImportResult() },
                            title = { Text(text = "导入完成") },
                            text = {
                                Column(
                                    modifier = Modifier
                                        .verticalScroll(rememberScrollState())
                                        .padding(vertical = 8.dp)
                                ) {
                                    Text(text = "总计: ${result.total} 个事件")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = "成功导入: ${result.imported} 个")
                                    if (result.skipped > 0) {
                                        Text(text = "跳过: ${result.skipped} 个")
                                    }
                                    if (result.errors.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(text = "错误:")
                                        result.errors.forEach { error ->
                                            Text(
                                                text = "• $error",
                                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { vm.clearImportResult() }) {
                                    Text("确定")
                                }
                            }
                        )
                    }

                    // 导出对话框
                    if (icsDialogVisible) {
                        AlertDialog(
                            onDismissRequest = { setIcsDialogVisible(false) },
                            title = { Text(text = "导出为 ICS") },
                            text = {
                                Column {
                                    SelectionContainer {
                                        Text(text = icsText)
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    val fileName = "calendar_export_${System.currentTimeMillis()}.ics"
                                    fileSaverLauncher.launch(fileName)
                                }) {
                                    Text("保存到文件")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { setIcsDialogVisible(false) }) {
                                    Text("关闭")
                                }
                            }
                        )
                    }

                    EventEditorDialog(
                        visible = uiState.isEditing,
                        date = uiState.selectedDate,
                        editingEvent = uiState.editingEvent,
                        onDismiss = { vm.dismissEditor() },
                        onSave = { event, minutes, repeatCount ->
                            vm.saveEvent(event, reminderMinutes = minutes, repeatCount = repeatCount)
                        },
                        onDelete = uiState.editingEvent?.let { editing ->
                            {
                                vm.deleteEvent(editing)
                                vm.dismissEditor()
                            }
                        }
                    )

                }
            }
        }
    }
}

class CalendarViewModelFactory(
    private val repository: EventRepository,
    private val subscriptionRepository: SubscriptionRepository? = null
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(com.example.calendar.ui.CalendarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalendarViewModel(repository, subscriptionRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class SubscriptionViewModelFactory(
    private val repository: SubscriptionRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(com.example.calendar.ui.SubscriptionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SubscriptionViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

private const val REMINDER_CHANNEL_ID = "calendar_reminders"

/**
 * 计算日期所在的一年中的周数
 */
private fun getWeekNumber(date: java.time.LocalDate): Int {
    val weekFields = WeekFields.of(Locale.getDefault())
    return date.get(weekFields.weekOfWeekBasedYear())
}

private fun MainActivity.createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "日程提醒"
        val descriptionText = "日程提醒通知渠道"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(REMINDER_CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
}

/**
 * 应用启动时检查并同步订阅数据（超过24小时则同步）
 * 用户可以在订阅管理界面手动创建订阅
 */
private fun MainActivity.syncSubscriptionsOnStartup(
    subscriptionRepository: SubscriptionRepository
) {
    CoroutineScope(Dispatchers.IO).launch {
        val subscriptions = subscriptionRepository.getAllSubscriptions().firstOrNull() ?: emptyList()
        subscriptions.filter { it.enabled }.forEach { subscription ->
            if (subscriptionRepository.shouldSync(subscription, syncIntervalHours = 24)) {
                subscriptionRepository.syncSubscription(subscription)
            }
        }
    }
}