package com.ernest.geo.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.ernest.geo.data.Reminder
import com.ernest.geo.data.ReminderRepository
import com.ernest.geo.data.ReminderStatus

object ReminderManager {

    fun setReminder(context: Context, task: String, timeInMillis: Long, interval: Long = 0) {
        val reminder = Reminder(task = task, timeInMillis = timeInMillis, interval = interval)
        ReminderRepository.addReminder(context, reminder)
        scheduleAlarm(context, reminder)
    }

    fun scheduleAlarm(context: Context, reminder: Reminder) {
        Log.d("GEO_REMINDER", "--- AlarmManager Verification ---")
        Log.d("GEO_REMINDER", "Task: ${reminder.task}")
        Log.d("GEO_REMINDER", "Target Epoch (ms): ${reminder.timeInMillis}")
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMINDER_TRIGGER
            putExtra("reminder_id", reminder.id)
            putExtra("task", reminder.task)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Log.w("GEO_REMINDER", "Cannot schedule exact alarms. Falling back to inexact.")
                    scheduleInexact(alarmManager, reminder.timeInMillis, reminder.interval, pendingIntent)
                    return
                }
            }

            if (reminder.interval > 0) {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    reminder.timeInMillis,
                    reminder.interval,
                    pendingIntent
                )
                Log.d("GEO_REMINDER", "Repeating reminder set for ${reminder.task} at ${reminder.timeInMillis}")
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminder.timeInMillis,
                    pendingIntent
                )
                Log.d("GEO_REMINDER", "Exact reminder set for ${reminder.task} at ${reminder.timeInMillis}")
            }
        } catch (e: Exception) {
            Log.e("GEO_REMINDER", "Failed to set alarm", e)
            scheduleInexact(alarmManager, reminder.timeInMillis, reminder.interval, pendingIntent)
        }
    }

    private fun scheduleInexact(alarmManager: AlarmManager, time: Long, interval: Long, intent: PendingIntent) {
        if (interval > 0) {
            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, time, interval, intent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, time, intent)
        }
    }

    fun restoreAlarms(context: Context) {
        ReminderRepository.loadReminders(context)
        val now = System.currentTimeMillis()
        ReminderRepository.reminders.forEach {
            if (it.status == ReminderStatus.PENDING && (it.timeInMillis > now || it.interval > 0)) {
                scheduleAlarm(context, it)
            }
        }
    }

    fun needsExactAlarmPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            return !alarmManager.canScheduleExactAlarms()
        }
        return false
    }

    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
