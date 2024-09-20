package com.planeat.planeat.ui.utils

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale

class Translator {
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun translate(value: String): String = suspendCancellableCoroutine { continuation ->
        val currentLocale = Locale.getDefault()

        // If the current locale is English, return the name directly
        if (currentLocale.language == Locale.ENGLISH.language) {
            continuation.resume(value) { }
            return@suspendCancellableCoroutine
        }

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.fromLanguageTag(currentLocale.language)!!)
            .build()

        val englishTranslator = Translation.getClient(options)
        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()

        // Download the translation model if needed
        englishTranslator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                // Once the model is downloaded, perform the translation
                englishTranslator.translate(value)
                    .addOnSuccessListener { translatedText ->
                        continuation.resume(translatedText.replaceFirstChar(Char::titlecase)) { }
                    }
                    .addOnFailureListener {
                        // If translation fails, return the original name
                        continuation.resume(value) { }
                    }
            }
            .addOnFailureListener {
                // If model download fails, return the original name
                continuation.resume(value) { }
            }
    }
}