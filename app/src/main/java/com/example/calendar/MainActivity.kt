@file:Suppress("SpellCheckingInspection")

package com.example.calendar

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.AutoMode
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calendar.data.EventRepository
import com.example.calendar.data.SubscriptionRepository
import com.example.calendar.ui.CalendarViewMode
import com.example.calendar.ui.CalendarViewModel
import com.example.calendar.ui.SubscriptionViewModel
import com.example.calendar.ui.CalendarScreen
import com.example.calendar.ui.EventEditorDialog
import com.example.calendar.ui.SubscriptionScreen
import com.example.calendar.ui.ImportConfirmDialog
import com.example.calendar.ui.ImportResultDialog
import com.example.calendar.ui.ExportOptionsDialog
import com.example.calendar.ui.DateRangePickerDialog
import com.example.calendar.ui.ExportConfirmDialog
import com.example.calendar.ui.theme.CalendarTheme
import com.example.calendar.util.ThemeMode
import com.example.calendar.util.rememberThemeManager
import com.example.calendar.util.IcsImporter
import com.example.calendar.util.calculateDarkTheme
import com.example.calendar.util.formatDate
import com.example.calendar.util.getWeekNumber
import kotlinx.coroutines.launch
import java.io.InputStream

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 从 Application 统一获取依赖
        val app = application as CalendarApp
        val eventRepository = app.eventRepository
        val subscriptionRepository = app.subscriptionRepository

        setContent {
            val themeManager = rememberThemeManager()
            val systemDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
            var userThemeMode by remember { mutableStateOf(themeManager.getThemeMode()) }
            
            // 根据用户主题模式和系统主题计算当前主题
            val currentDarkTheme = remember(userThemeMode, systemDarkTheme) {
                calculateDarkTheme(userThemeMode, systemDarkTheme)
            }
            
            CalendarTheme(darkTheme = currentDarkTheme) {
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
                val (showExportOptionsDialog, setShowExportOptionsDialog) = remember { mutableStateOf(false) }
                val (showDateRangePickerDialog, setShowDateRangePickerDialog) = remember { mutableStateOf(false) }
                val (exportDialogVisible, setExportDialogVisible) = remember { mutableStateOf(false) }
                val (exportIcsText, setExportIcsText) = remember { mutableStateOf("") }
                val (exportEventCount, setExportEventCount) = remember { mutableStateOf(0) }
                val (exportDateRange, setExportDateRange) = remember { mutableStateOf<String?>(null) }
                val (dateRangeStart, setDateRangeStart) = remember { mutableStateOf(java.time.LocalDate.now()) }
                val (dateRangeEnd, setDateRangeEnd) = remember { mutableStateOf(java.time.LocalDate.now()) }
                val (importConfirmDialogVisible, setImportConfirmDialogVisible) = remember { mutableStateOf(false) }
                val (pendingIcsContent, setPendingIcsContent) = remember { mutableStateOf<String?>(null) }
                val (pendingImportEvents, setPendingImportEvents) = remember { mutableStateOf<List<com.example.calendar.data.Event>>(emptyList()) }
                val (importConflictCount, setImportConflictCount) = remember { mutableStateOf(0) } // 冲突数量
                val (importConflictStrategy, setImportConflictStrategy) = remember { mutableStateOf(true) } // true=覆盖, false=跳过
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                val context = LocalContext.current

                // 设置同步错误回调
                LaunchedEffect(Unit) {
                    vm.onSyncError = { errorMessage ->
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                errorMessage,
                                duration = androidx.compose.material3.SnackbarDuration.Long
                            )
                        }
                    }
                    subscriptionVm.onSyncError = { errorMessage ->
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                errorMessage,
                                duration = androidx.compose.material3.SnackbarDuration.Long
                            )
                        }
                    }
                }

                // 监听从订阅页面返回主页面，刷新订阅数据
                val previousShowSubscriptionScreen = remember { mutableStateOf(false) }
                LaunchedEffect(showSubscriptionScreen) {
                    // 当从订阅页面（true）返回主页面（false）时，刷新订阅数据
                    if (previousShowSubscriptionScreen.value && !showSubscriptionScreen) {
                        vm.refreshSubscriptionDataIfNeeded()
                    }
                    previousShowSubscriptionScreen.value = showSubscriptionScreen
                }

                // 文件选择器（用于导入）
                val filePickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->
                    uri?.let { selectedUri ->
                        try {
                            val inputStream: InputStream? = context.contentResolver.openInputStream(selectedUri)
                            inputStream?.use { stream ->
                                val content = stream.bufferedReader().use { it.readText() }
                                setPendingIcsContent(content)
                                // 解析并显示将要导入的事件数量
                                try {
                                    val events = IcsImporter.parse(content)
                                    if (events.isNotEmpty()) {
                                        setPendingImportEvents(events)
                                        // 检查冲突
                                        scope.launch {
                                            val conflictCount = eventRepository.checkImportConflicts(events)
                                            setImportConflictCount(conflictCount)
                                            setImportConfirmDialogVisible(true)
                                        }
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                            "文件中没有找到有效的日程。请确认文件是标准的日历格式（.ics）文件，且包含日程信息。",
                                            duration = androidx.compose.material3.SnackbarDuration.Long
                                        )
                                        }
                                    }
                                } catch (e: IcsImporter.IcsParseException) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            "文件格式错误：${e.message ?: "无法解析日历文件"}\n请确认这是一个有效的 .ics 格式文件。",
                                            duration = androidx.compose.material3.SnackbarDuration.Long
                                        )
                                    }
                                } catch (e: Exception) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            "导入失败：${e.message ?: "未知错误"}\n请确认文件格式正确且未被损坏。",
                                            duration = androidx.compose.material3.SnackbarDuration.Long
                                        )
                                    }
                                }
                            }
                        } catch (e: java.io.FileNotFoundException) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "文件不存在或无法访问。请确认文件路径正确且有读取权限。",
                                    duration = androidx.compose.material3.SnackbarDuration.Long
                                )
                            }
                        } catch (e: java.io.IOException) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "读取文件失败：文件可能被其他程序占用或已损坏。请重试或选择其他文件。",
                                    duration = androidx.compose.material3.SnackbarDuration.Long
                                )
                            }
                        } catch (e: Exception) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "读取文件失败：${e.message ?: "未知错误"}\n请确认文件格式正确。",
                                    duration = androidx.compose.material3.SnackbarDuration.Long
                                )
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
                                    writer.write(exportIcsText)
                                }
                            }
                            scope.launch {
                                snackbarHostState.showSnackbar("日程已成功导出到文件")
                            }
                        } catch (e: java.io.IOException) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "保存文件失败：无法写入文件。请确认存储空间充足且有写入权限。",
                                    duration = androidx.compose.material3.SnackbarDuration.Long
                                )
                            }
                        } catch (e: java.lang.SecurityException) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "保存文件失败：没有写入权限。请在系统设置中授予存储权限。",
                                    duration = androidx.compose.material3.SnackbarDuration.Long
                                )
                            }
                        } catch (e: Exception) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "保存文件失败：${e.message ?: "未知错误"}\n请重试或选择其他保存位置。",
                                    duration = androidx.compose.material3.SnackbarDuration.Long
                                )
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
                                    CalendarTopBarTitle(
                                        viewMode = uiState.viewMode,
                                        selectedDate = uiState.selectedDate,
                                        onPrevious = { vm.goToPrevious() },
                                        onNext = { vm.goToNext() }
                                    )
                                },
                            actions = {
                                // 快速选中今天按钮（在所有视图显示）
                                IconButton(onClick = { vm.goToToday() }) {
                                    Icon(
                                        Icons.Filled.Today,
                                        contentDescription = "今天",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                
                                // 右侧三个点菜单 - 显示下拉菜单
                                var showMenu by remember { mutableStateOf(false) }
                                var showThemeSubMenu by remember { mutableStateOf(false) }
                                var primaryMenuWidth by remember { mutableIntStateOf(0) }
                                
                                Box {
                                    IconButton(onClick = { showMenu = true }) {
                                        Icon(Icons.Filled.MoreVert, contentDescription = "更多")
                                    }
                                    
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { 
                                            showMenu = false
                                            showThemeSubMenu = false
                                        },
                                        offset = DpOffset(x = (-8).dp, y = 0.dp), // 增加与右侧的间距
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .onGloballyPositioned { coordinates ->
                                                primaryMenuWidth = coordinates.size.width
                                            }
                                    ) {
                                        DropdownMenuItem(
                                            text = { 
                                                Text(
                                                    "导入日程",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            },
                                            onClick = {
                                                showMenu = false
                                                filePickerLauncher.launch("text/calendar")
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Filled.FileUpload,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { 
                                                Text(
                                                    "导出日程",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            },
                                            onClick = {
                                                showMenu = false
                                                setShowExportOptionsDialog(true)
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Filled.FileDownload,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { 
                                                Text(
                                                    "订阅管理",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            },
                                            onClick = {
                                                showMenu = false
                                                setShowSubscriptionScreen(true)
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Filled.Settings,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        )
                                        // 主题选项 - 带嵌套子菜单
                                        val density = LocalDensity.current
                                        val densityValue = density.density
                                        var themeMenuItemY by remember { mutableStateOf(0.dp) }
                                        var themeMenuItemHeight by remember { mutableStateOf(0.dp) }
                                        
                                        Box {
                                            DropdownMenuItem(
                                                text = { 
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            "主题",
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                        Icon(
                                                            Icons.AutoMirrored.Filled.ArrowForward,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp),
                                                            tint = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    showThemeSubMenu = true
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Filled.Palette,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                },
                                                modifier = Modifier.onGloballyPositioned { coordinates ->
                                                    themeMenuItemY = (coordinates.positionInParent().y / densityValue).dp
                                                    themeMenuItemHeight = (coordinates.size.height / densityValue).dp
                                                }
                                            )
                                            
                                            DropdownMenu(
                                                expanded = showThemeSubMenu,
                                                onDismissRequest = { showThemeSubMenu = false },
                                                offset = DpOffset(
                                                    x = if (primaryMenuWidth > 0) {
                                                        (primaryMenuWidth / densityValue).dp + 2.dp // 一级菜单宽度 + 小间距
                                                    } else {
                                                        200.dp + 2.dp // 默认宽度 + 小间距
                                                    },
                                                    y = themeMenuItemY - themeMenuItemHeight / 2 // 顶部对齐到按钮中心
                                                ),
                                                modifier = Modifier.padding(0.dp)
                                            ) {
                                                // 浅色模式选项
                                                DropdownMenuItem(
                                                    text = { 
                                                        Text(
                                                            "浅色模式",
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                    },
                                                    onClick = {
                                                        showThemeSubMenu = false
                                                        showMenu = false
                                                        themeManager.setThemeMode(ThemeMode.LIGHT)
                                                        userThemeMode = ThemeMode.LIGHT
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            Icons.Filled.LightMode,
                                                            contentDescription = null,
                                                            tint = if (userThemeMode == ThemeMode.LIGHT) 
                                                                MaterialTheme.colorScheme.primary 
                                                            else 
                                                                MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                )
                                                // 深色模式选项
                                                DropdownMenuItem(
                                                    text = { 
                                                        Text(
                                                            "深色模式",
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                    },
                                                    onClick = {
                                                        showThemeSubMenu = false
                                                        showMenu = false
                                                        themeManager.setThemeMode(ThemeMode.DARK)
                                                        userThemeMode = ThemeMode.DARK
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            Icons.Filled.DarkMode,
                                                            contentDescription = null,
                                                            tint = if (userThemeMode == ThemeMode.DARK) 
                                                                MaterialTheme.colorScheme.primary 
                                                            else 
                                                                MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                )
                                                // 跟随系统选项
                                                DropdownMenuItem(
                                                    text = { 
                                                        Text(
                                                            "跟随系统",
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                    },
                                                    onClick = {
                                                        showThemeSubMenu = false
                                                        showMenu = false
                                                        themeManager.clearThemeMode()
                                                        userThemeMode = null
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            Icons.Filled.AutoMode,
                                                            contentDescription = null,
                                                            tint = if (userThemeMode == null) 
                                                                MaterialTheme.colorScheme.primary 
                                                            else 
                                                                MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                )
                                            }
                                        }
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
                                modifier = Modifier.size(48.dp),
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = "添加日程",
                                    modifier = Modifier.size(24.dp)
                                )
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
                                contentPadding = innerPadding,
                                currentSelectedDate = uiState.selectedDate // 传递当前选中的日期
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
                    ImportConfirmDialog(
                        visible = importConfirmDialogVisible && pendingIcsContent != null && pendingImportEvents.isNotEmpty(),
                        pendingEvents = pendingImportEvents,
                        conflictCount = importConflictCount,
                        conflictStrategy = importConflictStrategy,
                        onConflictStrategyChange = { setImportConflictStrategy(it) },
                        onConfirm = {
                            vm.importEventsFromIcs(pendingIcsContent!!, onConflict = importConflictStrategy)
                            setImportConfirmDialogVisible(false)
                            setPendingIcsContent(null)
                            setPendingImportEvents(emptyList())
                            setImportConflictCount(0)
                        },
                        onDismiss = {
                            setImportConfirmDialogVisible(false)
                            setPendingIcsContent(null)
                            setPendingImportEvents(emptyList())
                            setImportConflictCount(0)
                        }
                    )

                    // 导入结果对话框
                    uiState.importResult?.let { result ->
                        ImportResultDialog(
                            result = result,
                            onDismiss = { vm.clearImportResult() }
                        )
                    }

                    // 日期范围选择对话框
                    DateRangePickerDialog(
                        visible = showDateRangePickerDialog,
                        startDate = dateRangeStart,
                        endDate = dateRangeEnd,
                        allEvents = allEvents,
                        onStartDateChange = { setDateRangeStart(it) },
                        onEndDateChange = { setDateRangeEnd(it) },
                        onConfirm = {
                            // 基于最终选择的日期范围计算事件数量
                            val systemZoneId = java.time.ZoneId.systemDefault()
                            val startTime = dateRangeStart.atStartOfDay(systemZoneId).toInstant().toEpochMilli()
                            val endTime = dateRangeEnd.plusDays(1).atStartOfDay(systemZoneId).toInstant().toEpochMilli()
                            val finalEventsCount = allEvents.count { event ->
                                event.dtStart >= startTime && event.dtStart < endTime
                            }
                            
                            val icsContent = vm.exportEventsAsIcs(dateRangeStart, dateRangeEnd)
                            setExportIcsText(icsContent)
                            setExportEventCount(finalEventsCount)
                            setExportDateRange("${dateRangeStart.formatDate()} 至 ${dateRangeEnd.formatDate()}")
                            setShowDateRangePickerDialog(false)
                            setExportDialogVisible(true)
                        },
                        onDismiss = { setShowDateRangePickerDialog(false) }
                    )

                    // 导出选项对话框
                    ExportOptionsDialog(
                        visible = showExportOptionsDialog,
                        selectedDate = uiState.selectedDate,
                        eventsForSelectedDate = eventsForSelectedDate,
                        allEvents = allEvents,
                        onOptionSelected = { option ->
                            when (option) {
                                0 -> {
                                    // 当前日期
                                    val events = eventsForSelectedDate
                                    val icsContent = vm.exportSelectedDateEventsAsIcs()
                                    setExportIcsText(icsContent)
                                    setExportEventCount(events.size)
                                    setExportDateRange(uiState.selectedDate.formatDate())
                                    setShowExportOptionsDialog(false)
                                    setExportDialogVisible(true)
                                }
                                1 -> {
                                    // 自定义日期范围 - 打开日期范围选择器
                                    setShowExportOptionsDialog(false)
                                    setShowDateRangePickerDialog(true)
                                }
                                else -> {
                                    // 所有日程
                                    val icsContent = vm.exportAllEventsAsIcs()
                                    setExportIcsText(icsContent)
                                    setExportEventCount(allEvents.size)
                                    setExportDateRange(null)
                                    setShowExportOptionsDialog(false)
                                    setExportDialogVisible(true)
                                }
                            }
                        },
                        onDismiss = { setShowExportOptionsDialog(false) }
                    )

                    // 导出确认对话框
                    val exportFileName = if (exportDateRange != null) {
                        "日程_${exportDateRange}.ics"
                    } else {
                        "日程_全部_${java.time.LocalDate.now().formatDate()}.ics"
                    }
                    
                    ExportConfirmDialog(
                        visible = exportDialogVisible && exportIcsText.isNotEmpty(),
                        eventCount = exportEventCount,
                        dateRange = exportDateRange,
                        fileName = exportFileName,
                        onConfirm = {
                            fileSaverLauncher.launch(exportFileName)
                            setExportDialogVisible(false)
                        },
                        onDismiss = { setExportDialogVisible(false) }
                    )

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
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
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
        if (modelClass.isAssignableFrom(SubscriptionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SubscriptionViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

/**
 * 导航图标按钮组件
 */
@Composable
private fun NavigationIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp)
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * 前进图标按钮组件
 */
@Composable
private fun ForwardIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp)
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * 日历顶部导航栏标题组件
 */
@Composable
private fun CalendarTopBarTitle(
    viewMode: CalendarViewMode,
    selectedDate: java.time.LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 左箭头
        NavigationIconButton(
            onClick = onPrevious,
            contentDescription = when (viewMode) {
                CalendarViewMode.MONTH -> "上一月"
                CalendarViewMode.WEEK -> "上一周"
                CalendarViewMode.DAY -> "上一天"
            }
        )
        
        // 标题内容
        when (viewMode) {
            CalendarViewMode.MONTH, CalendarViewMode.WEEK -> {
                Column {
                    Text(
                        text = "${selectedDate.year}年${selectedDate.monthValue}月",
                        style = MaterialTheme.typography.titleLarge
                    )
                    val weekNumber = selectedDate.getWeekNumber()
                    Text(
                        text = "第${weekNumber}周",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            CalendarViewMode.DAY -> {
                Text(
                    text = "${selectedDate.year}年${selectedDate.monthValue}月${selectedDate.dayOfMonth}日",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
        
        // 右箭头
        ForwardIconButton(
            onClick = onNext,
            contentDescription = when (viewMode) {
                CalendarViewMode.MONTH -> "下一月"
                CalendarViewMode.WEEK -> "下一周"
                CalendarViewMode.DAY -> "下一天"
            }
        )
    }
}
