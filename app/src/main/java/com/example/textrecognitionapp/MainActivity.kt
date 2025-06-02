package com.example.textrecognitionapp

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import com.example.textrecognitionapp.ui.theme.TextRecognitionAppTheme
import java.util.*

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var ttsManager: TtsManager
    private var pendingSpeakText: Pair<String, Locale>? = null

    private val REQUEST_INSTALL_TTS_DATA = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ttsManager = TtsManager(this) { text, locale ->
            speakText(text, locale)
        }
        ttsManager.initTts()

        setContent {
            TextRecognitionAppTheme(darkTheme = true) {
                TranslatorScreen(
                    onSpeakRequested = { text, locale ->
                        speakText(text, locale)
                    }
                )
            }
        }
    }

    private fun speakText(text: String, locale: Locale) {
        if (!ttsManager.isInitialized()) {
            pendingSpeakText = text to locale
            return
        }
        val result = ttsManager.setLanguage(locale)
        when (result) {
            TextToSpeech.LANG_MISSING_DATA -> {
                Toast.makeText(
                    this,
                    "Brak danych głosowych dla ${locale.displayName}. Instaluję...",
                    Toast.LENGTH_SHORT
                ).show()
                installTtsData()
                pendingSpeakText = text to locale
            }
            TextToSpeech.LANG_NOT_SUPPORTED -> {
                Toast.makeText(
                    this,
                    "Język ${locale.displayName} nie jest wspierany",
                    Toast.LENGTH_SHORT
                ).show()
            }
            else -> {
                ttsManager.speak(text)
            }
        }
    }

    private fun installTtsData() {
        try {
            val intent = Intent(ACTION_INSTALL_TTS_DATA)
            startActivityForResult(intent, REQUEST_INSTALL_TTS_DATA)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Brak aplikacji do instalacji danych TTS", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_INSTALL_TTS_DATA) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "Dane TTS zainstalowane, spróbuj ponownie", Toast.LENGTH_SHORT).show()
                pendingSpeakText?.let { (text, locale) ->
                    speakText(text, locale)
                    pendingSpeakText = null
                }
            } else {
                Toast.makeText(this, "Nie zainstalowano danych TTS", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onInit(status: Int) {
    }

    override fun onDestroy() {
        ttsManager.shutdown()
        super.onDestroy()
    }
}
