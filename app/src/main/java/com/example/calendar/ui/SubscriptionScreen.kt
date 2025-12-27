package com.example.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.calendar.data.Subscription
import com.example.calendar.data.SubscriptionType
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    viewModel: SubscriptionViewModel,
    onBack: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    currentSelectedDate: LocalDate = LocalDate.now() // 当前选中的日期，用于检查该月份的数据
) {
    val subscriptions by viewModel.subscriptions.collectAsState()
    
    // 定义可订阅的服务列表（只能选择天气和黄历）
    // 注意：确保天气服务在黄历服务之前，以便优先显示
    val availableServices = remember {
        listOf(
            SubscriptionService(
                type = SubscriptionType.WEATHER,
                name = "日历天气卡",
                description = "提供实时天气信息、预报和天气相关服务",
                iconColor = Color(0xFF64B5F6), // 浅蓝色
                iconEmoji = "☁️"
            ),
            SubscriptionService(
                type = SubscriptionType.HUANGLI,
                name = "黄历",
                description = "每日宜忌,趋吉避凶",
                iconColor = Color(0xFFFFB74D), // 黄色
                iconEmoji = "📖"
            )
        )
    }

    // 头部已在主Scaffold中处理，这里不再重复显示
    // 合并传入的 contentPadding（包含顶部导航栏高度）和内容边距
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 16.dp,
            start = 16.dp,
            end = 16.dp,
            bottom = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 显示可订阅的服务列表（始终显示所有可用服务）
        items(
            items = availableServices,
            key = { it.type } // 使用类型作为key确保唯一性
        ) { service ->
            val existingSubscription = subscriptions.find { it.type == service.type }
            SubscriptionServiceCard(
                service = service,
                isSubscribed = existingSubscription != null,
                isEnabled = existingSubscription?.enabled ?: false,
                onSubscribe = {
                    if (existingSubscription == null) {
                        // 创建新订阅
                        // 对于黄历订阅，传递当前选中日期，用于检查该日期的数据是否存在
                        viewModel.insertSubscription(
                            Subscription(
                                type = service.type,
                                name = service.name,
                                // 注意：url 字段当前未被使用，同步逻辑直接调用 API 服务
                                url = "http://example.com/${service.type.name.lowercase()}",
                                enabled = true
                            ),
                            targetDate = if (service.type == SubscriptionType.HUANGLI) currentSelectedDate else null
                        )
                    } else {
                        // 启用订阅
                        // 对于黄历订阅，传递当前选中日期，用于检查该日期的数据是否存在
                        viewModel.updateSubscription(
                            existingSubscription.copy(enabled = true),
                            targetDate = if (service.type == SubscriptionType.HUANGLI) currentSelectedDate else null
                        )
                    }
                },
                onUnsubscribe = {
                    existingSubscription?.let {
                        // 禁用订阅（不删除，只是禁用）
                        viewModel.updateSubscription(it.copy(enabled = false))
                    }
                }
            )
        }
    }
}

data class SubscriptionService(
    val type: SubscriptionType,
    val name: String,
    val description: String,
    val iconColor: Color,
    val iconEmoji: String
)

@Composable
private fun SubscriptionServiceCard(
    service: SubscriptionService,
    isSubscribed: Boolean,
    isEnabled: Boolean,
    onSubscribe: () -> Unit,
    onUnsubscribe: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标背景
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        service.iconColor,
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = service.iconEmoji,
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            
            Spacer(modifier = Modifier.size(16.dp))
            
            // 内容区域
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = service.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = service.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.size(16.dp))
            
            // 订阅/退订按钮
            Button(
                onClick = if (isSubscribed && isEnabled) onUnsubscribe else onSubscribe,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSubscribed && isEnabled) {
                        Color(0xFF757575) // 灰色 - 已订阅
                    } else {
                        MaterialTheme.colorScheme.primary // 蓝色 - 未订阅
                    }
                ),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = if (isSubscribed && isEnabled) "退订" else "订阅",
                    color = Color.White
                )
            }
        }
    }
}
