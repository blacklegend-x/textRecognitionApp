package com.example.textrecognitionapp

import com.google.mlkit.nl.translate.TranslateLanguage
import java.util.*

fun languageCodeFor(code: String): Locale = when (code) {
    TranslateLanguage.ENGLISH -> Locale("en", "US")
    TranslateLanguage.GERMAN -> Locale("de", "DE")
    TranslateLanguage.FRENCH -> Locale("fr", "FR")
    TranslateLanguage.SPANISH -> Locale("es", "ES")
    TranslateLanguage.POLISH -> Locale("pl", "PL")
    else -> Locale.getDefault()
}
