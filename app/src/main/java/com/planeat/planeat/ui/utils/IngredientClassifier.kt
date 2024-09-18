package com.planeat.planeat.ui.utils

import android.util.Log
import org.jsoup.Jsoup

class IngredientClassifier {
    fun classify(ingredient: String): String {
        var category = ""
        try {
            // Make the request to the search URL for the ingredient
            val url = "https://www.metro.ca/en/online-grocery/search?filter=$ingredient"
            val response = Jsoup.connect(url).userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:129.0) Gecko/20100101 Firefox/129.0").execute()
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

        } catch (error: Exception) {
            Log.e("IngredientClassifier", error.toString())
            return ""
        }

        if (category.isEmpty()) {
            category = "other"
        }

        return category.replace("&amp", "&")
    }
}