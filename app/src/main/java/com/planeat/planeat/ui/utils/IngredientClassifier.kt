package org.tensorflow.lite.examples.textclassification.client

import android.content.Context
import android.util.Log
import org.tensorflow.lite.task.text.nlclassifier.NLClassifier
import java.io.IOException

/** Load TfLite model and provide predictions with task api.  */
class IngredientClassifier(private val context: Context) {
    var classifier: NLClassifier? = null

    init {
        try {
            classifier = NLClassifier.createFromFile(context, "ingredients_classifier.tflite")
        } catch (e: IOException) {
            Log.e("PlanEat", e.message ?: "Unknown error")
        }
    }

    fun unload() {
        classifier?.close()
        classifier = null
    }

    fun classify(text: String): String {
        val apiResults = classifier?.classify(text) ?: return "No result"
        val bestCategory = apiResults.maxByOrNull { it.score }
        return bestCategory?.label ?: "No result"
    }
}