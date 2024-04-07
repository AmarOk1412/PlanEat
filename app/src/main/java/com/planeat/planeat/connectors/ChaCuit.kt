package com.planeat.planeat.connectors

import android.util.Log
import com.planeat.planeat.data.Recipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class ChaCuit : Connector {
    private var maxResult: Int

    constructor(maxResult: Int) : super() {
        this.maxResult = maxResult
    }

    override fun handleUrl(url: String): Boolean {
        return url.contains("cha-cu.it") || url.contains("re7.ache.one")
    }

    override fun search(searchTerm: String): List<Recipe> {
        val recipes = mutableListOf<Recipe>()
        try {
            val url = "https://cha-cu.it/recettes"
            val response = Jsoup.connect(url).execute()
            val document = response.parse()

            val elements = document.select(".p-2")
            for (element in elements) {
                val recipeUrl = element.select("a").attr("href")
                val title = element.select(".my-2.text-xl.font-semibold").text()

                if (title.lowercase().contains(searchTerm.lowercase())) {
                    recipes.add(getRecipe(recipeUrl))
                    if (recipes.size == this.maxResult)
                        break
                }
            }
        } catch (error: Exception) {
            Log.e("PlanEat", error.toString())
        }
        return recipes
    }

    override fun getRecipe(url: String): Recipe {
        var recipe: Recipe = Recipe()
        Log.d("PlanEat", "Parse recipe from cha-cu.it: $url")
        try {
            val response = Jsoup.connect(url).execute()
            val document = response.parse()

            val name = document.select("meta[property=\"og:title\"]").attr("content")

            val durationElement = document.select("h5:contains(\"Temps de préparation\")")
            val durationText = durationElement.text()
            val durationMatch = Regex("Temps de préparation : (\\d+)min").find(durationText)
            val duration = durationMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0

            val keywordsMeta = document.select("meta[itemprop=\"keywords\"]").attr("content")
            val tags = keywordsMeta.split(',')

            val firstImage = document.select(".rounded-lg.shadow-sm.w-full.object-contain").attr("src")
            val imageUrl = "https://cha-cu.it$firstImage"

            val ingredients = mutableListOf<String>()
            val equipment = mutableListOf<String>()

            // Extract ingredients
            document.select("#ingrédients + ul li").forEach { element ->
                val ingredient = element.text()
                ingredients.add(ingredient)
            }

            // Extract equipment
            document.select("#équipement + ul li").forEach { element ->
                val item = element.text()
                equipment.add(item)
            }

            val steps = mutableListOf<String>()
            document.select("ol li").forEach { element ->
                val step = element.text()
                steps.add(step)
            }

            recipe = recipe.copy(title = name, cookingTime = duration, ingredients = ingredients, steps = steps, tags = tags, image = imageUrl)
        } catch (error: Exception) {
            Log.e("PlanEat", error.toString())
        }
        return recipe
    }
}