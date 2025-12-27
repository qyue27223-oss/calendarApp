package com.example.calendar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendar.data.Subscription
import com.example.calendar.data.SubscriptionRepository
import com.example.calendar.data.SubscriptionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class SubscriptionViewModel(
    private val repository: SubscriptionRepository
) : ViewModel() {

    val subscriptions: StateFlow<List<Subscription>> =
        repository.getAllSubscriptions()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    // 同步错误回调
    var onSyncError: ((String) -> Unit)? = null

    /**
     * 插入订阅
     * @param subscription 订阅信息
     * @param targetDate 目标日期（用于黄历订阅，检查该日期的数据是否存在），如果为null则使用当前日期
     */
    fun insertSubscription(subscription: Subscription, targetDate: LocalDate? = null) {
        viewModelScope.launch {
            val subscriptionId = repository.insertSubscription(subscription)
            // 如果订阅是启用的，立即同步数据
            if (subscription.enabled) {
                val result = if (subscription.type == SubscriptionType.HUANGLI) {
                    // 黄历订阅：先检查数据是否存在，不存在才同步
                    repository.syncHuangliSubscriptionIfNeeded(
                        subscription.copy(id = subscriptionId),
                        targetDate
                    )
                } else {
                    // 其他订阅类型：直接同步
                    repository.syncSubscription(subscription.copy(id = subscriptionId))
                }
                if (!result.success) {
                    onSyncError?.invoke("同步失败，请稍后重试")
                }
            }
        }
    }

    /**
     * 更新订阅
     * @param subscription 订阅信息
     * @param targetDate 目标日期（用于黄历订阅，检查该日期的数据是否存在），如果为null则使用当前日期
     */
    fun updateSubscription(subscription: Subscription, targetDate: LocalDate? = null) {
        viewModelScope.launch {
            repository.updateSubscription(subscription)
            // 如果订阅是启用的，立即同步数据
            if (subscription.enabled) {
                val result = if (subscription.type == SubscriptionType.HUANGLI) {
                    // 黄历订阅：先检查数据是否存在，不存在才同步
                    repository.syncHuangliSubscriptionIfNeeded(subscription, targetDate)
                } else {
                    // 其他订阅类型：直接同步
                    repository.syncSubscription(subscription)
                }
                if (!result.success) {
                    onSyncError?.invoke("同步失败，请稍后重试")
                }
            }
        }
    }

    fun deleteSubscription(subscription: Subscription) {
        viewModelScope.launch {
            repository.deleteSubscription(subscription)
        }
    }
}

