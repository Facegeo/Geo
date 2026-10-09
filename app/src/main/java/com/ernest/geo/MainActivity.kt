package com.ernest.geo

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.runtime.mutableStateOf
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.ernest.geo.service.GeoOverlayService
import com.ernest.geo.ui.model.GeoState
import com.ernest.geo.ui.screens.ChatScreen
import com.ernest.geo.ui.screens.DashboardScreen
import com.ernest.geo.ui.screens.CommandCenterScreen
import com.ernest.geo.ui.settings.SettingsScreen
import com.ernest.geo.ui.theme.GeoTheme
import com.ernest.geo.viewmodel.ChatViewModel
import com.ernest.geo.voice.GeoVoice
import com.ernest.geo.voice.VoiceRecognizer
import com.ernest.geo.ai.WhatsAppReplyManager
import com.ernest.geo.data.ReminderRepository
import com.ernest.geo.reminder.ReminderManager

class MainActivity : ComponentActivity() {

    private lateinit var voiceRecognizer: VoiceRecognizer
    private val chatViewModel by viewModels<ChatViewModel>()
    private var imageCapture: ImageCapture? = null
    private var capturedBitmap: Bitmap? = null

    private val geoReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (action == "com.ernest.geo.ACTION_APPROVAL_NEEDED") {
                Log.d("GEO_DEBUG", "Approval needed broadcast received")
                chatViewModel.setState(GeoState.LISTENING)
                voiceRecognizer.startListening()
            } else if (action == "com.ernest.geo.ACTION_STATE_CHANGE") {
                val stateStr = intent.getStringExtra("state")
                try {
                    if (stateStr != null) {
                        chatViewModel.setState(GeoState.valueOf(stateStr))
                    }
                } catch (e: Exception) {}
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.RECORD_AUDIO] == true) {
            // If we were waiting for camera too
            if (permissions[Manifest.permission.CAMERA] == true) {
                chatViewModel.setState(GeoState.VISION)
                startCamera()
            } else {
                voiceRecognizer.startListening()
            }
        } else if (permissions[Manifest.permission.CAMERA] == true) {
            chatViewModel.setState(GeoState.VISION)
            startCamera()
        } else {
            Log.e("GEO_DEBUG", "Permission Denied")
            chatViewModel.setState(GeoState.IDLE)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Initialize Repositories
        ReminderRepository.loadReminders(this)
        ReminderManager.restoreAlarms(this)

        // --- Diagnostic Service Check & Force Rebind ---
        val componentName = ComponentName(this, com.ernest.geo.service.NotificationService::class.java)
        val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        val isEnabled = enabledListeners?.contains(componentName.flattenToString()) == true

        if (isEnabled) {
            NotificationListenerService.requestRebind(componentName)
        } else {
            // Redirect to Notification Access settings if not enabled
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
        }
        // ------------------------------------------------

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        } else {
            startService(Intent(this, GeoOverlayService::class.java))
        }

        GeoVoice.initialize(this)

        val filter = IntentFilter().apply {
            addAction("com.ernest.geo.ACTION_APPROVAL_NEEDED")
            addAction("com.ernest.geo.ACTION_STATE_CHANGE")
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(geoReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(geoReceiver, filter)
        }

        voiceRecognizer = VoiceRecognizer(
            context = this,
            onResult = { spokenText ->
                val bitmap = capturedBitmap
                if (bitmap != null) {
                    Log.d("GEO_DEBUG", "Vision result received. Text: $spokenText. Bitmap size: ${bitmap.width}x${bitmap.height}")
                    capturedBitmap = null
                    chatViewModel.processVisionCommand(bitmap, spokenText) { response ->
                        Log.d("GEO_DEBUG", "AI response for vision ready: ${response.take(30)}...")
                        GeoVoice.speak(this, response) {
                            chatViewModel.setState(GeoState.IDLE)
                        }
                    }
                } else {
                    chatViewModel.processCommand(spokenText) { response ->
                        GeoVoice.speak(this, response) {
                            if (WhatsAppReplyManager.currentState != WhatsAppReplyManager.State.IDLE) {
                                // Still in a flow, keep listening
                                chatViewModel.setState(GeoState.LISTENING)
                                voiceRecognizer.startListening()
                            } else {
                                chatViewModel.setState(GeoState.IDLE)
                            }
                        }
                    }
                }
            },
            onError = { error ->
                Log.e("GEO_DEBUG", "Voice Error: $error")
                chatViewModel.setState(GeoState.IDLE)
            },
            onReady = {
                // Optional: handle UI when ready
            }
        )

        setContent {
            GeoTheme {
                val currentState = chatViewModel.geoState.value

                when (currentState) {
                    GeoState.SETTINGS -> {
                        SettingsScreen(
                            onBack = {
                                chatViewModel.setState(GeoState.DASHBOARD)
                            }
                        )
                    }
                    GeoState.DASHBOARD -> {
                        DashboardScreen(
                            onBack = {
                                chatViewModel.setState(GeoState.IDLE)
                            },
                            onSettingsClick = {
                                chatViewModel.setState(GeoState.SETTINGS)
                            },
                            onCommandCenterClick = {
                                chatViewModel.setState(GeoState.COMMAND_CENTER)
                            }
                        )
                    }
                    GeoState.COMMAND_CENTER -> {
                        CommandCenterScreen(
                            onBack = {
                                chatViewModel.setState(GeoState.DASHBOARD)
                            }
                        )
                    }
                    else -> {
                        ChatScreen(
                            geoState = currentState,
                            isVisionFlow = chatViewModel.isVisionFlow.value,
                            capturedBitmap = chatViewModel.capturedBitmap.value,
                            onTalkClick = {
                                Log.d("GEO_DEBUG", "MIC BUTTON PRESSED. State: $currentState")

                                if (currentState == GeoState.VISION) {
                                    chatViewModel.setState(GeoState.LISTENING)
                                    takePhoto { bitmap ->
                                        Log.d("GEO_DEBUG", "Photo captured. Starting voice recognizer.")
                                        capturedBitmap = bitmap
                                        chatViewModel.setCapturedBitmap(bitmap)
                                        voiceRecognizer.startListening()
                                    }
                                } else {
                                    chatViewModel.setState(GeoState.LISTENING)

                                    if (ContextCompat.checkSelfPermission(
                                            this,
                                            Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED &&
                                        ContextCompat.checkSelfPermission(
                                            this,
                                            Manifest.permission.READ_CONTACTS
                                        ) == PackageManager.PERMISSION_GRANTED &&
                                        ContextCompat.checkSelfPermission(
                                            this,
                                            Manifest.permission.CALL_PHONE
                                        ) == PackageManager.PERMISSION_GRANTED
                                    ) {
                                        voiceRecognizer.startListening()
                                    } else {
                                        requestPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.RECORD_AUDIO,
                                                Manifest.permission.READ_CONTACTS,
                                                Manifest.permission.CALL_PHONE
                                            )
                                        )
                                    }
                                }
                            },
                            onVisionClick = {
                                if (currentState == GeoState.VISION) {
                                    chatViewModel.setState(GeoState.IDLE)
                                } else {
                                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                        chatViewModel.setState(GeoState.VISION)
                                        startCamera()
                                    } else {
                                        requestPermissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                                    }
                                }
                            },
                            onStopClick = {
                                Log.d("GEO_DEBUG", "CANCEL BUTTON PRESSED")
                                chatViewModel.setState(GeoState.IDLE)
                                voiceRecognizer.stopListening()
                                GeoVoice.stop()
                            },
                            onSettingsClick = {
                                chatViewModel.setState(GeoState.DASHBOARD)
                            },
                            onPreviewViewCreated = { view ->
                                previewView = view
                                if (chatViewModel.geoState.value == GeoState.VISION) {
                                    startCamera()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(geoReceiver)
        } catch (e: Exception) {}
        voiceRecognizer.destroy()
    }

    private var previewView: PreviewView? = null

    private fun startCamera() {
        val previewView = previewView ?: return
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
            preview.surfaceProvider = previewView.surfaceProvider
            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Log.e("GEO_DEBUG", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto(onBitmap: (Bitmap) -> Unit) {
        val imageCapture = imageCapture ?: run {
            Log.e("GEO_DEBUG", "ImageCapture is null. Starting camera first.")
            startCamera()
            return
        }

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    image.close()
                    if (bitmap != null) {
                        onBitmap(bitmap)
                    } else {
                        Log.e("GEO_DEBUG", "Failed to decode captured photo into bitmap")
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("GEO_DEBUG", "Photo capture failed: ${exception.message}", exception)
                }
            }
        )
    }
}
