package com.ernest.geo.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf // Add this
import androidx.compose.runtime.setValue
import androidx.compose.runtime.State
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ernest.geo.ai.CommandProcessor
import com.ernest.geo.data.GeoMemory
import com.ernest.geo.model.Message
import kotlinx.coroutines.launch
import com.ernest.geo.ui.model.GeoState

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    // Initialize CommandProcessor using the Application Context
    private val commandProcessor = CommandProcessor(application.applicationContext)

    val messages = mutableStateListOf<Message>()

    private val _geoState = mutableStateOf(GeoState.IDLE)
    private val _isVisionFlow = mutableStateOf(false)
    private val _capturedBitmap = mutableStateOf<Bitmap?>(null)

    // Public immutable State for the UI to observe
    val geoState: State<GeoState> = _geoState
    val isVisionFlow: State<Boolean> = _isVisionFlow
    val capturedBitmap: State<Bitmap?> = _capturedBitmap

    init {
        messages.add(
            Message(
                text = "Hello Ernest. I am Geo.",
                isUser = false
            )
        )
    }

    fun setState(state: GeoState) {
        _geoState.value = state
        if (state == GeoState.VISION) {
            _isVisionFlow.value = true
        } else if (state == GeoState.IDLE || state == GeoState.DASHBOARD || state == GeoState.SETTINGS) {
            _isVisionFlow.value = false
            _capturedBitmap.value = null
        }
    }

    fun setCapturedBitmap(bitmap: Bitmap?) {
        _capturedBitmap.value = bitmap
    }

    fun processCommand(
        command: String,
        onResponse: (String) -> Unit
    ) {
        setState(GeoState.THINKING)

        messages.add(
            Message(
                text = command,
                isUser = true
            )
        )

        // Track user message in memory
        GeoMemory.addUserMessage(command)

        viewModelScope.launch {
            // Process the command using the defined processor
            val response =
                commandProcessor.process(command)

            android.util.Log.d(
                "GEO_DEBUG",
                "CommandProcessor response: $response"
            )

            messages.add(
                Message(
                    text = response,
                    isUser = false
                )
            )

            // Track Geo's response in memory
            GeoMemory.addGeoMessage(response)

            setState(GeoState.SPEAKING)

            onResponse(response)
        }
    }

    fun processVisionCommand(
        bitmap: Bitmap,
        prompt: String,
        onResponse: (String) -> Unit
    ) {
        setState(GeoState.ANALYZING)

        messages.add(
            Message(
                text = "[Image Analysis Request]: $prompt",
                isUser = true
            )
        )

        viewModelScope.launch {
            val response = commandProcessor.processVision(bitmap, prompt)

            messages.add(
                Message(
                    text = response,
                    isUser = false
                )
            )

            GeoMemory.addGeoMessage(response)

            setState(GeoState.SPEAKING)

            onResponse(response)
        }
    }
}