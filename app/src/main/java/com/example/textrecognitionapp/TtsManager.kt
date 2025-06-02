package com.example.textrecognitionapp

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.widget.Toast
import java.util.*

class TtsManager(
    private val context: Context,
    private val onInitSuccess: (String, Locale) -> Unit
) : OnInitListener {

    private var textToSpeech: TextToSpeech? = null
    private var isReady = false

    fun initTts() {
        textToSpeech = TextToSpeech(context, this)
    }

    fun isInitialized() = isReady

    fun setLanguage(locale: Locale): Int {
        return textToSpeech?.setLanguage(locale) ?: TextToSpeech.LANG_MISSING_DATA
    }

    fun speak(text: String) {
        if (!isReady) {
            Toast.makeText(context, "TTS nie jest gotowy", Toast.LENGTH_SHORT).show()
            return
        }
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "UTTERANCE_ID")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isReady = true
            // Można tu obsłużyć callback, jeśli chcesz np. odczytać tekst w kolejce
        } else {
            Toast.makeText(context, "Inicjalizacja TTS nie powiodła się", Toast.LENGTH_SHORT).show()
        }
    }

    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isReady = false
    }
}
