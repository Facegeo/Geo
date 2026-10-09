package com.ernest.geo.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.ernest.geo.settings.GeoPersonality
import com.ernest.geo.settings.PrivacySettings
import java.util.Locale

enum class GeoEmotion {
    NORMAL,
    EXCITED,
    SOFT,
    URGENT
}

object GeoVoice : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var initialized = false
    private var currentContext: Context? = null
    private var isFallbackAttempted = false
    
    private var pendingSpeech: Pair<String, GeoEmotion?>? = null
    private val speechQueue = mutableListOf<SpeechRequest>()
    private var isSpeaking = false

    data class SpeechRequest(
        val text: String,
        val emotion: GeoEmotion?,
        val onComplete: (() -> Unit)?
    )

    fun initialize(context: Context) {
        currentContext = context.applicationContext
        android.util.Log.d("GEO_VOICE", "Initializing GeoVoice")
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext, this, "com.google.android.tts")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            initialized = true
            android.util.Log.d("GEO_VOICE", "GeoVoice Ready")
            
            val pending = pendingSpeech
            if (pending != null) {
                pendingSpeech = null
                currentContext?.let { speak(it, pending.first, pending.second) }
            }
        } else if (!isFallbackAttempted) {
            isFallbackAttempted = true
            currentContext?.let { tts = TextToSpeech(it, this) }
        }
    }

    private fun detectEmotion(text: String): GeoEmotion {
        val lower = text.lowercase()
        return when {
            lower.contains("congratulations") || lower.contains("congrats") || 
            lower.contains("won") || lower.contains("awesome") || 
            lower.contains("great news") || (lower.contains("!") && text.length < 100) -> GeoEmotion.EXCITED
            
            lower.contains("sorry") || lower.contains("loss") || 
            lower.contains("sad") || lower.contains("died") || 
            lower.contains("rip") || lower.contains("condolences") -> GeoEmotion.SOFT
            
            lower.contains("urgent") || lower.contains("emergency") || 
            lower.contains("danger") || lower.contains("warning") ||
            lower.contains("help me") -> GeoEmotion.URGENT
            
            else -> GeoEmotion.NORMAL
        }
    }

    private fun applyVoiceSettings(context: Context, emotion: GeoEmotion) {
        val personality = PrivacySettings.getPersonality(context)
        val ttsInstance = tts ?: return
        val locale = if (personality == GeoPersonality.BRITISH) Locale.UK else Locale.US
        ttsInstance.setLanguage(locale)

        var pitch = when (personality) {
            GeoPersonality.CLASSIC -> 1.1f
            GeoPersonality.CALM -> 0.8f
            GeoPersonality.ENERGETIC -> 1.4f
            GeoPersonality.BRITISH -> 1.0f
        }

        var rate = when (personality) {
            GeoPersonality.CLASSIC -> 1.0f
            GeoPersonality.CALM -> 0.85f
            GeoPersonality.ENERGETIC -> 1.2f
            GeoPersonality.BRITISH -> 1.0f
        }

        when (emotion) {
            GeoEmotion.EXCITED -> { pitch += 0.3f; rate += 0.2f }
            GeoEmotion.SOFT -> { pitch -= 0.2f; rate -= 0.15f }
            GeoEmotion.URGENT -> { pitch += 0.1f; rate += 0.3f }
            GeoEmotion.NORMAL -> {}
        }
        ttsInstance.setPitch(pitch)
        ttsInstance.setSpeechRate(rate)
    }

    fun speak(context: Context, text: String, forceEmotion: GeoEmotion? = null, onComplete: (() -> Unit)? = null) {
        if (!initialized) {
            pendingSpeech = Pair(text, forceEmotion)
            onComplete?.invoke()
            return
        }

        speechQueue.add(SpeechRequest(text, forceEmotion, onComplete))
        if (!isSpeaking) {
            processNextInQueue(context)
        }
    }

    private fun processNextInQueue(context: Context) {
        if (speechQueue.isEmpty()) {
            isSpeaking = false
            return
        }

        isSpeaking = true
        val request = speechQueue.removeAt(0)
        val emotion = request.emotion ?: detectEmotion(request.text)
        applyVoiceSettings(context, emotion)

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                request.onComplete?.invoke()
                processNextInQueue(context)
            }
            override fun onError(utteranceId: String?) {
                request.onComplete?.invoke()
                processNextInQueue(context)
            }
        })

        tts?.speak(request.text, TextToSpeech.QUEUE_FLUSH, null, "geo_utterance")
    }

    fun stop() {
        android.util.Log.d("GEO_VOICE", "Stop called")
        speechQueue.clear()
        tts?.stop()
        isSpeaking = false
    }
}
