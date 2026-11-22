// language: Kotlin, file: VoiceCoach.kt
package com.example.mlbbassistant

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

object VoiceCoach : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isReady = false
    private var lastSpokenAt = 0L
    private var lastText = ""
    
    private const val COOLDOWN_MS = 5000L 

    fun init(context: Context) {
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("id", "ID"))
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isReady = true
            } else {
                tts?.language = Locale.US
                isReady = true
            }
        }
    }

    fun speak(text: String, force: Boolean = false) {
        if (!isReady || text.isBlank()) return
        
        val now = System.currentTimeMillis()
        // Force = true artinya abaikan cooldown (buat peringatan bahaya mendesak)
        if ((force || now - lastSpokenAt > COOLDOWN_MS) && text != lastText) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "MLBB_AI_VOICE")
            lastSpokenAt = now
            lastText = text
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}
