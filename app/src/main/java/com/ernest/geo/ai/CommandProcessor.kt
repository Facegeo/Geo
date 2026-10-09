package com.ernest.geo.ai

import android.content.Context
import com.ernest.geo.data.NotificationRepository
import com.ernest.geo.settings.PrivacySettings
import com.ernest.geo.ai.WhatsAppReplyManager
import com.ernest.geo.reminder.ReminderManager
import com.ernest.geo.util.ContactHelper
import com.ernest.geo.service.GeoAccessibilityService
import org.json.JSONObject
import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import android.provider.AlarmClock
import android.hardware.camera2.CameraManager
import android.net.Uri

// Inject Context via constructor
class CommandProcessor(private val context: Context) {

    private val aiManager = AIManager()

    suspend fun processVision(bitmap: Bitmap, prompt: String): String {
        return aiManager.analyzeImage(context, bitmap, prompt)
    }

    suspend fun process(command: String): String {
        // Clean the command: handle common misrecognitions
        var text = command.lowercase()
            .replace("geother", "turn off")
            .replace("geoton", "turn on")
            .replace("geo", "")
            .replace("heyo", "")
            .replace("jio", "")
            .replace("hey", "")
            .replace("dear", "")
            .trim()

        // Handle WhatsApp Approval Flow
        when (WhatsAppReplyManager.currentState) {
            WhatsAppReplyManager.State.WAITING_FOR_APPROVAL -> {
                if (text == "send") {
                    val success = WhatsAppReplyManager.sendReply(context)
                    return if (success) "Reply sent." else "I couldn't send the reply."
                }
                if (text == "edit") {
                    WhatsAppReplyManager.enterEditMode()
                    return "What should I say instead?"
                }
                if (text == "cancel") {
                    WhatsAppReplyManager.clear()
                    return "Okay, I won't send it."
                }
            }
            WhatsAppReplyManager.State.WAITING_FOR_EDIT_TEXT -> {
                WhatsAppReplyManager.updateReplyAndReady(command)
                return "Suggested reply: $command. Should I send it?"
            }
            else -> {}
        }

        // --- Geo State Controls (Check these FIRST for system commands) ---
        
        // Logical check for "Turn Off"
        val isTurnOff = (text.contains("turn") && text.contains("off")) || 
                        text.contains("deactivate") || 
                        text.contains("go to sleep") || 
                        text.contains("stop listening")
        
        if (isTurnOff) {
            PrivacySettings.setAppEnabled(context, false)
            return "Geo is now inactive."
        }
        
        // Logical check for "Turn On"
        val isTurnOn = (text.contains("turn") && text.contains("on")) || 
                       text.contains("activate") || 
                       text.contains("wake up") ||
                       text.contains("yourself on")
        
        if (isTurnOn) {
            PrivacySettings.setAppEnabled(context, true)
            return "Geo is now active."
        }

        // --- Offline Commands ---

        // 1. Camera
        if (text.contains("open camera")) {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return "Opening camera."
        }

        // 2. Flashlight
        if (text.contains("turn on flashlight") || text.contains("flashlight on")) {
            return try {
                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                val cameraId = cameraManager.cameraIdList[0]
                cameraManager.setTorchMode(cameraId, true)
                "Flashlight turned on."
            } catch (e: Exception) {
                "I couldn't turn on the flashlight."
            }
        }
        if (text.contains("turn off flashlight") || text.contains("flashlight off")) {
            return try {
                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                val cameraId = cameraManager.cameraIdList[0]
                cameraManager.setTorchMode(cameraId, false)
                "Flashlight turned off."
            } catch (e: Exception) {
                "I couldn't turn off the flashlight."
            }
        }

        // 3. Open WhatsApp
        if (text.contains("open whatsapp")) {
            val intent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
            return if (intent != null) {
                context.startActivity(intent)
                "Opening WhatsApp."
            } else {
                "WhatsApp is not installed."
            }
        }

        // 4. Call Contact
        if (text.startsWith("call ")) {
            val name = text.removePrefix("call ").trim()
            val number = ContactHelper.getNumberFromName(context, name)
            return if (number != null) {
                val intent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$number")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                "Calling $name."
            } else {
                "I couldn't find $name in your contacts."
            }
        }

        // 5. Summarize Screen
        if (text.contains("summarize this page") || text.contains("summarize the screen")) {
            val screenText = GeoAccessibilityService.getScreenText()
            return if (screenText.isNotEmpty()) {
                aiManager.askGeo(context, "Please summarize this screen content: $screenText")
            } else {
                "I can't see anything on the screen to summarize. Make sure Geo Accessibility is on."
            }
        }

        // --- End Offline Commands ---

        // Handle Reminders / Alarms / Timers
        if (text.contains("remind") || text.contains("alarm") || text.contains("timer") || text.contains("minutes from now")) {
            val now = System.currentTimeMillis()
            android.util.Log.d("GEO_REMINDER", "--- Scheduling Pipeline Start ---")
            android.util.Log.d("GEO_REMINDER", "Current System Time (ms): $now")
            
            val json = aiManager.extractReminder(command)
            android.util.Log.d("GEO_REMINDER", "Extracted structured JSON: $json")
            
            if (json != null) {
                try {
                    val obj = JSONObject(json)
                    val task = obj.getString("task")
                    val type = obj.getString("type")
                    val interval = obj.optLong("interval", 0)
                    
                    var computedTime: Long = 0
                    
                    if (type == "RELATIVE") {
                        val delay = obj.getLong("relativeDelayMs")
                        computedTime = now + delay
                        android.util.Log.d("GEO_REMINDER", "Intent: RELATIVE. Delay: $delay ms")
                    } else {
                        // ABSOLUTE
                        val iso = obj.getString("isoDateTime")
                        android.util.Log.d("GEO_REMINDER", "Intent: ABSOLUTE. ISO: $iso")
                        val localDateTime = java.time.LocalDateTime.parse(iso)
                        computedTime = localDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    }
                    
                    android.util.Log.d("GEO_REMINDER", "Computed Reminder Time (ms): $computedTime")
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
                    android.util.Log.d("GEO_REMINDER", "Computed Reminder Readable: ${sdf.format(java.util.Date(computedTime))}")

                    // Prevent scheduling in the past
                    if (computedTime <= now) {
                        android.util.Log.w("GEO_REMINDER", "Warning: Computed time is in the past or now. Adding 1 minute buffer.")
                        computedTime = now + 60000 
                    }

                    ReminderManager.setReminder(context, task, computedTime, interval)
                    
                    val timeString = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date(computedTime))
                    return "Okay Ernest, I've set that for you. I'll remind you to $task at $timeString."
                } catch (e: Exception) {
                    android.util.Log.e("GEO_REMINDER", "Scheduling Pipeline Error: ${e.message}")
                }
            }
        }

        // Voice Command: Privacy Mode
        if (text.contains("enable privacy mode", true)) {
            PrivacySettings.setPrivacyMode(context, true)
            return "Privacy mode enabled."
        }
        if (text.contains("disable privacy mode", true)) {
            PrivacySettings.setPrivacyMode(context, false)
            return "Privacy mode disabled."
        }

        // Existing Notification logic
        if ("notification" in text || "notifications" in text ||
            "messages" in text || "alerts" in text) {
            val count = NotificationRepository.notifications.size
            return if (count == 0) {
                "You currently have no stored notifications."
            } else {
                "You currently have $count stored notifications."
            }
        }

        // Fallback to Gemini
        aiManager.extractAndStoreFact(context, command)
        
        return aiManager.askGeo(
            context,
            """
            User: $command
            """.trimIndent()
        )
    }
}
