package com.example.calendar.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.calendar.data.Event

class ReminderScheduler(
    private val context: Context
) {

    private val alarmManager: AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun scheduleReminder(event: Event, reminderTimeMillis: Long) {
        // 验证提醒时间是否有效（不能是过去的时间）
        val currentTime = System.currentTimeMillis()
        if (reminderTimeMillis <= currentTime) {
            // 提醒时间已过期，不设置提醒
            return
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_EVENT_ID, event.id)
            putExtra(ReminderReceiver.EXTRA_HAS_ALARM, event.hasAlarm)
        }
        
        // 使用事件ID的hashCode作为requestCode，避免Long转Int导致的精度丢失
        // 虽然理论上仍可能冲突，但概率极低，且比直接转换更安全
        val requestCode = event.id.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canSchedule = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() == true
        } else {
            true
        }

        if (canSchedule && alarmManager != null) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTimeMillis,
                    pendingIntent
                )
            } catch (e: SecurityException) {
                // 精确闹钟权限被拒绝时的安全处理
                // 可以尝试使用非精确的闹钟作为降级方案
                try {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, reminderTimeMillis, pendingIntent)
                } catch (_: Exception) {
                    // 如果仍然失败，静默处理
                }
            }
        }
    }

    fun cancelReminder(eventId: Long) {
        val intent = Intent(context, ReminderReceiver::class.java)
        // 使用与scheduleReminder相同的requestCode生成方式
        val requestCode = eventId.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager?.cancel(pendingIntent)
    }
}


