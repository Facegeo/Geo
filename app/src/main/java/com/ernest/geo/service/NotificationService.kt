package com.ernest.geo.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.content.Intent
import android.app.Notification
import android.app.RemoteInput
import com.ernest.geo.data.NotificationRepository
import com.ernest.geo.voice.GeoVoice
import com.ernest.geo.data.NotificationStorage
import com.ernest.geo.settings.NotificationPrivacy
import com.ernest.geo.settings.PrivacySettings
import com.ernest.geo.ai.AIManager
import com.ernest.geo.ai.WhatsAppReplyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationService : NotificationListenerService() {

    private val aiManager = AIManager()
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("GEO_NOTIFICATION", "NotificationService created")
        GeoVoice.initialize(this)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        android.util.Log.d("GEO_NOTIFICATION", "Listener Connected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (!PrivacySettings.isAppEnabled(this)) return

        android.util.Log.d("GEO_NOTIFICATION", "Notification received from: ${sbn?.packageName}")

        val notification = sbn?.notification ?: return
        val extras = notification.extras ?: return

        // 1. Skip if this is a group summary
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) {
            return
        }

        // 2. Skip ongoing/service notifications
        if (sbn.isOngoing || notification.category == Notification.CATEGORY_SERVICE) {
            return
        }

        val packageName = sbn.packageName
        val title = extras.getString("android.title") ?: "Unknown"
        var finalTitle = title
        
        val text = extras.getCharSequence("android.text")?.toString()
            ?: extras.getCharSequence("android.bigText")?.toString()
            ?: ""

        // Facebook Lite special handling
        if (packageName == "com.facebook.lite") {
            if (title == "Unknown" || title.isEmpty()) {
                if (text.contains("Sent a message")) {
                    val extracted = text.replace("Sent a message.", "").trim()
                    if (extracted.isNotEmpty()) finalTitle = extracted
                }
            }
        }

        android.util.Log.d("GEO_DEBUG", "Processing: pkg=$packageName, title=$title, finalTitle=$finalTitle, text=$text")

        // Filters
        if (finalTitle == "Unknown" && packageName != "com.facebook.lite") return
        if (finalTitle.contains("doing work", true)) return
        if (text.length < 2 && packageName != "com.facebook.lite") return

        // App Whitelist
        val appName = when (packageName) {
            "com.whatsapp" -> "WhatsApp"
            "com.twitter.android" -> "X"
            "com.facebook.katana" -> "Facebook"
            "com.facebook.lite" -> "Facebook Lite"
            "org.telegram.messenger" -> "Telegram"
            "com.facebook.orca" -> "Messenger"
            "com.google.android.gm" -> "Gmail"
            "com.google.android.apps.messaging" -> "Messages"
            "com.instagram.android" -> "Instagram"
            "com.linkedin.android" -> "LinkedIn"
            "com.discord" -> "Discord"
            else -> return
        }

        val privacyLevel = PrivacySettings.getPrivacyLevel(this)

        val speech = when (privacyLevel) {
            NotificationPrivacy.ANNOUNCE_APP_ONLY -> {
                "New message from $appName"
            }
            NotificationPrivacy.ANNOUNCE_SENDER -> {
                "New $appName message from $finalTitle"
            }
            NotificationPrivacy.ANNOUNCE_ALL -> {
                if (text.isEmpty()) {
                    "New $appName message from $finalTitle"
                } else {
                    "New $appName message from $finalTitle. $text"
                }
            }
        }

        NotificationRepository.addNotification(speech)
        NotificationStorage.saveNotification(this, speech)
        
        android.util.Log.d("GEO_SPEECH", "Geo Speaking: $speech")

        // Handle WhatsApp Smart Approval - Only if privacy allows full announcement
        if (packageName == "com.whatsapp" && privacyLevel == NotificationPrivacy.ANNOUNCE_ALL) {
            serviceScope.launch {
                val suggestion = aiManager.generateSuggestedReply(finalTitle, text)
                
                // Find reply action
                var replyAction: Notification.Action? = null
                var remoteInput: RemoteInput? = null
                
                val actions = notification.actions
                if (actions != null) {
                    for (action in actions) {
                        if (action.remoteInputs != null) {
                            for (ri in action.remoteInputs) {
                                if (ri.allowFreeFormInput) {
                                    replyAction = action
                                    remoteInput = ri
                                    break
                                }
                            }
                        }
                        if (replyAction != null) break
                    }
                }

                if (replyAction != null && remoteInput != null) {
                    WhatsAppReplyManager.setPendingReply(replyAction.actionIntent, remoteInput, suggestion)
                    
                    val waSpeech = "$finalTitle said: $text. Suggested reply: $suggestion. Should I send it?"
                    GeoVoice.speak(this@NotificationService, waSpeech) {
                        notifyApprovalNeeded()
                    }
                } else {
                    GeoVoice.speak(this@NotificationService, speech)
                }
            }
        } else {
            GeoVoice.speak(this@NotificationService, speech)
        }
    }

    private fun notifyApprovalNeeded() {
        // Send a broadcast or update a shared state that MainActivity can observe
        val intent = Intent("com.ernest.geo.ACTION_APPROVAL_NEEDED")
        sendBroadcast(intent)
    }
}