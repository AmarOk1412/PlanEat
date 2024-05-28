package com.planeat.planeat.connectors

import android.util.Log
import com.planeat.planeat.data.Recipe
import org.json.JSONObject

import org.jsoup.Jsoup
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class Ricardo : Connector {
    private var maxResult: Int

    constructor(maxResult: Int) : super() {
        this.maxResult = maxResult
    }

    override fun handleUrl(url: String): Boolean {
        return url.contains("ricardocuisine.com")
    }

    fun getRouteProps(scriptContent: String): JSONObject {
        val startPos = scriptContent.indexOf("routeProps:") + "routeProps:".length
        val braceStack = mutableListOf<Char>()
        var endPos: Int = 0

        for (i in startPos until scriptContent.length) {
            if (scriptContent[i] == '{') {
                braceStack.add('{')
            } else if (scriptContent[i] == '}') {
                if (braceStack.isEmpty()) {
                    throw Error("Invalid scriptContent: unexpected }")
                }
                braceStack.removeLast()
                if (braceStack.isEmpty()) {
                    endPos = i
                    break
                }
            }
        }

        if (braceStack.isNotEmpty()) {
            throw Error("Invalid scriptContent: unmatched {")
        }

        val routePropsStr = scriptContent.substring(startPos, endPos + 1)
        val routeProps = JSONObject(routePropsStr)

        return routeProps
    }

    override fun search(searchTerm: String, onRecipe: (Recipe) -> Unit) {
        var r = 0
        try {
            val searchTermEscaped = URLEncoder.encode(searchTerm, StandardCharsets.UTF_8.toString())
            val url = "https://www.ricardocuisine.com/recherche?sort=score&searchValue=$searchTermEscaped&content-type=recipe&currentPage=1"
            val response = Jsoup.connect(url).execute()
            val document = response.parse()

            val scriptContent = document.select("script[id=\"react-bridge-bootstrap\"]").html()
            val data = getRouteProps(scriptContent)

            if (data["status"] == "success") {
                val rows = data.getJSONObject("content").getJSONObject("results").getJSONArray("rows")
                for (i in 0 until rows.length()) {
                    val row = rows.getJSONObject(i)
                    val rowUrl = row.getString("url")
                    val recipeUrl = "https://www.ricardocuisine.com/recettes/$rowUrl"
                    val recipe = getRecipe(recipeUrl)
                    if (recipe.title.isEmpty())
                        continue
                    onRecipe(recipe)
                    if (r == this.maxResult)
                        break
                    r++
                }
            }
        } catch (error: Exception) {
            Log.e("PlanEat", error.toString())
        }
    }

    override fun getRecipe(url: String): Recipe {
        var recipe: Recipe = Recipe()
        Log.d("PlanEat", "Parse recipe from Ricardo: $url")
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
                val ingredients = recipeData.optJSONArray("recipeIngredient")?.toList()?.map { it.toString() } ?: emptyList()
                val imageUrl = recipeData.getJSONArray("image").getString(0)
                val steps = recipeData.optJSONArray("recipeInstructions")?.toList()?.map { (it as LinkedHashMap<*, *>).get("text").toString() } ?: emptyList()

                recipe = recipe.copy(title = name, url = url, image = imageUrl, tags = tags, cookingTime = duration, ingredients = ingredients, steps = steps)
            }
        } catch (error: Exception) {
            Log.e("PlanEat", error.toString())
        }
        return recipe
    }
}