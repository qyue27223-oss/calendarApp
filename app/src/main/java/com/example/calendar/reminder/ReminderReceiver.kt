package com.example.calendar.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.calendar.R
import com.example.calendar.data.AppDatabase
import com.example.calendar.data.getEventByIdOnce
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
        val hasAlarm = intent.getBooleanExtra(EXTRA_HAS_ALARM, false)
        
        if (eventId <= 0L) return

        // 简单起见，这里直接用 IO 协程查询数据库并发通知
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(context)
            val event = db.getEventByIdOnce(eventId)
            event?.let {
                // 使用事件中的 hasAlarm 字段（如果 Intent 中没有传递）
                val shouldRing = hasAlarm || event.hasAlarm
                showNotification(context, it.summary, shouldRing)
            }
        }
    }

    private fun showNotification(context: Context, title: String, hasAlarm: Boolean) {
        val channelId = "calendar_reminders"

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("日程提醒")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(
                if (hasAlarm) {
                    NotificationCompat.DEFAULT_ALL // 包含声音、震动、灯光
                } else {
                    NotificationCompat.DEFAULT_LIGHTS // 只有灯光
                }
            )

        // 如果启用响铃，设置默认通知铃声
        if (hasAlarm) {
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            builder.setSound(defaultSoundUri)
        }

        with(NotificationManagerCompat.from(context)) {
            notify(title.hashCode(), builder.build())
        }
    }

    companion object {
        const val EXTRA_EVENT_ID = "extra_event_id"
        const val EXTRA_HAS_ALARM = "extra_has_alarm"
    }
}


