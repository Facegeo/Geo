package com.ernest.geo.data

import android.content.Context

object NotificationStorage {

    private const val PREF_NAME = "geo_notifications"
    private const val KEY_NOTIFICATIONS = "notifications"

    fun saveNotification(
        context: Context,
        notification: String
    ) {

        val prefs =
            context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
            )

        val current =
            prefs.getStringSet(
                KEY_NOTIFICATIONS,
                mutableSetOf()
            ) ?: mutableSetOf()

        val updated =
            current.toMutableSet()

        updated.add(notification)

        prefs.edit()
            .putStringSet(
                KEY_NOTIFICATIONS,
                updated
            )
            .apply()
    }

    fun getNotifications(
        context: Context
    ): List<String> {

        val prefs =
            context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
            )

        return prefs.getStringSet(
            KEY_NOTIFICATIONS,
            emptySet()
        )?.toList() ?: emptyList()
    }
}