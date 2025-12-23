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
                                    val events = IcsImporter.parse(content)
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
                                                    "导入 ICS",
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
                                                    "导出为 ICS",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            },
                                            onClick = {
                                                showMenu = false
                                                setIcsText(vm.exportSelectedDateEventsAsIcs())
                                                setIcsDialogVisible(true)
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
                    if (importConfirmDialogVisible && pendingIcsContent != null) {
                        val eventCount = try {
                            IcsImporter.parse(pendingIcsContent).size
                        } catch (_: Exception) {
                            0
                        }
                        AlertDialog(
                            onDismissRequest = { 
                                setImportConfirmDialogVisible(false)
                                setPendingIcsContent(null)
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
                                    text = "确认导入",
                                    style = MaterialTheme.typography.titleLarge
                                ) 
                            },
                            text = { 
                                Column {
                                    Text(
                                        text = if (eventCount > 0) {
                                            "检测到 ICS 文件中包含 $eventCount 个事件。"
                                        } else {
                                            "无法解析 ICS 文件，请确认文件格式是否正确。"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    if (eventCount > 0) {
                                        Text(
                                            text = "是否要导入这些事件？如果存在相同 UID 的事件，将被覆盖。",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        vm.importEventsFromIcs(pendingIcsContent, onConflict = true)
                                        setImportConfirmDialogVisible(false)
                                        setPendingIcsContent(null)
                                    },
                                    enabled = eventCount > 0
                                ) {
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
                                    // 总体统计
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${result.total}",
                                                style = MaterialTheme.typography.headlineSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "总计事件",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${result.imported}",
                                                style = MaterialTheme.typography.headlineSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "成功导入",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (result.skipped > 0) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${result.skipped}",
                                                    style = MaterialTheme.typography.headlineSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "跳过",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    
                                    // 错误信息
                                    if (result.errors.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "错误信息：",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        result.errors.take(5).forEach { error ->
                                            Text(
                                                text = "• $error",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                                            )
                                        }
                                        if (result.errors.size > 5) {
                                            Text(
                                                text = "还有 ${result.errors.size - 5} 个错误...",
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

                    // 导出对话框
                    if (icsDialogVisible) {
                        val eventCount = icsText.split("BEGIN:VEVENT").size - 1
                        AlertDialog(
                            onDismissRequest = { setIcsDialogVisible(false) },
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
                                    text = "导出为 ICS",
                                    style = MaterialTheme.typography.titleLarge
                                ) 
                            },
                            text = {
                                Column {
                                    // 导出信息提示
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "共 $eventCount 个事件",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "ICS 文件内容（可选中复制）",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }
                                    
                                    // ICS 文本内容
                                    SelectionContainer {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .verticalScroll(rememberScrollState())
                                            ) {
                                                Text(
                                                    text = icsText,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier.padding(12.dp),
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                Button(onClick = {
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