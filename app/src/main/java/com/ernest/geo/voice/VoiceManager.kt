package com.ernest.geo.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class VoiceManager(context: Context) : TextToSpeech.OnInitListener {

    private val tts = TextToSpeech(context, this)
    private var initialized = false

    private var onFinished: (() -> Unit)? = null

    override fun onInit(status: Int) {
        // Diagnostic log: check status code
        Log.d("GEO_SPEECH", "TTS init status = $status")

        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            tts.setSpeechRate(1.0f)
            tts.setPitch(1.1f)

            tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    onFinished?.invoke()
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    onFinished?.invoke()
                }
                override fun onError(utteranceId: String?, errorCode: Int) {
                    onFinished?.invoke()
                }
            })

            initialized = true

            // Diagnostic log: confirm ready state
            Log.d("GEO_SPEECH", "TTS READY")
        } else {
            Log.e("GEO_SPEECH", "TTS initialization failed with status: $status")
        }
    }

    fun speak(text: String, onComplete: (() -> Unit)? = null) {

        this.onFinished = onComplete

        android.util.Log.d(
            "GEO_DEBUG",
            "VoiceManager speak called: $text"
        )

        if (!initialized) {
            android.util.Log.d(
                "GEO_DEBUG",
                "VoiceManager NOT initialized"
            )
            onComplete?.invoke()
            return
        }

        tts.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "geo_response"
        )
    }
}