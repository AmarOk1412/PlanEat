package com.planeat.planeat.ui.utils

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import java.net.SocketTimeoutException

class IngredientClassifier {

    private val hasConnection = mutableStateOf(true)
    private val checkConnectionJob = mutableStateOf<Job?>(null)

    private fun startCheckConnection() {
        if (checkConnectionJob.value == null) { // Only start if no job is running
            val scope = CoroutineScope(Dispatchers.Default)

            // Launch a job that checks the connection every minute
            checkConnectionJob.value = scope.launch {
                while (!hasConnection.value) {
                    // Perform your connection check here
                    Log.e("PlanEat", "Checking connection to classify ingredients.")

                    // Wait for 1 minute
                    delay(60 * 1000L)

                    try {
                        val url = "https://www.metro.ca/en/online-grocery/search?filter=egg"
                        val response = Jsoup.connect(url).timeout(2000).userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:129.0) Gecko/20100101 Firefox/129.0").execute()
                        response.parse()
                        hasConnection.value = true
                        Log.d("PlanEat", "Connection to classify ingredients is back!")
                    } catch (error: Exception) {
                        Log.d("PlanEat", "Error: $error")
                        continue
                    }
                }

                // When hasConnection becomes false, stop the job and set it to null
                println("No connection, stopping job.")
                checkConnectionJob.value = null
            }
        }
    }

    fun classify(ingredient: String): String {
        if (!hasConnection.value)
            return ""
        var category = ""
        try {
            // Make the request to the search URL for the ingredient
            val url = "https://www.metro.ca/en/online-grocery/search?filter=$ingredient"
            val response = Jsoup.connect(url).timeout(2000).userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:129.0) Gecko/20100101 Firefox/129.0").execute()
            val document = response.parse()

            // Select product tiles with ingredient info
            val elements = document.select(".tile-product")
            val categoryMap = mutableMapOf<String, Int>()

            // Loop through the product tiles and extract data-product-category-en
            run extract@{
                elements.forEach { element ->
                    val productCategory = element.attr("data-product-category-en")
                    if (productCategory.isNotEmpty()) {
                        // Count occurrences of each category
                        val count = categoryMap.getOrDefault(productCategory, 0) + 1
                        categoryMap[productCategory] = count

                        // If the category count reaches 3, stop and return that category
                        if (count == 3) {
                            category = productCategory
                            return@extract
                        }
                    }
                }
            }


            // If no category reached 3, find the most frequent one
            if (category.isEmpty() && categoryMap.isNotEmpty()) {
                category = categoryMap.maxByOrNull { it.value }?.key ?: ""
            }

        } catch (error: SocketTimeoutException) {
            hasConnection.value = false
            startCheckConnection()
        }  catch (error: Exception) {
            Log.e("IngredientClassifier", error.toString())
            return ""
        }

        if (category.isEmpty()) {
            category = "other"
        }

        return category.replace("&amp", "&")
    }
}