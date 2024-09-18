package com.planeat.planeat.ui.utils

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.Charset

class TagClassifier(val context: Context) {
    private val interpreter: Interpreter
    private val tokenizer: Map<String, Int>
    private val labels: List<String>
    private val maxLength = 241 // Replace with the actual max length from training
    init {
        // Load the TFLite model
        interpreter = Interpreter(loadModelFile("model_multi_label.tflite"))


        // Load the tokenizer
        tokenizer = loadTokenizer(context.assets.open("tokenizer.json"))

        // Load labels
        labels = loadLabels(context.assets.open("labels.txt"))
    }

    private fun loadModelFile(fileName: String): ByteBuffer {
        context.assets.openFd(fileName).use { fileDescriptor ->
            FileInputStream(fileDescriptor.fileDescriptor).channel.use { fileChannel ->
                val startOffset = fileDescriptor.startOffset
                val declaredLength = fileDescriptor.declaredLength
                return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            }
        }
    }

    fun classify(recipe: String): List<String> {
        val input = preprocessRecipe(recipe)
        val output = Array(1) { FloatArray(labels.size) }
        interpreter.run(input, output)

        val predictions = mutableListOf<String>()
        for (i in output[0].indices) {
            if (output[0][i] > 0.5f) {
                predictions.add(labels[i])
            }
        }
        Log.d("PlanEat", "Predictions: $predictions for recipe: $recipe")
        return predictions
    }

    private fun loadTokenizer(inputStream: InputStream): Map<String, Int> {
        val tokenizerJson = inputStream.bufferedReader().use(BufferedReader::readText)
        val jsonObject = JSONObject(tokenizerJson)
        val tokenizerMap = mutableMapOf<String, Int>()

        jsonObject.keys().forEach { key ->
            val value = jsonObject.getInt(key)
            tokenizerMap[key] = value
        }
        return tokenizerMap
    }


    private fun loadLabels(inputStream: InputStream): List<String> {
        return inputStream.bufferedReader(Charset.forName("UTF-8")).useLines { lines ->
            lines.toList()
        }
    }

    private fun preprocessRecipe(recipe: String): ByteBuffer {
        // Convert the ingredient to a sequence of indices
        val sequence = tokenizer.entries.mapNotNull { (word, index) ->
            if (recipe.contains(word, ignoreCase = true)) index else null
        }.take(maxLength)

        // Create an array for the sequence with padding
        val sequenceArray = IntArray(maxLength) { 0 }

        // Place the sequence at the end of the array
        for (i in sequence.indices) {
            sequenceArray[maxLength - sequence.size + i] = sequence[i]
        }

        // Create ByteBuffer and fill with the sequence
        val byteBuffer = ByteBuffer.allocateDirect(4 * maxLength).apply {
            order(java.nio.ByteOrder.nativeOrder())
        }
        for (value in sequenceArray) {
            byteBuffer.putFloat(value.toFloat())
        }

        return byteBuffer
    }
}