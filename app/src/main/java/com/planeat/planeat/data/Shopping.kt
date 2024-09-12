package com.planeat.planeat.data

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.tensorflow.lite.examples.textclassification.client.IngredientClassifier
import java.io.FileInputStream
import java.io.InputStreamReader

class ShoppingList(val planned: List<Agenda>, val rdb: RecipesDb, val ingredientsDb: IngredientsDb, val ic: IngredientClassifier, val context: Context) {
    var ingredientsPerCategory: MutableMap<String, MutableMap<String, IngredientItem>> = mutableMapOf()
    var ingredientsPerRecipes: MutableMap<Long, MutableMap<String, IngredientItem>> = mutableMapOf()
    var customIngredients: MutableMap<String, IngredientItem> = mutableMapOf()
    var sortingMethod: String = "Aisle"

    var plannedRecipes: List<Recipe> = emptyList()
    var plannedId: MutableList<Long> = mutableListOf()
    var checkedIngredients: List<String> = emptyList()

    private fun jsonToLongList(jsonObject: JSONObject, key: String): List<Long> {
        val jsonArray: JSONArray = try {
            // Get the value associated with the key
            val value = jsonObject.get(key)

            // Check if the value is a String that needs to be parsed
            if (value is String) {
                // Parse the string into a JSONArray
                JSONArray(value)
            } else {
                // Otherwise, treat it as a JSONArray
                value as JSONArray
            }
        } catch (e: JSONException) {
            e.printStackTrace()
            JSONArray() // Return an empty JSONArray if there is an issue
        } catch (e: Exception) {
            JSONArray() // Return an empty JSONArray if there is an issue
        }

        // Convert JSONArray to List<Long>
        val list = mutableListOf<Long>()
        for (i in 0 until jsonArray.length()) {
            try {
                val longValue = jsonArray.getLong(i)
                list.add(longValue)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        return list
    }

    private fun jsonToStringList(jsonObject: JSONObject, key: String): List<String> {
        val jsonArray: JSONArray = try {
            // Get the value associated with the key
            val value = jsonObject.get(key)

            // Check if the value is a String
            if (value is String) {
                // Try parsing the string as a JSONArray
                try {
                    JSONArray(value)
                } catch (e: JSONException) {
                    // If parsing fails, treat it as a single string element in a JSONArray
                    JSONArray().put(value)
                }
            } else {
                // Otherwise, treat it as a JSONArray
                value as JSONArray
            }
        } catch (e: JSONException) {
            e.printStackTrace()
            JSONArray() // Return an empty JSONArray if there is an issue
        }

        // Convert JSONArray to List<String>
        val list = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            try {
                val stringValue = jsonArray.getString(i)
                list.add(stringValue)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        return list
    }

    private fun mapToCustomIngredients(ingredientsJson: JSONObject): MutableMap<String, IngredientItem> {
        val customIngredientsMap = mutableMapOf<String, IngredientItem>()

        ingredientsJson.keys().forEach { key ->
            val jsonItem = ingredientsJson.getJSONObject(key)

            // Extract the ingredient data from the JSONObject
            val name = jsonItem.optString("name", "")
            val quantity = jsonItem.optDouble("quantity", 1.0).toFloat()
            val unit = jsonItem.optString("unit", "")
            val category = jsonItem.optString("category", "")
            val checked = jsonItem.optBoolean("checked", checkedIngredients.contains(name))

            // Create a new IngredientItem
            val ingredientItem = IngredientItem(name, quantity, unit, category, checked)

            // Add to the map using the ingredient name as the key
            customIngredientsMap[name] = ingredientItem
        }

        return customIngredientsMap
    }

    fun saveLoadFromDisk() {
        var shoppingJson = loadShoppingJson("shoppingList.json")
        if (shoppingJson == null) {
            shoppingJson = JSONObject()
            shoppingJson.put("planned", JSONArray())
            shoppingJson.put("checkedIngredients", JSONArray())
            shoppingJson.put("customIngredients", customIngredients)
            shoppingJson.put("sortingMethod", sortingMethod)
        }
        sortingMethod = shoppingJson.getString("sortingMethod")

        var resetList = false

        val currentPlanned: List<Long> = jsonToLongList(shoppingJson, "planned")
        checkedIngredients = jsonToStringList(shoppingJson, "checkedIngredients")

        // Initialize plannedRecipes
        planned.forEach { agendaItem ->
            val recipe = rdb.recipeDao().findById(agendaItem.recipeId)
            if (recipe != null) {
                // Update planned recipes and ingredient count in UI state
                plannedRecipes += recipe
                plannedId += recipe.recipeId
                // If a new recipe is detected, reset shopping list
                if (!currentPlanned.contains(recipe.recipeId)) {
                    resetList = true
                }
            }
        }
        resetList = resetList || (planned.size == plannedId.size)
        if (resetList) {
            shoppingJson.put("planned", plannedId)
            shoppingJson.put("checkedIngredients", emptyList<String>())
        }

        // Initialize customIngredients
        Log.e("PlanEat", "$shoppingJson")
        val customIngredientsJson = shoppingJson.opt("customIngredients")
        customIngredients = when (customIngredientsJson) {
            is JSONObject -> mapToCustomIngredients(customIngredientsJson)
            is LinkedHashMap<*, *> -> mapToCustomIngredients(JSONObject(customIngredientsJson as Map<String, Any>))
            else -> mutableMapOf()
        }

        // Initialize ingredientsPerCategory and ingredientsPerRecipes
        plannedRecipes.forEach { recipe ->
            recipe.parsed_ingredients.forEach { item ->
                var ingredient = item
                Log.d("PlanEat", "${item.name} vs ${checkedIngredients}")
                ingredient.checked = checkedIngredients.contains(item.name)
                ingredientsPerRecipes.getOrPut(recipe.recipeId) { mutableMapOf() }[item.name] = ingredient
            }

            val recipeIngredients = recipe.parsed_ingredients.associateBy { it.name }
            recipeIngredients.forEach { (name, ingredient) ->
                val ingredientName = name.lowercase()
                val pluralIngredientName = if (ingredientName.endsWith("s")) ingredientName.dropLast(1) else ingredientName + "s"
                val category = toIngredientCategory(ingredientName, ic, ingredientsDb)

                // Get or create a mutable copy of the category's ingredient map
                val updatedCategoryMap = ingredientsPerCategory.toMutableMap()
                val innerMap = updatedCategoryMap[category]?.toMutableMap() ?: mutableMapOf()

                // Check for singular and plural forms and update quantities accordingly
                val existingIngredientName = when {
                    innerMap.containsKey(ingredientName) -> ingredientName
                    innerMap.containsKey(pluralIngredientName) -> pluralIngredientName
                    else -> null
                }

                if (existingIngredientName != null) {
                    // Get the existing ingredient and update its quantity
                    val oldIngredient = innerMap[existingIngredientName]!!
                    oldIngredient.addQuantity(ingredient)
                    innerMap[existingIngredientName] = oldIngredient
                } else {
                    // If the ingredient doesn't exist, add it to the inner map
                    innerMap[ingredientName] = ingredient
                }

                // Update the outer map with the modified inner map
                updatedCategoryMap[category] = innerMap

                // Update the state with the modified map
                ingredientsPerCategory = updatedCategoryMap
            }
        }

        writeToDisk("shoppingList.json", shoppingJson)
    }

    fun loadShoppingJson(fileName: String): JSONObject? {
        return try {
            // Open the file from internal storage
            val fileInputStream: FileInputStream = context.openFileInput(fileName)
            val inputStreamReader = InputStreamReader(fileInputStream)

            // Read the file into a string
            val content = inputStreamReader.readText()

            // Parse the content into a JSONObject
            JSONObject(content)
        } catch (e: Exception) {
            e.printStackTrace()
            null // Return null if there is an issue
        }
    }

    fun writeToDisk(fileName: String, jsonObject: JSONObject): Boolean {
        return try {
            // Open a file in the app's internal storage in MODE_PRIVATE (which overwrites existing files)
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use { outputStream ->
                // Convert the JSONObject to a string and write it to the file
                outputStream.write(jsonObject.toString().toByteArray())
            }
            true // Return true to indicate successful write
        } catch (e: Exception) {
            e.printStackTrace()
            false // Return false if an error occurs
        }
    }

    fun changeSortingMethod(newValue: String) {
        sortingMethod = newValue
        val shoppingJson = JSONObject()
        shoppingJson.put("planned", plannedId)
        shoppingJson.put("checkedIngredients", JSONArray(checkedIngredients))
        shoppingJson.put("customIngredients", customIngredients)
        shoppingJson.put("sortingMethod", sortingMethod)
        writeToDisk("shoppingList.json", shoppingJson)
    }

    fun checkIngredient(ingredientName: String, newValue: Boolean) {
        // Update the checked status in ingredientsPerCategory
        ingredientsPerCategory.values.forEach { categoryMap ->
            categoryMap[ingredientName]?.let { ingredient ->
                ingredient.checked = newValue
            }
        }

        // Update the checked status in ingredientsPerRecipes
        ingredientsPerRecipes.values.forEach { recipeMap ->
            recipeMap[ingredientName]?.let { ingredient ->
                ingredient.checked = newValue
            }
        }

        // Update the checked status in customIngredients
        customIngredients[ingredientName]?.let { ingredient ->
            ingredient.checked = newValue
        }

        // Update checkedIngredients list
        if (newValue) {
            if (!checkedIngredients.contains(ingredientName)) {
                checkedIngredients = checkedIngredients + ingredientName
            }
        } else {
            checkedIngredients = checkedIngredients.filter { it != ingredientName }
        }

        val shoppingJson = JSONObject()
        shoppingJson.put("planned", JSONArray(plannedId))
        shoppingJson.put("checkedIngredients", JSONArray(checkedIngredients))
        shoppingJson.put("customIngredients", customIngredients)
        shoppingJson.put("sortingMethod", sortingMethod)
        writeToDisk("shoppingList.json", shoppingJson)
    }

    fun copy(): ShoppingList {
        // Deep copy the essential properties
        val copiedPlanned = planned.toList()
        val copiedPlannedRecipes = plannedRecipes.toList()
        val copiedPlannedId = plannedId.toMutableList()
        val copiedCheckedIngredients = checkedIngredients.toList()

        // Deep copy ingredientsPerCategory and ingredientsPerRecipes
        val copiedIngredientsPerCategory = ingredientsPerCategory.mapValues {
            it.value.mapValues { entry -> entry.value.copy() }.toMutableMap()
        }.toMutableMap()

        val copiedIngredientsPerRecipes = ingredientsPerRecipes.mapValues {
            it.value.mapValues { entry -> entry.value.copy() }.toMutableMap()
        }.toMutableMap()

        // Deep copy customIngredients
        val copiedCustomIngredients = customIngredients.mapValues { it.value.copy() }.toMutableMap()

        // Create a new ShoppingList instance
        return ShoppingList(
            planned = copiedPlanned,
            rdb = rdb,
            ingredientsDb = ingredientsDb,
            ic = ic,
            context = context
        ).apply {
            ingredientsPerCategory = copiedIngredientsPerCategory
            ingredientsPerRecipes = copiedIngredientsPerRecipes
            customIngredients = copiedCustomIngredients
            sortingMethod = this@ShoppingList.sortingMethod
            plannedRecipes = copiedPlannedRecipes
            plannedId = copiedPlannedId
            checkedIngredients = copiedCheckedIngredients
        }
    }

    fun addIngredient(it: IngredientItem) {
        val key = it.name.lowercase()
        if (key.isEmpty())
            return
        if (customIngredients.containsKey(key)) {
            customIngredients[key]!!.addQuantity(it)
        } else {
            customIngredients[key] = it
        }
        val shoppingJson = JSONObject()
        shoppingJson.put("planned", JSONArray(plannedId))
        shoppingJson.put("checkedIngredients", JSONArray(checkedIngredients))
        shoppingJson.put("customIngredients", customIngredients)
        shoppingJson.put("sortingMethod", sortingMethod)
        writeToDisk("shoppingList.json", shoppingJson)
    }

}