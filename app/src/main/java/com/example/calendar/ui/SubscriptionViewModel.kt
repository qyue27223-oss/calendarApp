package com.example.calendar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendar.data.Subscription
import com.example.calendar.data.SubscriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    fun insertSubscription(subscription: Subscription) {
        viewModelScope.launch {
            val subscriptionId = repository.insertSubscription(subscription)
            // 如果订阅是启用的，立即同步数据
            if (subscription.enabled) {
                val result = repository.syncSubscription(subscription.copy(id = subscriptionId))
                if (!result.success) {
                    onSyncError?.invoke("同步失败，请稍后重试")
                }
            }
        }
    }

    fun updateSubscription(subscription: Subscription) {
        viewModelScope.launch {
            repository.updateSubscription(subscription)
            // 如果订阅是启用的，立即同步数据
            if (subscription.enabled) {
                val result = repository.syncSubscription(subscription)
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

