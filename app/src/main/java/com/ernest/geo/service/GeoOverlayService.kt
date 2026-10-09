package com.ernest.geo.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import com.ernest.geo.MainActivity
import com.ernest.geo.R
import com.ernest.geo.settings.PrivacySettings
import android.content.SharedPreferences
import com.ernest.geo.voice.VoiceRecognizer
import com.ernest.geo.ai.CommandProcessor
import com.ernest.geo.voice.GeoVoice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeoOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var voiceRecognizer: VoiceRecognizer? = null
    private val commandProcessor by lazy { CommandProcessor(this) }
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    
    private val CHANNEL_ID = "geo_overlay_channel"
    private val NOTIFICATION_ID = 101

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "app_enabled" || key == "bubble_visible") {
            updateVisibility()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        
        createNotificationChannel()
        
        GeoVoice.initialize(this)
        
        getSharedPreferences("geo_settings", Context.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(prefsListener)
        
        voiceRecognizer = VoiceRecognizer(
            context = this,
            onResult = { spokenText ->
                serviceScope.launch {
                    val response = commandProcessor.process(spokenText)
                    GeoVoice.speak(this@GeoOverlayService, response) {
                        // Stop foreground when Geo is done speaking
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            stopForeground(STOP_FOREGROUND_REMOVE)
                        } else {
                            @Suppress("DEPRECATION")
                            stopForeground(true)
                        }
                    }
                }
            },
            onError = { 
                Log.e("GEO_OVERLAY", "Voice Error: $it")
                // Stop foreground if there's an error (e.g., no speech)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
            }
        )

        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_bubble, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 100
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager?.addView(floatingView, params)

        val bubble = floatingView?.findViewById<ImageView>(R.id.bubble_icon)
        
        bubble?.setOnTouchListener(object : View.OnTouchListener {
            private var lastAction: Int = 0
            private var initialX: Int = 0
            private var initialY: Int = 0
            private var initialTouchX: Float = 0f
            private var initialTouchY: Float = 0f

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        lastAction = MotionEvent.ACTION_DOWN
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (lastAction == MotionEvent.ACTION_DOWN) {
                            // If Geo is currently speaking, a tap should stop it
                            GeoVoice.stop()
                            
                            // If it wasn't speaking, or after stopping, start listening
                            // Note: stopForeground is called inside GeoVoice.speak's onComplete or here if needed
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                stopForeground(STOP_FOREGROUND_REMOVE)
                            } else {
                                @Suppress("DEPRECATION")
                                stopForeground(true)
                            }

                            // Start Foreground only when tapping the bubble to listen
                            startForeground(NOTIFICATION_ID, createNotification())
                            voiceRecognizer?.startListening()
                        }
                        lastAction = MotionEvent.ACTION_UP
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager?.updateViewLayout(floatingView, params)
                        lastAction = MotionEvent.ACTION_MOVE
                        return true
                    }
                }
                return false
            }
        })
        
        updateVisibility()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Geo Active",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Geo is Listening")
            .setContentText("I'm ready for your command.")
            .setSmallIcon(R.drawable.geo_orb)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateVisibility() {
        val enabled = PrivacySettings.isAppEnabled(this)
        val visible = PrivacySettings.isBubbleVisible(this)
        
        floatingView?.visibility = if (visible) View.VISIBLE else View.GONE
        floatingView?.alpha = if (enabled) 1.0f else 0.4f
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceRecognizer?.destroy()
        getSharedPreferences("geo_settings", Context.MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(prefsListener)
        if (floatingView != null) windowManager?.removeView(floatingView)
    }
}
