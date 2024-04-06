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

@Throws(JSONException::class)
fun JSONObject.toMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    val keysItr: Iterator<String> = this.keys()
    while (keysItr.hasNext()) {
        val key = keysItr.next()
        var value: Any = this.get(key)
        when (value) {
            is JSONArray -> value = value.toList()
            is JSONObject -> value = value.toMap()
        }
        map[key] = value
    }
    return map
}

@Throws(JSONException::class)
fun JSONArray.toList(): List<Any> {
    val list = mutableListOf<Any>()
    for (i in 0 until this.length()) {
        var value: Any = this[i]
        when (value) {
            is JSONArray -> value = value.toList()
            is JSONObject -> value = value.toMap()
        }
        list.add(value)
    }
    return list
}

class Marmiton : Connector {
    private var maxResult: Int

    constructor(maxResult: Int) : super() {
        this.maxResult = maxResult
    }

    override fun handleUrl(url: String): Boolean {
        return url.contains("marmiton.org")
    }

    override fun search(searchTerm: String): Unit {
        val recipes = mutableListOf<Recipe>()
        try {
            val searchTermEscaped = URLEncoder.encode(searchTerm, StandardCharsets.UTF_8.toString())
            val url = "https://www.marmiton.org/recettes/recherche.aspx?aqt=$searchTermEscaped"
            val doc: Document = Jsoup.connect(url).get()

            val elements: Elements = doc.select(".recipe-card-algolia")
            for (element in elements) {
                val relurl = element.select(".recipe-card-link").attr("href")
                recipes.add(this.getRecipe(relurl))
                if (recipes.size == this.maxResult)
                    break
            }
        } catch (error: Exception) {
            Log.e("PlanEat", error.toString())
        }
    }

    override fun getRecipe(url: String): Recipe {
        var recipe: Recipe = Recipe()
        Log.d("PlanEat", "Parse recipe from Marmiton: $url")
        try {
            val response = Jsoup.connect(url).execute()
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
                var tags = emptyList<String>()
                if (recipeData.has("recipeCuisine")) {
                    tags = listOf(recipeData.getString("recipeCuisine"))
                }
                if (recipeData.has("keywords")) {
                    tags = recipeData.getString("keywords").split(", ")
                } else if (recipeData.has("recipeCategory")) {
                    tags = recipeData.getString("recipeCategory").split(", ")
                }

                val duration = recipeData.getString("totalTime").replace("\\D".toRegex(), "").toInt()
                val ingredients = recipeData.getJSONArray("recipeIngredient").toList().map { it.toString() }
                val steps = recipeData.getJSONArray("recipeInstructions").toList().map { (it as LinkedHashMap<*, *>).get("text").toString() }
                val imageUrl = recipeData.getJSONArray("image").getString(0)

                recipe = recipe.copy(title = name, image = imageUrl, tags = tags, cookingTime = duration, ingredients = ingredients, steps = steps)
            }

        } catch (error: Exception) {
            Log.e("PlanEat", error.toString())
        }
        return recipe
    }
}