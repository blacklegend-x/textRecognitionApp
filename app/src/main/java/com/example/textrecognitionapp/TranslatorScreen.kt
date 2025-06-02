package com.example.textrecognitionapp

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.util.*

@Composable
fun TranslatorScreen(onSpeakRequested: (String, Locale) -> Unit) {
    val context = LocalContext.current

    val cameraPermissionGranted = remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraPermissionGranted.value = granted
        if (!granted) Toast.makeText(context, "Brak uprawnień do aparatu", Toast.LENGTH_LONG).show()
    }
    LaunchedEffect(Unit) {
        if (!cameraPermissionGranted.value) cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    var inputText by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }
    var translating by remember { mutableStateOf(false) }
    var modelReady by remember { mutableStateOf(false) }

    val langMap = mapOf(
        "Angielski" to TranslateLanguage.ENGLISH,
        "Niemiecki" to TranslateLanguage.GERMAN,
        "Hiszpański" to TranslateLanguage.SPANISH,
        "Francuski" to TranslateLanguage.FRENCH,
        "Polski" to TranslateLanguage.POLISH
    )
    val langNames = langMap.keys.toList()
    var srcLang by remember { mutableStateOf("Polski") }
    var tgtLang by remember { mutableStateOf("Angielski") }

    val translator = remember(srcLang, tgtLang) {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(langMap[srcLang] ?: TranslateLanguage.POLISH)
            .setTargetLanguage(langMap[tgtLang] ?: TranslateLanguage.ENGLISH)
            .build()
        Translation.getClient(options)
    }
    val downloadConditions = DownloadConditions.Builder().requireWifi().build()

    LaunchedEffect(srcLang, tgtLang) {
        modelReady = false
        translatedText = "Pobieranie modelu językowego..."
        translator.downloadModelIfNeeded(downloadConditions)
            .addOnSuccessListener {
                modelReady = true
                translatedText = ""
            }
            .addOnFailureListener { e ->
                translatedText = "Błąd pobierania modelu: ${e.message}"
            }
    }

    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            try {
                val image = InputImage.fromFilePath(context, it)
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    .process(image)
                    .addOnSuccessListener { result -> inputText = result.text }
                    .addOnFailureListener { e -> translatedText = "Błąd OCR: ${e.message}" }
            } catch (e: Exception) {
                translatedText = "Błąd podczas przetwarzania obrazu: ${e.message}"
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            photoUri?.let { uri ->
                try {
                    val image = InputImage.fromFilePath(context, uri)
                    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                        .process(image)
                        .addOnSuccessListener { result -> inputText = result.text }
                        .addOnFailureListener { e -> translatedText = "Błąd OCR z aparatu: ${e.message}" }
                } catch (e: Exception) {
                    translatedText = "Błąd przetwarzania zdjęcia: ${e.message}"
                }
            }
        } else {
            Toast.makeText(context, "Nie udało się wykonać zdjęcia", Toast.LENGTH_SHORT).show()
        }
    }

    fun translate() {
        if (inputText.isBlank() || !modelReady) return
        translating = true
        translator.translate(inputText)
            .addOnSuccessListener { result ->
                translatedText = result
                translating = false
            }
            .addOnFailureListener { e ->
                translatedText = "Błąd tłumaczenia: ${e.message}"
                translating = false
            }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                Text("Skanuj tekst z obrazu")
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                val file = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    "ocr_${System.currentTimeMillis()}.jpg"
                )
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                photoUri = uri
                cameraLauncher.launch(uri)
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Użyj aparatu")
            }

            Spacer(Modifier.height(8.dp))
            LanguageDropdown("Przetłumacz z:", langNames, srcLang) { srcLang = it }
            Spacer(Modifier.height(8.dp))
            LanguageDropdown("Przetłumacz na:", langNames, tgtLang) { tgtLang = it }

            Spacer(Modifier.height(16.dp))
            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text("Tekst do tłumaczenia") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                singleLine = false,
                maxLines = Int.MAX_VALUE
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = ::translate,
                enabled = modelReady && !translating,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (translating) "Tłumaczę…" else "Tłumacz")
            }

            Spacer(Modifier.height(8.dp))
            TextField(
                value = translatedText,
                onValueChange = {},
                label = { Text("Przetłumaczony tekst") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                singleLine = false,
                maxLines = Int.MAX_VALUE,
                readOnly = true
            )

            Spacer(Modifier.height(8.dp))
            Button(onClick = { inputText = ""; translatedText = "" }, modifier = Modifier.fillMaxWidth()) {
                Text("Wyczyść tekst")
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    if (inputText.isNotBlank()) onSpeakRequested(inputText, languageCodeFor(langMap[srcLang]!!))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Czytaj tekst oryginalny")
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    if (translatedText.isNotBlank()) onSpeakRequested(translatedText, languageCodeFor(langMap[tgtLang]!!))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Czytaj tłumaczenie")
            }
        }
    }
}
