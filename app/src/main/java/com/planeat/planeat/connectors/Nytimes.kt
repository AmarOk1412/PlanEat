package com.planeat.planeat.connectors

import android.util.Log
import com.planeat.planeat.data.Recipe
import org.json.JSONArray
import org.json.JSONObject

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.net.URLEncoder
import java.nio.charset.StandardCharsets


class Nytimes : Connector {
    private var maxResult: Int
    private var maxSuggestion: Int = 9

    constructor(maxResult: Int) : super() {
        this.maxResult = maxResult
    }

    override fun handleUrl(url: String): Boolean {
        return url.contains("cooking.nytimes.com")
    }

    override fun parsePages(url: String, onRecipe: (Recipe) -> Unit) {
    }

    override fun search(searchTerm: String, onRecipe: (Recipe) -> Unit) {
        var i = 0
        try {
            val searchTermEscaped = URLEncoder.encode(searchTerm, StandardCharsets.UTF_8.toString())
            val url = "https://cooking.nytimes.com/search?q=$searchTermEscaped"
            val doc: Document = Jsoup.connect(url).timeout(2000).get()

            val elements: Elements = doc.select(".atoms_card__MIRyu")
            for (element in elements) {
                val relurl = element.select(".link_link__7WCQy").attr("href")
                val recipe = getRecipe("https://cooking.nytimes.com$relurl")
                if (recipe.title.isEmpty())
                    continue
                onRecipe(recipe)
                if (i == this.maxResult)
                    break
                i++
            }
        } catch (error: Exception) {
            Log.e("PlanEat", error.toString())
        }
    }

    override fun suggest(onRecipe: (Recipe) -> Unit) {
        var i = 0
        try {
            val url = "https://cooking.nytimes.com/"
            val doc: Document = Jsoup.connect(url).timeout(2000).get()

            val elements: Elements = doc.select(".atoms_card__MIRyu a")
            for (element in elements) {
                val relurl = element.attr("href")  // Get the href attribute
                val recipe = getRecipe("https://cooking.nytimes.com$relurl")  // Assuming you have a function to process the URL
                if (recipe.title.isEmpty())
                    continue
                onRecipe(recipe)  // Pass the recipe object to the callback
                if (i == this.maxSuggestion)
                    break
                i++
            }
        } catch (error: Exception) {
            Log.e("PlanEat", error.toString())
        }

    }

    override fun getRecipe(url: String): Recipe {
        var recipe = Recipe()
        Log.d("PlanEat", "Parse recipe from Nytime: $url")
        try {
            val response = Jsoup.connect(url).timeout(2000).execute()
            val document = response.parse()

            var recipeData: JSONObject? = null

            val scriptElements = document.select("script[type=\"application/ld+json\"]")
            for (element in scriptElements) {
                val scriptContent = element.html()
                val data = JSONObject(scriptContent)

                if (data.getString("@type") == "Recipe") {
                    recipeData = data
                    break
                }
            }

            if (recipeData != null) {
                val name = recipeData.getString("name")
                var tags = mutableListOf<String>()
                if (recipeData.has("recipeCuisine")) {
                    tags = listOf(recipeData.getString("recipeCuisine")).toMutableList()
                }
                if (recipeData.has("keywords")) {
                    tags += recipeData.getString("keywords").split(", ")
                }
                if (recipeData.has("recipeCategory")) {
                    tags += recipeData.getString("recipeCategory").split(", ")
                }
                if (recipeData.has("recipeTags")) {
                    tags += recipeData.getString("recipeTags").split(", ")
                }

                val duration = recipeData.getString("totalTime").replace("\\D".toRegex(), "").toInt()
                val ingredients = recipeData.getJSONArray("recipeIngredient").toList().map { it.toString() }
                val steps = JSONArray()
                val recipeInstructions = recipeData.getJSONArray("recipeInstructions")
                for (i in 0 until recipeInstructions.length()) {
                    val instruction = recipeInstructions.getJSONObject(i)

                    // Create a new JSONObject for each step
                    val stepObject = JSONObject()

                    // Add the text field
                    stepObject.put("text", instruction.getString("text"))

                    // Optionally add the image field if it exists
                    if (instruction.has("image")) {
                        stepObject.put("image", instruction.getString("image"))
                    }

                    // Add the stepObject to the steps array
                    steps.put(stepObject)
                }
                val imageUrl = recipeData.getJSONArray("image").getJSONObject(0).getString("url")

                recipe = recipe.copy(title = name, url = url, image = imageUrl, tags = tags.map { it.lowercase() }, cookingTime = duration, ingredients = ingredients, steps = steps.toString())
            }

        } catch (error: Exception) {
            Log.e("PlanEat", error.toString())
        }
        return recipe
    }
}