@file:Suppress("SpellCheckingInspection")

package com.example.calendar

import android.app.NotificationChannel
import android.app.NotificationManager
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import java.time.temporal.WeekFields
import java.util.Locale
import com.example.calendar.data.EventRepository
import com.example.calendar.data.SubscriptionRepository
import com.example.calendar.ui.CalendarViewMode
import com.example.calendar.ui.CalendarViewModel
import com.example.calendar.ui.SubscriptionViewModel
import com.example.calendar.ui.CalendarScreen
import com.example.calendar.ui.EventEditorDialog
import com.example.calendar.ui.SubscriptionScreen
import com.example.calendar.ui.theme.CalendarTheme
import com.example.calendar.util.ThemeMode
import com.example.calendar.util.rememberThemeManager
import com.example.calendar.util.IcsImporter
import com.example.calendar.util.calculateDarkTheme
import com.example.calendar.util.formatDate
import com.example.calendar.util.formatTime
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
                val (importConflictStrategy, setImportConflictStrategy) = remember { mutableStateOf(true) } // true=覆盖, false=跳过
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
                                    val events = IcsImporter.parse(content)
                                    if (events.isNotEmpty()) {
                                        setPendingImportEvents(events)
                                        setImportConfirmDialogVisible(true)
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
                                // 快速选中今天按钮（只在月视图和周视图显示）
                                if (uiState.viewMode == CalendarViewMode.MONTH || 
                                    uiState.viewMode == CalendarViewMode.WEEK) {
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
                    if (importConfirmDialogVisible && pendingIcsContent != null && pendingImportEvents.isNotEmpty()) {
                        AlertDialog(
                            onDismissRequest = { 
                                setImportConfirmDialogVisible(false)
                                setPendingIcsContent(null)
                                setPendingImportEvents(emptyList())
                            },
                            icon = {
                                Icon(
                                    Icons.Filled.FileUpload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            },
                            title = { 
                                Text(
                                    text = "确认导入日程",
                                    style = MaterialTheme.typography.titleLarge
                                ) 
                            },
                            text = { 
                                Column(
                                    modifier = Modifier.verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = "检测到 ${pendingImportEvents.size} 个日程，预览如下：",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(bottom = 12.dp),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    
                                    // 显示前5个日程预览
                                    pendingImportEvents.take(5).forEachIndexed { index, event ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 8.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp)
                                            ) {
                                                Text(
                                                    text = event.summary,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(bottom = 4.dp)
                                                )
                                                val startDateTime = java.time.Instant.ofEpochMilli(event.dtStart)
                                                    .atZone(java.time.ZoneId.systemDefault())
                                                    .toLocalDateTime()
                                                Text(
                                                    text = "${startDateTime.formatDate()} ${startDateTime.formatTime()}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                event.location?.let { location ->
                                                    Text(
                                                        text = "地点：$location",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(top = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    
                                    if (pendingImportEvents.size > 5) {
                                        Text(
                                            text = "还有 ${pendingImportEvents.size - 5} 个日程...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // 冲突处理选项
                                    Text(
                                        text = "冲突处理方式：",
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { setImportConflictStrategy(true) }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = importConflictStrategy,
                                            onClick = { setImportConflictStrategy(true) }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "覆盖已有日程",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = "如果存在相同 ID 的日程，将用新日程替换",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { setImportConflictStrategy(false) }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = !importConflictStrategy,
                                            onClick = { setImportConflictStrategy(false) }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "跳过已有日程",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = "如果存在相同 ID 的日程，将保留原有日程",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        vm.importEventsFromIcs(pendingIcsContent, onConflict = importConflictStrategy)
                                        setImportConfirmDialogVisible(false)
                                        setPendingIcsContent(null)
                                        setPendingImportEvents(emptyList())
                                    }
                                ) {
                                    Text("确认导入")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = {
                                    setImportConfirmDialogVisible(false)
                                    setPendingIcsContent(null)
                                    setPendingImportEvents(emptyList())
                                }) {
                                    Text("取消")
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    }

                    // 导入结果对话框
                    uiState.importResult?.let { result ->
                        val isSuccess = result.errors.isEmpty() && result.imported > 0
                        AlertDialog(
                            onDismissRequest = { vm.clearImportResult() },
                            icon = {
                                Icon(
                                    Icons.Filled.FileUpload,
                                    contentDescription = null,
                                    tint = if (isSuccess) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    },
                                    modifier = Modifier.size(32.dp)
                                )
                            },
                            title = { 
                                Text(
                                    text = if (isSuccess) "导入成功" else "导入完成",
                                    style = MaterialTheme.typography.titleLarge
                                ) 
                            },
                            text = {
                                Column(
                                    modifier = Modifier
                                        .verticalScroll(rememberScrollState())
                                        .padding(vertical = 4.dp)
                                ) {
                                    // 友好的统计信息
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column {
                                                    Text(
                                                        text = "${result.imported}",
                                                        style = MaterialTheme.typography.headlineMedium,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "成功导入",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                                if (result.skipped > 0) {
                                                    Column {
                                                        Text(
                                                            text = "${result.skipped}",
                                                            style = MaterialTheme.typography.headlineMedium,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                                        )
                                                        Text(
                                                            text = "已跳过",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                                        )
                                                    }
                                                }
                                                if (result.total > result.imported + result.skipped) {
                                                    Column {
                                                        Text(
                                                            text = "${result.total - result.imported - result.skipped}",
                                                            style = MaterialTheme.typography.headlineMedium,
                                                            color = MaterialTheme.colorScheme.error
                                                        )
                                                        Text(
                                                            text = "失败",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.error
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    
                                    // 错误信息（仅在出错时显示）
                                    if (result.errors.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "部分日程导入失败：",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        result.errors.take(3).forEach { error ->
                                            Text(
                                                text = "• ${error.replace("导入事件失败 (UID: ", "").replace("): .*$".toRegex(), "")}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                                            )
                                        }
                                        if (result.errors.size > 3) {
                                            Text(
                                                text = "还有 ${result.errors.size - 3} 个错误...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                Button(onClick = { vm.clearImportResult() }) {
                                    Text("确定")
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    }

                    // 日期范围选择对话框
                    if (showDateRangePickerDialog) {
                        var tempStartDate by remember { mutableStateOf(dateRangeStart) }
                        var tempEndDate by remember { mutableStateOf(dateRangeEnd) }
                        val dateRangeEventsCount = remember(tempStartDate, tempEndDate, allEvents) {
                            val systemZoneId = java.time.ZoneId.systemDefault()
                            val startTime = tempStartDate.atStartOfDay(systemZoneId).toInstant().toEpochMilli()
                            val endTime = tempEndDate.plusDays(1).atStartOfDay(systemZoneId).toInstant().toEpochMilli()
                            allEvents.count { event ->
                                event.dtStart >= startTime && event.dtStart < endTime
                            }
                        }
                        
                        AlertDialog(
                            onDismissRequest = { setShowDateRangePickerDialog(false) },
                            icon = {
                                Icon(
                                    Icons.Filled.FileDownload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            },
                            title = { 
                                Text(
                                    text = "选择日期范围",
                                    style = MaterialTheme.typography.titleLarge
                                ) 
                            },
                            text = {
                                Column {
                                    // 开始日期选择
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            Text(
                                                text = "开始日期",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(
                                                    onClick = { tempStartDate = tempStartDate.minusDays(1) }
                                                ) {
                                                    Icon(
                                                        Icons.AutoMirrored.Filled.ArrowBack,
                                                        contentDescription = "前一天"
                                                    )
                                                }
                                                Text(
                                                    text = tempStartDate.formatDate(),
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    modifier = Modifier.weight(1f),
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                                IconButton(
                                                    onClick = { 
                                                        tempStartDate = tempStartDate.plusDays(1)
                                                        if (tempStartDate > tempEndDate) {
                                                            tempEndDate = tempStartDate
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        Icons.AutoMirrored.Filled.ArrowForward,
                                                        contentDescription = "后一天"
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // 结束日期选择
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            Text(
                                                text = "结束日期",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(
                                                    onClick = { 
                                                        tempEndDate = tempEndDate.minusDays(1)
                                                        if (tempEndDate < tempStartDate) {
                                                            tempStartDate = tempEndDate
                                                        }
                                                    },
                                                    enabled = tempEndDate > tempStartDate
                                                ) {
                                                    Icon(
                                                        Icons.AutoMirrored.Filled.ArrowBack,
                                                        contentDescription = "前一天"
                                                    )
                                                }
                                                Text(
                                                    text = tempEndDate.formatDate(),
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    modifier = Modifier.weight(1f),
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                                IconButton(
                                                    onClick = { tempEndDate = tempEndDate.plusDays(1) }
                                                ) {
                                                    Icon(
                                                        Icons.AutoMirrored.Filled.ArrowForward,
                                                        contentDescription = "后一天"
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    Text(
                                        text = "此日期范围内共有 $dateRangeEventsCount 个日程",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    if (tempStartDate > tempEndDate) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "⚠ 开始日期不能晚于结束日期",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        if (tempStartDate <= tempEndDate && dateRangeEventsCount > 0) {
                                            val icsContent = vm.exportEventsAsIcs(tempStartDate, tempEndDate)
                                            setExportIcsText(icsContent)
                                            setExportEventCount(dateRangeEventsCount)
                                            setExportDateRange("${tempStartDate.formatDate()} 至 ${tempEndDate.formatDate()}")
                                            setDateRangeStart(tempStartDate)
                                            setDateRangeEnd(tempEndDate)
                                            setShowDateRangePickerDialog(false)
                                            setExportDialogVisible(true)
                                        }
                                    },
                                    enabled = tempStartDate <= tempEndDate && dateRangeEventsCount > 0
                                ) {
                                    Text("确定")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { setShowDateRangePickerDialog(false) }) {
                                    Text("取消")
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    }

                    // 导出选项对话框
                    if (showExportOptionsDialog) {
                        var selectedExportOption by remember { mutableStateOf(0) } // 0: 当前日期, 1: 日期范围, 2: 全部
                        AlertDialog(
                            onDismissRequest = { setShowExportOptionsDialog(false) },
                            icon = {
                                Icon(
                                    Icons.Filled.FileDownload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            },
                            title = { 
                                Text(
                                    text = "导出日程",
                                    style = MaterialTheme.typography.titleLarge
                                ) 
                            },
                            text = {
                                Column {
                                    // 导出选项
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedExportOption = 0 }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selectedExportOption == 0,
                                            onClick = { selectedExportOption = 0 }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "当前选中日期",
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Text(
                                                text = "${uiState.selectedDate.formatDate()} 的所有日程",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (eventsForSelectedDate.isNotEmpty()) {
                                            Text(
                                                text = "${eventsForSelectedDate.size} 个",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        } else {
                                            Text(
                                                text = "无日程",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontStyle = FontStyle.Italic
                                            )
                                        }
                                    }
                                    
                                    if (eventsForSelectedDate.isEmpty() && selectedExportOption == 0) {
                                        Text(
                                            text = "当前日期没有日程可导出",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(start = 48.dp, top = 4.dp)
                                        )
                                    }
                                    
                                    HorizontalDivider()
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedExportOption = 1 }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selectedExportOption == 1,
                                            onClick = { selectedExportOption = 1 }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "自定义日期范围",
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Text(
                                                text = "选择开始和结束日期",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    
                                    HorizontalDivider()
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedExportOption = 2 }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selectedExportOption == 2,
                                            onClick = { selectedExportOption = 2 }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "所有日程",
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Text(
                                                text = "导出所有已保存的日程",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (allEvents.isNotEmpty()) {
                                            Text(
                                                text = "${allEvents.size} 个",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        } else {
                                            Text(
                                                text = "无日程",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontStyle = FontStyle.Italic
                                            )
                                        }
                                    }
                                    
                                    if (allEvents.isEmpty() && selectedExportOption == 2) {
                                        Text(
                                            text = "当前没有日程可导出",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(start = 48.dp, top = 4.dp)
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        when (selectedExportOption) {
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
                                    enabled = when (selectedExportOption) {
                                        0 -> eventsForSelectedDate.isNotEmpty()
                                        1 -> true // 日期范围总是可用
                                        else -> allEvents.isNotEmpty()
                                    }
                                ) {
                                    Text("下一步")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { setShowExportOptionsDialog(false) }) {
                                    Text("取消")
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    }

                    // 导出确认对话框
                    if (exportDialogVisible && exportIcsText.isNotEmpty()) {
                        val fileName = if (exportDateRange != null) {
                            "日程_${exportDateRange}.ics"
                        } else {
                            "日程_全部_${java.time.LocalDate.now().formatDate()}.ics"
                        }
                        
                        AlertDialog(
                            onDismissRequest = { setExportDialogVisible(false) },
                            icon = {
                                Icon(
                                    Icons.Filled.FileDownload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            },
                            title = { 
                                Text(
                                    text = "准备导出",
                                    style = MaterialTheme.typography.titleLarge
                                ) 
                            },
                            text = {
                                Column {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            Text(
                                                text = "共 ${exportEventCount} 个日程",
                                                style = MaterialTheme.typography.headlineSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (exportDateRange != null) {
                                                Text(
                                                    text = "日期：$exportDateRange",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(top = 4.dp)
                                                )
                                            } else {
                                                Text(
                                                    text = "包含所有已保存的日程",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(top = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "文件将保存为：$fileName",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        fileSaverLauncher.launch(fileName)
                                        setExportDialogVisible(false)
                                    }
                                ) {
                                    Text("保存文件")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { setExportDialogVisible(false) }) {
                                    Text("取消")
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.surface
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

private const val REMINDER_CHANNEL_ID = "calendar_reminders"

/**
 * 计算日期所在的一年中的周数
 */
private fun getWeekNumber(date: java.time.LocalDate): Int {
    val weekFields = WeekFields.of(Locale.getDefault())
    return date.get(weekFields.weekOfWeekBasedYear())
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
                    val weekNumber = getWeekNumber(selectedDate)
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

private fun MainActivity.createNotificationChannel() {
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

/**
 * 应用启动时检查并同步订阅数据（超过24小时则同步）
 * 用户可以在订阅管理界面手动创建订阅
 */
private fun syncSubscriptionsOnStartup(
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