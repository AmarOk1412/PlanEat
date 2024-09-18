package com.planeat.planeat.connectors

import android.util.Log
import com.planeat.planeat.data.Recipe

import org.jsoup.Jsoup

class ChaCuit : Connector {
    private var maxResult: Int

    constructor(maxResult: Int) : super() {
        this.maxResult = maxResult
    }

    override fun handleUrl(url: String): Boolean {
        return url.contains("cha-cu.it") || url.contains("re7.ache.one")
    }

    override fun parsePages(url: String, onRecipe: (Recipe) -> Unit) { }

    override fun search(searchTerm: String, onRecipe: (Recipe) -> Unit) {
        var i = 0
        try {
            val url = "https://cha-cu.it/recettes"
            val response = Jsoup.connect(url).execute()
            val document = response.parse()

            val elements = document.select(".p-2")
            for (element in elements) {
                val recipeUrl = element.select("a").attr("href")
                val title = element.select(".my-2.text-xl.font-semibold").text()

                if (title.lowercase().contains(searchTerm.lowercase())) {
                    val recipe = getRecipe(recipeUrl)
                    if (recipe.title.isEmpty())
                        continue
                    onRecipe(recipe)
                    if (i == this.maxResult)
                        break
                    i++
                }
            }
        } catch (error: Exception) {
            Log.e("PlanEat", error.toString())
        }
    }

    override fun suggest(onRecipe: (Recipe) -> Unit) {
        var i = 0
        try {
            val url = "https://cha-cu.it/"
            val response = Jsoup.connect(url).execute()
            val document = response.parse()

            val elements = document.select(".p-2")
            for (element in elements) {
                val recipeUrl = element.select("a").attr("href")

                val recipe = getRecipe(recipeUrl)
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


    override fun getRecipe(url: String): Recipe {
        var recipe: Recipe = Recipe()
        Log.d("PlanEat", "Parse recipe from cha-cu.it: $url")
        try {
            val response = Jsoup.connect(url).execute()
            val document = response.parse()

            val name = document.select("meta[property=\"og:title\"]").attr("content")

            val durationText = document.text()
            var durationMatch = Regex("Temps de préparation : (\\d+) *min").find(durationText)
            var duration = durationMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
            if (duration == 0) {
                durationMatch = Regex("Temps de préparation : (\\d+)h *(\\d+)?").find(durationText)
                val hours = durationMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val minutes = durationMatch?.groupValues?.get(2)?.toIntOrNull() ?: 0
                duration = hours * 60 + minutes
            }
            val keywordsMeta = document.select("meta[itemprop=\"keywords\"]").attr("content")
            val tags = keywordsMeta.split(',')

            val firstImage = document.select(".rounded-lg.shadow-sm.w-full.object-contain").attr("src")
            val imageUrl = "https://cha-cu.it$firstImage"

            val ingredients = mutableListOf<String>()
            val equipment = mutableListOf<String>()

            val l = document.select("#ingrédients + ul li")
            // Extract ingredients
            val ingredientElements = document.select("#ingrédients + ul li")
            if (ingredientElements.isEmpty()) {
                // Check for optional h2 elements
                val optionalH2Elements = document.select("#ingrédients + h2")
                for (h2Element in optionalH2Elements) {
                    val nextUlElement = h2Element.nextElementSibling()
                    if (nextUlElement.tagName() == "ul") {
                        ingredientElements.addAll(nextUlElement.select("li"))
                    }
                }
            }
            ingredientElements.forEach { element ->
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

            recipe = recipe.copy(title = name, url = url, cookingTime = duration, ingredients = ingredients, steps = steps, tags = tags, image = imageUrl)
        } catch (error: Exception) {
            Log.e("PlanEat", error.toString())
        }
        return recipe
    }
}