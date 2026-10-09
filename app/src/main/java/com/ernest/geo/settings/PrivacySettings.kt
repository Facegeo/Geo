package com.ernest.geo.settings

import android.content.Context
import androidx.core.content.edit

enum class NotificationPrivacy {
    ANNOUNCE_ALL,      // Everything: Sender, App, Content
    ANNOUNCE_SENDER,   // Sender and App (No Content)
    ANNOUNCE_APP_ONLY  // App Name Only
}

enum class GeoPersonality {
    CLASSIC,
    CALM,
    ENERGETIC,
    BRITISH
}

object PrivacySettings {

    private const val PREFS = "geo_settings"
    private const val KEY_PRIVACY_LEVEL = "privacy_level"
    private const val KEY_PERSONALITY = "geo_personality"
    private const val KEY_APP_ENABLED = "app_enabled"
    private const val KEY_BUBBLE_VISIBLE = "bubble_visible"

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getPrivacyLevel(context: Context): NotificationPrivacy {
        val levelName = getPrefs(context).getString(KEY_PRIVACY_LEVEL, NotificationPrivacy.ANNOUNCE_ALL.name)
        return try {
            NotificationPrivacy.valueOf(levelName ?: NotificationPrivacy.ANNOUNCE_ALL.name)
        } catch (_: Exception) {
            NotificationPrivacy.ANNOUNCE_ALL
        }
    }

    fun setPrivacyLevel(context: Context, level: NotificationPrivacy) {
        getPrefs(context).edit {
            putString(KEY_PRIVACY_LEVEL, level.name)
        }
    }

    fun setPrivacyMode(context: Context, enabled: Boolean) {
        val level = if (enabled) NotificationPrivacy.ANNOUNCE_APP_ONLY else NotificationPrivacy.ANNOUNCE_ALL
        setPrivacyLevel(context, level)
    }

    fun getPersonality(context: Context): GeoPersonality {
        val personalityName = getPrefs(context).getString(KEY_PERSONALITY, GeoPersonality.CLASSIC.name)
        return try {
            GeoPersonality.valueOf(personalityName ?: GeoPersonality.CLASSIC.name)
        } catch (_: Exception) {
            GeoPersonality.CLASSIC
        }
    }

    fun setPersonality(context: Context, personality: GeoPersonality) {
        getPrefs(context).edit {
            putString(KEY_PERSONALITY, personality.name)
        }
    }

    fun isAppEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_APP_ENABLED, true)
    }

    fun setAppEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit {
            putBoolean(KEY_APP_ENABLED, enabled)
        }
    }

    fun isBubbleVisible(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BUBBLE_VISIBLE, true)
    }

    fun setBubbleVisible(context: Context, visible: Boolean) {
        getPrefs(context).edit {
            putBoolean(KEY_BUBBLE_VISIBLE, visible)
        }
    }
}
