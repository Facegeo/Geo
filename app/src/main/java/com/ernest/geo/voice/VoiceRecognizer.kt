package com.ernest.geo.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.content.ComponentName
import android.speech.RecognitionService

class VoiceRecognizer(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onReady: () -> Unit = {}
) : RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isProcessingResults = false
    
    private val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
    }

    fun startListening() {
        isProcessingResults = false
        try {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                Log.e("GEO_VOICE", "Speech recognition not available on this device")
                onError("Speech recognition not available")
                return
            }

            if (speechRecognizer == null) {
                val googleService = findGoogleRecognitionService()
                
                speechRecognizer = if (googleService != null) {
                    Log.d("GEO_VOICE", "Using Google recognition service: $googleService")
                    SpeechRecognizer.createSpeechRecognizer(context, googleService)
                } else {
                    Log.d("GEO_VOICE", "Google service not found, using default recognizer")
                    SpeechRecognizer.createSpeechRecognizer(context)
                }
                
                speechRecognizer?.setRecognitionListener(this)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("GEO_VOICE", "Error starting recognizer", e)
            onError(e.message ?: "Unknown error")
        }
    }

    private fun findGoogleRecognitionService(): ComponentName? {
        val services = context.packageManager.queryIntentServices(
            Intent(RecognitionService.SERVICE_INTERFACE), 0
        )
        for (info in services) {
            val packageName = info.serviceInfo.packageName
            if (packageName.contains("google", ignoreCase = true)) {
                return ComponentName(packageName, info.serviceInfo.name)
            }
        }
        return null
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
    }

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    override fun onReadyForSpeech(params: Bundle?) {
        Log.d("GEO_VOICE", "Ready for speech")
        onReady()
    }

    override fun onBeginningOfSpeech() {
        Log.d("GEO_VOICE", "Beginning of speech")
    }

    override fun onRmsChanged(rmsdB: Float) {}

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        Log.d("GEO_VOICE", "End of speech")
    }

    override fun onError(error: Int) {
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
            SpeechRecognizer.ERROR_SERVER -> "Error from server"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Unknown error"
        }
        Log.e("GEO_VOICE", "Error: $errorMessage ($error)")
        
        if (error == SpeechRecognizer.ERROR_CLIENT || error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
            destroy()
        }
        
        onError(errorMessage)
    }

    override fun onResults(results: Bundle?) {
        if (isProcessingResults) return
        isProcessingResults = true

        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.getOrNull(0) ?: ""
        Log.d("GEO_VOICE", "Results: $text")
        onResult(text)
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.getOrNull(0) ?: ""
        Log.d("GEO_VOICE", "Partial Results: $text")
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}
}
