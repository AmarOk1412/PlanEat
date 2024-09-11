/*
 * Copyright 2020 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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