package com.example.calendar.reminder

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.calendar.R
import com.example.calendar.data.AppDatabase
import com.example.calendar.data.getEventByIdOnce
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
        val hasAlarm = intent.getBooleanExtra(EXTRA_HAS_ALARM, false)
        
        if (eventId <= 0L) return

        // 使用 runBlocking 确保在 BroadcastReceiver 中同步执行
        // BroadcastReceiver 的生命周期很短，必须同步完成工作
        kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
            val db = AppDatabase.getInstance(context)
            val event = db.getEventByIdOnce(eventId)
            event?.let {
                // 直接使用数据库中的 hasAlarm 字段，这是最准确的
                // Intent 中的 hasAlarm 可能不是最新的（如果用户修改了设置）
                // 只有数据库中 hasAlarm 为 true 时才响铃
                showNotification(context, it.id, it.summary, it.hasAlarm)
            }
        }
    }

    private fun showNotification(context: Context, eventId: Long, title: String, hasAlarm: Boolean) {
        // 根据 hasAlarm 选择不同的通知渠道
        // Android 8.0+ 上，声音和震动由渠道控制，不能通过 builder 设置
        val channelId = if (hasAlarm) {
            REMINDER_CHANNEL_ID // 有响铃的渠道
        } else {
            REMINDER_SILENT_CHANNEL_ID // 静音渠道
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("日程提醒")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // 对于 Android 8.0 以下版本，仍然可以通过 builder 设置声音和震动
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
            if (hasAlarm) {
                // 启用响铃时：设置声音、震动、灯光
                val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                builder.setSound(defaultSoundUri)
                    .setVibrate(longArrayOf(0, 250, 250, 250))
                    .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
                    .setOnlyAlertOnce(false)
            } else {
                // 不启用响铃时：只有灯光，无声音无震动
                builder.setDefaults(NotificationCompat.DEFAULT_LIGHTS)
                    .setSound(null)
                    .setVibrate(null)
            }
        } else {
            // Android 8.0+ 上，声音和震动由渠道控制，这里只设置灯光（如果需要）
            builder.setDefaults(NotificationCompat.DEFAULT_LIGHTS)
        }

        val notificationManager = NotificationManagerCompat.from(context)
        val canNotify = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (canNotify) {
            try {
                // 使用事件ID的hashCode作为通知ID，确保每个事件的通知都是唯一的
                // 虽然理论上可能冲突，但概率极低，且比使用title.hashCode()更可靠
                val notificationId = eventId.hashCode()
                notificationManager.notify(notificationId, builder.build())
            } catch (_: SecurityException) {
                // 安全兜底：权限被拒绝时不抛出崩溃
            }
        }
    }

    companion object {
        const val EXTRA_EVENT_ID = "extra_event_id"
        const val EXTRA_HAS_ALARM = "extra_has_alarm"
        
        // 通知渠道 ID
        const val REMINDER_CHANNEL_ID = "calendar_reminders"
        const val REMINDER_SILENT_CHANNEL_ID = "calendar_reminders_silent"
    }
}


