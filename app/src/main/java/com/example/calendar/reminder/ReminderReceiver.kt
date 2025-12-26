package com.example.calendar.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
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
        // 确保通知渠道存在（Android 8.0+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ensureNotificationChannels(context)
        }
        
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

        // 对于 Android 8.0 以下版本，仍然可以通过 builder 设置声音
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
            if (hasAlarm) {
                // 启用响铃时：设置声音
                // 使用系统自带的通知铃声
                val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                builder.setSound(defaultSoundUri)
                    .setOnlyAlertOnce(false)
            } else {
                // 不启用响铃时：无声音（只有消息提醒）
                builder.setSound(null)
            }
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

    /**
     * 确保通知渠道存在（如果不存在则创建）
     * 这是一个安全措施，防止在应用未运行时触发提醒时渠道不存在
     */
    private fun ensureNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // 检查并创建响铃渠道
            var channel = notificationManager.getNotificationChannel(REMINDER_CHANNEL_ID)
            if (channel == null) {
                channel = NotificationChannel(REMINDER_CHANNEL_ID, "日程提醒（响铃）", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "带声音的日程提醒"
                    val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    setSound(defaultSoundUri, AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build())
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                    setBypassDnd(false)
                }
                notificationManager.createNotificationChannel(channel)
            }
            
            // 检查并创建静音渠道
            var silentChannel = notificationManager.getNotificationChannel(REMINDER_SILENT_CHANNEL_ID)
            if (silentChannel == null) {
                silentChannel = NotificationChannel(REMINDER_SILENT_CHANNEL_ID, "日程提醒（静音）", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "静音日程提醒"
                    setSound(null, null)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                    setBypassDnd(false)
                }
                notificationManager.createNotificationChannel(silentChannel)
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


