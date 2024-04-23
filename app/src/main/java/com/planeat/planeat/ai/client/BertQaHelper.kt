package com.planeat.planeat.ai.client

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.planeat.planeat.ui.AppModel
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.text.qa.BertQuestionAnswerer
import org.tensorflow.lite.task.text.qa.BertQuestionAnswerer.BertQuestionAnswererOptions
import org.tensorflow.lite.task.text.qa.QaAnswer

class BertQaHelper(
    val context: Context,
    var numThreads: Int = 2,
    var currentDelegate: Int = 0,
    val answererListener: AppModel
) {

    private var bertQuestionAnswerer: BertQuestionAnswerer? = null

    init {
        setupBertQuestionAnswerer()
    }

    fun clearBertQuestionAnswerer() {
        bertQuestionAnswerer = null
    }

    private fun setupBertQuestionAnswerer() {
        val baseOptionsBuilder = BaseOptions.builder().setNumThreads(numThreads)

        when (currentDelegate) {
            DELEGATE_CPU -> {
                // Default
            }
            DELEGATE_GPU -> {
                baseOptionsBuilder.useGpu()
            }
            DELEGATE_NNAPI -> {
                baseOptionsBuilder.useNnapi()
            }
        }

        val options = BertQuestionAnswererOptions.builder()
            .setBaseOptions(baseOptionsBuilder.build())
            .build()

        try {
            bertQuestionAnswerer =
                BertQuestionAnswerer.createFromFileAndOptions(context, BERT_QA_MODEL, options)
        } catch (e: IllegalStateException) {
            answererListener
                .onError("Bert Question Answerer failed to initialize. See error logs for details")
            Log.e(TAG, "TFLite failed to load model with error: " + e.message)
        }
    }

    fun answer(contextOfQuestion: String, question: String) {
        // TODO make thread safe
        if (bertQuestionAnswerer == null) {
            setupBertQuestionAnswerer()
        }

        // Inference time is the difference between the system time at the start and finish of the
        // process
        var inferenceTime = SystemClock.uptimeMillis()

        val answers = bertQuestionAnswerer?.answer(contextOfQuestion, question)
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime

        answererListener.onResults(contextOfQuestion, question, answers, inferenceTime)
    }

    interface AnswererListener {
        fun onError(error: String)
        fun onResults(
            contextOfQuestion: String,
            question: String,
            results: List<QaAnswer>?,
            inferenceTime: Long
        )
    }

    companion object {
        private const val BERT_QA_MODEL = "ingredients.tflite"
        private const val TAG = "BertQaHelper"
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val DELEGATE_NNAPI = 2
    }
}