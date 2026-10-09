package com.ernest.geo.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ernest.geo.MainActivity
import com.ernest.geo.R
import com.ernest.geo.data.ReminderRepository
import com.ernest.geo.data.ReminderStatus
import com.ernest.geo.voice.GeoVoice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    
    companion object {
        const val ACTION_REMINDER_TRIGGER = "com.ernest.geo.ACTION_REMINDER_TRIGGER"
        const val ACTION_DONE = "com.ernest.geo.ACTION_DONE"
        const val ACTION_SNOOZE = "com.ernest.geo.ACTION_SNOOZE"
        const val ACTION_DISMISS = "com.ernest.geo.ACTION_DISMISS"
        
        private const val CHANNEL_ID = "geo_reminders_channel"
        private const val NOTIFICATION_ID_BASE = 2000
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("GEO_REMINDER", "ReminderReceiver.onReceive: $action")
        
        // Handle Boot Completed
        if (action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("GEO_REMINDER", "Phone rebooted. Restoring alarms.")
            ReminderManager.restoreAlarms(context)
            return
        }

        val id = intent.getStringExtra("reminder_id") ?: return

        when (action) {
            ACTION_REMINDER_TRIGGER -> {
                val task = intent.getStringExtra("task") ?: "something"
                Log.d("GEO_REMINDER", "Alarm triggered for reminder: $id ($task)")
                
                // 1. Show notification
                showNotification(context, id, task)
                
                // 2. State change broadcast for UI
                context.sendBroadcast(Intent("com.ernest.geo.ACTION_STATE_CHANGE").apply {
                    putExtra("state", "SPEAKING")
                    `package` = context.packageName
                })

                // 3. Speak directly using goAsync to keep process alive
                val pendingResult = goAsync()
                
                CoroutineScope(Dispatchers.Main).launch {
                    val aiManager = com.ernest.geo.ai.AIManager()
                    val speech = aiManager.generateReminderAnnouncement(task)
                    
                    GeoVoice.initialize(context)
                    GeoVoice.speak(context, speech) {
                        Log.d("GEO_REMINDER", "Reminder announcement finished")
                        context.sendBroadcast(Intent("com.ernest.geo.ACTION_STATE_CHANGE").apply {
                            putExtra("state", "IDLE")
                            `package` = context.packageName
                        })
                        pendingResult.finish()
                    }
                }
            }
            ACTION_DONE -> {
                Log.d("GEO_REMINDER", "Action: DONE for $id")
                ReminderRepository.loadReminders(context)
                ReminderRepository.updateReminderStatus(context, id, ReminderStatus.DONE)
                cancelNotification(context, id)
            }
            ACTION_SNOOZE -> {
                Log.d("GEO_REMINDER", "Action: SNOOZE for $id")
                ReminderRepository.loadReminders(context)
                val newTime = System.currentTimeMillis() + (10 * 60 * 1000)
                ReminderRepository.snoozeReminder(context, id, newTime)
                val reminder = ReminderRepository.getReminderById(id)
                if (reminder != null) {
                    ReminderManager.scheduleAlarm(context, reminder)
                }
                cancelNotification(context, id)
            }
            ACTION_DISMISS -> {
                Log.d("GEO_REMINDER", "Action: DISMISS for $id")
                ReminderRepository.loadReminders(context)
                ReminderRepository.updateReminderStatus(context, id, ReminderStatus.DISMISSED)
                cancelNotification(context, id)
            }
        }
    }

    private fun showNotification(context: Context, id: String, task: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Geo Reminders", NotificationManager.IMPORTANCE_HIGH)
            nm.createNotificationChannel(channel)
        }

        val doneIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_DONE
            putExtra("reminder_id", id)
        }
        val donePending = PendingIntent.getBroadcast(context, id.hashCode() + 1, doneIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra("reminder_id", id)
        }
        val snoozePending = PendingIntent.getBroadcast(context, id.hashCode() + 2, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val dismissIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_DISMISS
            putExtra("reminder_id", id)
        }
        val dismissPending = PendingIntent.getBroadcast(context, id.hashCode() + 3, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val mainIntent = Intent(context, MainActivity::class.java)
        val mainPending = PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.geo_orb)
            .setContentTitle("Reminder")
            .setContentText(task)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(mainPending)
            .addAction(0, "✅ Done", donePending)
            .addAction(0, "⏰ Snooze", snoozePending)
            .addAction(0, "❌ Dismiss", dismissPending)
            .build()

        nm.notify(id.hashCode() + NOTIFICATION_ID_BASE, notification)
    }

    private fun cancelNotification(context: Context, id: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(id.hashCode() + NOTIFICATION_ID_BASE)
        GeoVoice.stop()
    }
}
