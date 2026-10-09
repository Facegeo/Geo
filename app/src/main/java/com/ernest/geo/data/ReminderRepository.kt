package com.ernest.geo.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class ReminderStatus {
    PENDING,
    DONE,
    DISMISSED
}

data class Reminder(
    val id: String = UUID.randomUUID().toString(),
    val task: String,
    val timeInMillis: Long,
    val interval: Long = 0,
    val status: ReminderStatus = ReminderStatus.PENDING
)

object ReminderRepository {
    private const val PREFS = "geo_reminders"
    private const val KEY_LIST = "reminders_list"

    val reminders = mutableStateListOf<Reminder>()

    fun loadReminders(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_LIST, null) ?: return
        
        try {
            reminders.clear()
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                reminders.add(
                    Reminder(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        task = obj.getString("task"),
                        timeInMillis = obj.getLong("timeInMillis"),
                        interval = obj.optLong("interval", 0L),
                        status = ReminderStatus.valueOf(obj.optString("status", ReminderStatus.PENDING.name))
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveReminders(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val array = JSONArray()
        reminders.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("task", it.task)
            obj.put("timeInMillis", it.timeInMillis)
            obj.put("interval", it.interval)
            obj.put("status", it.status.name)
            array.put(obj)
        }
        prefs.edit().putString(KEY_LIST, array.toString()).apply()
    }

    fun addReminder(context: Context, reminder: Reminder) {
        // Avoid duplicates if needed, but for now just add
        reminders.add(reminder)
        saveReminders(context)
    }

    fun updateReminderStatus(context: Context, id: String, status: ReminderStatus) {
        val index = reminders.indexOfFirst { it.id == id }
        if (index != -1) {
            val current = reminders[index]
            reminders[index] = current.copy(status = status)
            saveReminders(context)
        }
    }

    fun snoozeReminder(context: Context, id: String, newTime: Long) {
        val index = reminders.indexOfFirst { it.id == id }
        if (index != -1) {
            val current = reminders[index]
            reminders[index] = current.copy(timeInMillis = newTime, status = ReminderStatus.PENDING)
            saveReminders(context)
        }
    }

    fun getReminderById(id: String): Reminder? {
        return reminders.find { it.id == id }
    }

    fun removeOldReminders(context: Context) {
        val now = System.currentTimeMillis()
        reminders.removeAll { it.timeInMillis < now && it.interval == 0L && it.status != ReminderStatus.PENDING }
        saveReminders(context)
    }

    fun deleteReminder(context: Context, id: String) {
        reminders.removeAll { it.id == id }
        saveReminders(context)
    }

    fun clearCompletedReminders(context: Context) {
        reminders.removeAll { it.status == ReminderStatus.DONE || it.status == ReminderStatus.DISMISSED }
        saveReminders(context)
    }
}
