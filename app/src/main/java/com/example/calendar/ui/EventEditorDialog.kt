package com.example.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.calendar.data.Event
import com.example.calendar.data.EventType
import com.example.calendar.util.formatDate
import com.example.calendar.util.formatTime
import com.example.calendar.util.toMillis
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditorDialog(
    visible: Boolean,
    date: LocalDate,
    editingEvent: Event? = null,
    onDismiss: () -> Unit,
    onSave: (Event, Int?, Int) -> Unit, // 添加 repeatCount 参数
    onDelete: (() -> Unit)? = null
) {
    if (!visible) return

    // 第一个模块：名称和类型
    val (title, setTitle) = remember(editingEvent) {
        mutableStateOf(editingEvent?.summary ?: "")
    }
    val (eventType, setEventType) = remember(editingEvent) {
        mutableStateOf(editingEvent?.eventType ?: EventType.NORMAL)
    }
    val (showTypeDropdown, setShowTypeDropdown) = remember { mutableStateOf(false) }

    // 第二个模块：开始和结束时间
    val defaultStart = editingEvent?.let {
        java.time.Instant.ofEpochMilli(it.dtStart)
            .atZone(java.time.ZoneId.of(it.timezone))
            .toLocalDateTime()
    } ?: date.atTime(15, 30)
    val defaultEnd = editingEvent?.let {
        java.time.Instant.ofEpochMilli(it.dtEnd)
            .atZone(java.time.ZoneId.of(it.timezone))
            .toLocalDateTime()
    } ?: date.atTime(16, 30)
    
    val (startTime, setStartTime) = remember(editingEvent) { mutableStateOf(defaultStart) }
    val (endTime, setEndTime) = remember(editingEvent) { mutableStateOf(defaultEnd) }
    
    // 时间选择器状态
    val (showStartTimePicker, setShowStartTimePicker) = remember { mutableStateOf(false) }
    val (showEndTimePicker, setShowEndTimePicker) = remember { mutableStateOf(false) }

    // 第三个模块：重复次数、提前提醒、响铃提醒
    // 重复类型：仅一次(0)、每天(1)、每周(7)、每月(30)
    val repeatOptions = listOf(0 to "仅一次", 1 to "每天", 7 to "每周", 30 to "每月")
    val (repeatCount, setRepeatCount) = remember(editingEvent) {
        mutableIntStateOf(
            when (editingEvent?.repeatType) {
                com.example.calendar.data.RepeatType.DAILY -> 1
                com.example.calendar.data.RepeatType.WEEKLY -> 7
                com.example.calendar.data.RepeatType.MONTHLY -> 30
                else -> 0
            }
        )
    }
    // 提醒时间选项：五分钟、十五分钟、三十分钟、一小时
    val reminderOptions = listOf(5 to "五分钟", 15 to "十五分钟", 30 to "三十分钟", 60 to "一小时")
    val (reminderMinutes, setReminderMinutes) = remember(editingEvent) {
        mutableIntStateOf(editingEvent?.reminderMinutes ?: 5) // 默认5分钟
    }
    val (hasReminder, setHasReminder) = remember(editingEvent) {
        // 新建日程时默认开启提醒（默认5分钟），编辑时根据原有设置
        mutableStateOf(editingEvent?.reminderMinutes != null || editingEvent == null)
    }
    val (hasAlarm, setHasAlarm) = remember(editingEvent) {
        mutableStateOf(editingEvent?.hasAlarm ?: false) // 响铃提醒
    }

    // 第四个模块：备注
    val (location, setLocation) = remember(editingEvent) {
        mutableStateOf(editingEvent?.location ?: "")
    }
    val (description, setDescription) = remember(editingEvent) {
        mutableStateOf(editingEvent?.description ?: "")
    }
    
    // 键盘控制器
    val keyboardController = LocalSoftwareKeyboardController.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (editingEvent == null) "新建" else "编辑",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp) // 限制最大高度，避免在小屏设备上触底
                    .verticalScroll(rememberScrollState())
            ) {
                // ========== 第一个模块：名称和类型 ==========
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    // 名称输入（限制18字）
                    TextField(
                        value = title,
                        onValueChange = { newValue ->
                            if (newValue.length <= 18) {
                                setTitle(newValue)
                            }
                        },
                        placeholder = { 
                            Text(
                                "请输入日程名称",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodyMedium
                            ) 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White
                        ),
                        textStyle = MaterialTheme.typography.titleLarge,
                        singleLine = true,
                        trailingIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${title.length}/18",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (title.length >= 18) Color.Red else Color.Gray
                                )
                                if (title.isNotEmpty()) {
                                    TextButton(
                                        onClick = { 
                                            setTitle("")
                                            keyboardController?.hide()
                                        }
                                    ) {
                                        Text("✕", color = Color.Gray)
                                    }
                                }
                            }
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 类型选择
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "类型",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        // 下拉菜单
                        Box {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { setShowTypeDropdown(true) },
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFF5F5F5)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = eventType.displayName,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "▼",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                            
                            DropdownMenu(
                                expanded = showTypeDropdown,
                                onDismissRequest = { setShowTypeDropdown(false) },
                                modifier = Modifier.width(180.dp)
                            ) {
                                EventType.entries.forEach { type ->
                                    val isSelected = eventType == type
                                    DropdownMenuItem(
                                        text = { 
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = type.displayName,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (isSelected) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurface
                                                    }
                                                )
                                                if (isSelected) {
                                                    Text(
                                                        text = "✓",
                                                        color = MaterialTheme.colorScheme.primary,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            setEventType(type)
                                            setShowTypeDropdown(false)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = androidx.compose.material3.MenuDefaults.itemColors(
                                            textColor = if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // ========== 第二个模块：开始和结束时间 ==========
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "持续时间",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 开始时间
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { setShowStartTimePicker(true) }
                            ) {
                                Text(
                                    text = "开始",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Text(
                                            text = startTime.formatTime(),
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = startTime.formatDate(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                            
                            // 结束时间
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { setShowEndTimePicker(true) }
                            ) {
                                Text(
                                    text = "结束",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Text(
                                            text = endTime.formatTime(),
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = endTime.formatDate(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ========== 第三个模块：重复、提醒、响铃 ==========
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // 重复次数
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Text(
                                text = "重复次数",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                repeatOptions.forEach { (count, displayText) ->
                                    val isSelected = repeatCount == count
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { setRepeatCount(count) },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                Color.White
                                            }
                                        )
                                    ) {
                                        Text(
                                            text = displayText,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 10.dp),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                Color.Gray
                                            },
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                        
                        // 提前提醒
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Text(
                                text = "提前提醒",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                reminderOptions.forEach { (minutes, displayText) ->
                                    val isSelected = hasReminder && reminderMinutes == minutes
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { 
                                                setReminderMinutes(minutes)
                                                setHasReminder(true)
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                Color.White
                                            }
                                        )
                                    ) {
                                        Text(
                                            text = displayText,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 10.dp),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                Color.Gray
                                            },
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                        
                        // 响铃提醒
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "响铃提醒",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Switch(
                                checked = hasAlarm,
                                onCheckedChange = setHasAlarm
                            )
                        }
                    }
                }

                // ========== 第四个模块：地点（可选） ==========
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "地点（可选）",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        TextField(
                            value = location,
                            onValueChange = setLocation,
                            placeholder = { 
                                Text(
                                    "请输入位置",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = Color.White,
                                focusedContainerColor = Color.White
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Place,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            singleLine = true,
                            maxLines = 1
                        )
                    }
                }

                // ========== 第五个模块：备注和按钮 ==========
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "备注（可选）",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                            Text(
                                text = "${description.length}/200",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (description.length >= 200) Color.Red else Color.Gray
                            )
                        }
                        TextField(
                            value = description,
                            onValueChange = { newValue ->
                                if (newValue.length <= 200) {
                                    setDescription(newValue)
                                }
                            },
                            placeholder = { 
                                Text(
                                    "请输入备注信息",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodySmall
                                ) 
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = Color.White,
                                focusedContainerColor = Color.White
                            ),
                            maxLines = 5
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = Color.Gray)
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (editingEvent != null && onDelete != null) {
                        TextButton(onClick = onDelete) {
                            Text("删除", color = Color.Red)
                        }
                    }
                    
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                // 使用事件指定的时区来转换时间戳，确保保存和显示时区一致
                                val eventTimezone = editingEvent?.timezone ?: java.time.ZoneId.systemDefault().id
                                
                                // 确保结束时间不早于开始时间
                                val finalEndTime = if (endTime.isBefore(startTime) || endTime.isEqual(startTime)) {
                                    // 如果结束时间早于或等于开始时间，自动设置为开始时间 + 1小时
                                    startTime.plusHours(1)
                                } else {
                                    endTime
                                }
                                
                                val startMillis = startTime.toMillis(eventTimezone)
                                val endMillis = finalEndTime.toMillis(eventTimezone)
                                val reminder = if (hasReminder) reminderMinutes else null
                                
                                val event = if (editingEvent == null) {
                                    Event(
                                        uid = java.util.UUID.randomUUID().toString(),
                                        summary = title,
                                        description = description.ifBlank { null },
                                        location = location.ifBlank { null },
                                        dtStart = startMillis,
                                        dtEnd = endMillis,
                                        timezone = eventTimezone,
                                        reminderMinutes = reminder,
                                        eventType = eventType,
                                        hasAlarm = hasAlarm
                                    )
                                } else {
                                    editingEvent.copy(
                                        summary = title,
                                        description = description.ifBlank { null },
                                        location = location.ifBlank { null },
                                        dtStart = startMillis,
                                        dtEnd = endMillis,
                                        reminderMinutes = reminder,
                                        eventType = eventType,
                                        hasAlarm = hasAlarm,
                                        lastModified = System.currentTimeMillis()
                                    )
                                }
                                onSave(event, reminder, repeatCount)
                            }
                            onDismiss()
                        },
                        enabled = title.isNotBlank()
                    ) {
                        Text(if (editingEvent == null) "完成" else "保存")
                    }
                }
            }
        },
        dismissButton = {},
        containerColor = Color.White
    )
    
    // 开始时间选择器
    if (showStartTimePicker) {
        val startPickerState = rememberTimePickerState(
            initialHour = startTime.hour,
            initialMinute = startTime.minute
        )
        
        AlertDialog(
            onDismissRequest = { setShowStartTimePicker(false) },
            title = { Text("选择开始时间") },
            text = {
                TimePicker(
                    state = startPickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialSelectedContentColor = MaterialTheme.colorScheme.primary,
                        clockDialColor = MaterialTheme.colorScheme.primaryContainer,
                        selectorColor = MaterialTheme.colorScheme.primary,
                        periodSelectorBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        setStartTime(
                            startTime.toLocalDate().atTime(
                                startPickerState.hour,
                                startPickerState.minute
                            )
                        )
                        setShowStartTimePicker(false)
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { setShowStartTimePicker(false) }) {
                    Text("取消")
                }
            }
        )
    }
    
    // 结束时间选择器
    if (showEndTimePicker) {
        val endPickerState = rememberTimePickerState(
            initialHour = endTime.hour,
            initialMinute = endTime.minute
        )
        
        AlertDialog(
            onDismissRequest = { setShowEndTimePicker(false) },
            title = { Text("选择结束时间") },
            text = {
                TimePicker(
                    state = endPickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialSelectedContentColor = MaterialTheme.colorScheme.primary,
                        clockDialColor = MaterialTheme.colorScheme.primaryContainer,
                        selectorColor = MaterialTheme.colorScheme.primary,
                        periodSelectorBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        setEndTime(
                            endTime.toLocalDate().atTime(
                                endPickerState.hour,
                                endPickerState.minute
                            )
                        )
                        setShowEndTimePicker(false)
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { setShowEndTimePicker(false) }) {
                    Text("取消")
                }
            }
        )
    }
}
