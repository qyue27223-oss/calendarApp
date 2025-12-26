package com.example.calendar.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.calendar.data.Event
import com.example.calendar.data.ImportResult
import com.example.calendar.util.formatDate
import com.example.calendar.util.formatTime
import java.time.LocalDate
import java.time.ZoneId

/**
 * 导入确认对话框
 */
@Composable
fun ImportConfirmDialog(
    visible: Boolean,
    pendingEvents: List<Event>,
    conflictCount: Int, // 冲突数量
    conflictStrategy: Boolean, // true=覆盖, false=跳过
    onConflictStrategyChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (visible && pendingEvents.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
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
                        text = "检测到 ${pendingEvents.size} 个日程，预览如下：",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 12.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // 显示前5个日程预览
                    pendingEvents.take(5).forEachIndexed { index, event ->
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

                    if (pendingEvents.size > 5) {
                        Text(
                            text = "还有 ${pendingEvents.size - 5} 个日程...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // 显示冲突信息
                    if (conflictCount > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "⚠ 检测到 $conflictCount 个日程与已有日程冲突",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "✓ 未检测到冲突，所有日程将作为新日程导入",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // 冲突处理选项（仅在存在冲突时显示）
                    if (conflictCount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "冲突处理方式：",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onConflictStrategyChange(true) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = conflictStrategy,
                                onClick = { onConflictStrategyChange(true) }
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
                                .clickable { onConflictStrategyChange(false) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = !conflictStrategy,
                                onClick = { onConflictStrategyChange(false) }
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
                }
            },
            confirmButton = {
                Button(onClick = onConfirm) {
                    Text("确认导入")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

/**
 * 导入结果对话框
 */
@Composable
fun ImportResultDialog(
    result: ImportResult,
    onDismiss: () -> Unit
) {
    val isSuccess = result.errors.isEmpty() && result.imported > 0
    AlertDialog(
        onDismissRequest = onDismiss,
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
            Button(onClick = onDismiss) {
                Text("确定")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

/**
 * 日期范围选择对话框
 */
@Composable
fun DateRangePickerDialog(
    visible: Boolean,
    startDate: LocalDate,
    endDate: LocalDate,
    allEvents: List<Event>,
    onStartDateChange: (LocalDate) -> Unit,
    onEndDateChange: (LocalDate) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (visible) {
        var tempStartDate by remember { mutableStateOf(startDate) }
        var tempEndDate by remember { mutableStateOf(endDate) }
        
        // 当对话框打开或外部日期变化时，重置临时日期
        LaunchedEffect(visible, startDate, endDate) {
            if (visible) {
                tempStartDate = startDate
                tempEndDate = endDate
            }
        }
        
        // 基于临时日期范围实时计算事件数量
        val eventsCount = remember(tempStartDate, tempEndDate, allEvents) {
            val systemZoneId = ZoneId.systemDefault()
            val startTime = tempStartDate.atStartOfDay(systemZoneId).toInstant().toEpochMilli()
            val endTime = tempEndDate.plusDays(1).atStartOfDay(systemZoneId).toInstant().toEpochMilli()
            allEvents.count { event ->
                event.dtStart >= startTime && event.dtStart < endTime
            }
        }

        AlertDialog(
            onDismissRequest = onDismiss,
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
                                    textAlign = TextAlign.Center
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
                                    textAlign = TextAlign.Center
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
                        text = "此日期范围内共有 $eventsCount 个日程",
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
                        onStartDateChange(tempStartDate)
                        onEndDateChange(tempEndDate)
                        onConfirm()
                    },
                    enabled = tempStartDate <= tempEndDate && eventsCount > 0
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

/**
 * 导出选项对话框
 */
@Composable
fun ExportOptionsDialog(
    visible: Boolean,
    selectedDate: LocalDate,
    eventsForSelectedDate: List<Event>,
    allEvents: List<Event>,
    onOptionSelected: (Int) -> Unit, // 0: 当前日期, 1: 日期范围, 2: 全部
    onDismiss: () -> Unit
) {
    if (visible) {
        var selectedExportOption by remember { mutableStateOf(0) } // 0: 当前日期, 1: 日期范围, 2: 全部

        AlertDialog(
            onDismissRequest = onDismiss,
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
                                text = "${selectedDate.formatDate()} 的所有日程",
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
                    onClick = { onOptionSelected(selectedExportOption) },
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
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

/**
 * 导出确认对话框
 */
@Composable
fun ExportConfirmDialog(
    visible: Boolean,
    eventCount: Int,
    dateRange: String?,
    fileName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (visible) {
        AlertDialog(
            onDismissRequest = onDismiss,
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
                                text = "共 $eventCount 个日程",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                            if (dateRange != null) {
                                Text(
                                    text = "日期：$dateRange",
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
                Button(onClick = onConfirm) {
                    Text("保存文件")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

