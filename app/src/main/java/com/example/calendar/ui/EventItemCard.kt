package com.example.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.calendar.data.Event
import com.example.calendar.util.toLocalTime
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter

/**
 * 六元组数据类，用于存储样式信息
 */
private data class Sextuple<A, B, C, D, E, F>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F
)

/**
 * 日程状态枚举
 */
enum class EventStatus {
    COMPLETED,      // 已完成
    IN_PROGRESS,    // 正在进行中
    UPCOMING        // 未完成（即将到来）
}

/**
 * 判断日程状态
 * 注意：时间戳都是UTC的，可以直接比较，不需要时区转换
 * @param event 事件对象
 * @param currentTime 当前时间戳（用于测试和定期更新）
 */
private fun getEventStatus(event: Event, currentTime: Long = System.currentTimeMillis()): EventStatus {
    // 直接比较时间戳（都是UTC，可以直接比较）
    // 注意：必须确保结束时间 >= 开始时间，否则数据异常
    if (event.dtEnd < event.dtStart) {
        // 如果结束时间早于开始时间，数据异常，返回待办状态
        return EventStatus.UPCOMING
    }
    
    // 判断逻辑：
    // 1. 如果结束时间 < 当前时间 -> 已完成（事件已结束）
    // 2. 如果开始时间 <= 当前时间 <= 结束时间 -> 进行中（事件正在进行）
    // 3. 如果当前时间 < 开始时间 -> 待办（事件还未开始）
    return when {
        // 结束时间已过当前时间 -> 已完成
        event.dtEnd < currentTime -> EventStatus.COMPLETED
        // 开始时间已过或等于当前时间，且结束时间还未到 -> 进行中
        event.dtStart <= currentTime && event.dtEnd > currentTime -> EventStatus.IN_PROGRESS
        // 开始时间还未到 -> 待办
        else -> EventStatus.UPCOMING
    }
}

/**
 * 事件项卡片组件，用于在日历视图中显示单个事件
 * 
 * @param event 要显示的事件
 * @param formatter 时间格式化器
 * @param onClick 点击事件回调
 * @param modifier Modifier
 * @param showDetails 是否显示详细信息（描述和地点），默认false
 */
@Composable
fun EventItemCard(
    event: Event,
    formatter: DateTimeFormatter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDetails: Boolean = false
) {
    // 添加定期更新机制，每30秒更新一次状态，实现动态状态更新
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L) // 每30秒更新一次
            currentTime = System.currentTimeMillis()
        }
    }
    
    val status = getEventStatus(event, currentTime)
    
    // 根据状态设置不同的颜色和样式
    val (accentColor, containerColor, textColor, borderColor, badgeText, badgeColor) = when (status) {
        EventStatus.COMPLETED -> {
            // 已完成：灰色竖线，浅灰背景，灰色文字，灰色边框
            Sextuple(
                Color(0xFF9E9E9E),
                Color(0xFFF5F5F5),
                Color(0xFF757575),
                Color(0xFFE0E0E0),
                "已完成",
                Color(0xFF9E9E9E)
            )
        }
        EventStatus.IN_PROGRESS -> {
            // 正在进行中：橙色竖线，橙色浅背景，正常文字，橙色边框
            Sextuple(
                Color(0xFFFF6F00),
                Color(0xFFFFF3E0),
                Color(0xFFE65100),
                Color(0xFFFFB74D),
                "进行中",
                Color(0xFFFF6F00)
            )
        }
        EventStatus.UPCOMING -> {
            // 未完成：蓝色竖线，蓝色浅背景，正常文字，蓝色边框
            Sextuple(
                Color(0xFF1976D2),
                Color(0xFFE3F2FD),
                Color(0xFF1565C0),
                Color(0xFF90CAF9),
                "待办",
                Color(0xFF1976D2)
            )
        }
    }
    
    // 处理标题和备注长度限制
    val displayTitle = remember(event.summary) {
        if (event.summary.length > 18) event.summary.take(18) else event.summary
    }
    val displayDescription = remember(event.description) {
        event.description?.takeIf { it.isNotBlank() }?.let { desc ->
            if (desc.length > 200) desc.take(200) else desc
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = 1.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (status == EventStatus.IN_PROGRESS) 4.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 左侧竖线（根据状态显示不同颜色和宽度）
            Box(
                modifier = Modifier
                    .width(if (status == EventStatus.IN_PROGRESS) 5.dp else 4.dp)
                    .fillMaxWidth(0f)
                    .background(accentColor)
            )
            
            // 内容区域
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                // 顶部行：状态标签 + 时间 + 地点（都在同一行）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 状态标签
                    Box(
                        modifier = Modifier
                            .background(
                                badgeColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // 时间
                    val startTime = event.dtStart.toLocalTime(event.timezone)
                    val endTime = event.dtEnd.toLocalTime(event.timezone)
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = textColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${formatter.format(startTime)} - ${formatter.format(endTime)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = if (status == EventStatus.COMPLETED) 0.6f else 0.8f),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    // 地点（如果存在）
                    if (showDetails) {
                        event.location?.takeIf { it.isNotBlank() }?.let { location ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Place,
                                    contentDescription = null,
                                    tint = textColor.copy(alpha = 0.65f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = location,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textColor.copy(alpha = if (status == EventStatus.COMPLETED) 0.6f else 0.7f),
                                    modifier = Modifier.padding(start = 4.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    
                    // 响铃图标（如果启用了响铃提醒）
                    if (event.hasAlarm) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "响铃提醒",
                            tint = textColor.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.padding(vertical = 6.dp))

                // 标题（只能一行）
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor,
                    fontWeight = if (status == EventStatus.COMPLETED) FontWeight.Normal else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )

                // 备注内容（可以多行）
                if (showDetails) {
                    displayDescription?.let { desc ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Notes,
                                contentDescription = null,
                                tint = textColor.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 2.dp)
                            )
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor.copy(alpha = if (status == EventStatus.COMPLETED) 0.6f else 0.7f),
                                modifier = Modifier.padding(start = 6.dp),
                                maxLines = Int.MAX_VALUE,
                                overflow = TextOverflow.Clip
                            )
                        }
                    }
                }
            }
        }
    }
}

