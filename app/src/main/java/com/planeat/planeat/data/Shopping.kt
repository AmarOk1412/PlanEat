
import android.content.Context
import com.planeat.planeat.data.Agenda
import com.planeat.planeat.data.IngredientItem
import com.planeat.planeat.data.IngredientsDb
import com.planeat.planeat.data.Recipe
import com.planeat.planeat.data.RecipesDb
import com.planeat.planeat.data.toIngredientCategory
import com.planeat.planeat.ui.utils.IngredientClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.FileInputStream
import java.io.InputStreamReader

@Serializable
class ShoppingIngredient(
    val ingredient: IngredientItem = IngredientItem(),
    var recipeId: Long = 0,
    var validated: Boolean = false,
) {
    fun toJson(): JSONObject {
        val result = JSONObject()
        result.put("ingredient", ingredient.toJson())
        result.put("recipeId", recipeId)
        result.put("validated", validated)
        return result
    }

    fun fromJson(data: JSONObject) {
        ingredient.fromJson(data.getJSONObject("ingredient"))
        recipeId = data.getLong("recipeId")
        validated = data.getBoolean("validated")
    }

    fun copy(): ShoppingIngredient {
        return ShoppingIngredient(
            ingredient = ingredient.copy(),
            recipeId = recipeId,
            validated = validated
        )
    }
}

class ShoppingList(
    private val planned: List<Agenda>,
    private val rdb: RecipesDb,
    private val ingredientsDb: IngredientsDb,
    private val ic: IngredientClassifier,
    val context: Context
) {


    private val lock = Any()

    @Volatile
    private var _shoppingList: MutableList<ShoppingIngredient> = mutableListOf()
    var shoppingList: List<ShoppingIngredient>
        get() = _shoppingList
        private set(value) {
            _shoppingList = value.toMutableList()
        }

    @Volatile
    private var _jsonObject: JSONObject = JSONObject()
    var jsonObject: JSONObject
        get() = _jsonObject
        private set(value) {
            _jsonObject = value
        }

    var sortingMethod: String = "Aisle"

    @Volatile
    var plannedRecipes: List<Recipe> = emptyList()

    private fun loadShoppingJson(): JSONObject? {
        return try {
            // Open the file from internal storage
            val fileInputStream: FileInputStream = context.openFileInput("shoppingList.json")
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

    private fun jsonToLongList(jsonObject: JSONObject, key: String): List<Long> {
        val jsonArray: JSONArray = try {
            jsonObject.getJSONArray(key)
        } catch (e: Exception) {
            e.printStackTrace()
            JSONArray() // Return an empty JSONArray if there is an issue
        } catch (e: JSONException) {
            JSONArray()
        }

        // Convert JSONArray to List<Long>
        val list = mutableListOf<Long>()
        for (i in 0 until jsonArray.length()) {
            try {
                val longValue = jsonArray.getLong(i)
                list.add(longValue)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return list
    }

    private fun jsonToShoppingList(jsonObject: JSONObject, key: String): MutableList<ShoppingIngredient> {
        val jsonArray: JSONArray = try {
            jsonObject.getJSONArray(key)
        } catch (e: Exception) {
            e.printStackTrace()
            JSONArray() // Return an empty JSONArray if there is an issue
        } catch (e: JSONException) {
            JSONArray()
        }

        // Convert JSONArray to List<Long>
        val list = mutableListOf<ShoppingIngredient>()
        for (i in 0 until jsonArray.length()) {
            try {
                val obj = jsonArray.getJSONObject(i)
                val item = ShoppingIngredient()
                item.fromJson(obj)
                list.add(item)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return list
    }

    @Synchronized
    private fun writeToDisk(): Boolean {
        return try {
            // Open a file in the app's internal storage in MODE_PRIVATE (which overwrites existing files)
            context.openFileOutput("shoppingList.json", Context.MODE_PRIVATE).use { outputStream ->
                // Convert the JSONObject to a string and write it to the file
                outputStream.write(jsonObject.toString().toByteArray())
            }
            true // Return true to indicate successful write
        } catch (e: Exception) {
            e.printStackTrace()
            false // Return false if an error occurs
        }
    }

    suspend fun saveLoadFromDisk() {
        var shoppingJson = loadShoppingJson()
        if (shoppingJson == null) {
            shoppingJson = JSONObject()
            shoppingJson.put("planned", JSONArray())
            shoppingJson.put("shoppingList", JSONArray())
            shoppingJson.put("sortingMethod", sortingMethod)
        }
        sortingMethod = shoppingJson.getString("sortingMethod")

        val currentPlanned: List<Long> = jsonToLongList(shoppingJson, "planned")
        shoppingList = jsonToShoppingList(shoppingJson, "shoppingList")

        var resetList = false
        val plannedId: MutableList<Long> = mutableListOf()
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
        resetList = true || resetList || (planned.size != currentPlanned.size)
        if (!resetList) {
            // Nothing to do
            for (it in shoppingList) {
                if (it.ingredient.checked)
                    it.validated = true
            }

            jsonObject = shoppingJson
        } else {
            // Else initialize!
            shoppingList = mutableListOf()

            // Initialize ingredientsPerCategory and ingredientsPerRecipes
            plannedRecipes.forEach { recipe ->
                recipe.parsed_ingredients.forEach {
                    withContext(Dispatchers.IO) {
                        if (it.category.isEmpty()) {
                            it.category = toIngredientCategory(it.name, ic, ingredientsDb)
                        }
                        shoppingList += ShoppingIngredient(it, recipe.recipeId)
                    }
                }
            }

            shoppingJson.put("planned", JSONArray(plannedId))
            val arrayObj = JSONArray()
            for (ingredient in shoppingList) {
                arrayObj.put(ingredient.toJson())
            }
            shoppingJson.put("shoppingList", arrayObj)
            jsonObject = shoppingJson
            writeToDisk()
        }
    }

    @Synchronized
    fun changeSortingMethod(method: String) {
        synchronized(lock) {
            sortingMethod = method
            jsonObject.put("sortingMethod", sortingMethod)
            if (method == "Aisle") {
                shoppingList = shoppingList.sortedWith(compareBy { it.ingredient.category }).toMutableList()
            } else {
                shoppingList = shoppingList.sortedWith(compareBy { it.recipeId }).toMutableList()
            }
            writeToDisk()
        }
    }

    @Synchronized
    fun addValidated(ingredient: IngredientItem) {
        synchronized(lock) {
            shoppingList.forEach {
                if (it.ingredient.checked) {
                    it.validated = true
                }
            }

            val arrayObj = JSONArray()
            for (ingredient in shoppingList) {
                arrayObj.put(ingredient.toJson())
            }
            jsonObject.put("shoppingList", arrayObj)
            writeToDisk()
        }
    }

    @Synchronized
    fun addIngredient(it: IngredientItem) {
        synchronized(lock) {
            val key = it.name.lowercase()
            if (key.isEmpty()) return
            if (it.category.isEmpty()) {
                it.category = toIngredientCategory(ingredientName = it.name, ic, ingredientsDb)
            }
            shoppingList += ShoppingIngredient(it, 0)

            val arrayObj = JSONArray()
            for (ingredient in shoppingList) {
                arrayObj.put(ingredient.toJson())
            }
            jsonObject.put("shoppingList", arrayObj)
            writeToDisk()
        }
    }

    @Synchronized
    fun checkIngredient(ingredientName: String, newValue: Boolean) {
        synchronized(lock) {
            shoppingList.forEach {
                if (it.ingredient.name == ingredientName) {
                    if (newValue) {
                        // Avoid weird UI
                        if (it.ingredient.checked && !it.validated)
                            it.validated = true
                    }
                    it.ingredient.checked = newValue
                    if (!newValue) {
                        it.validated = false
                    }
                }
            }
            val arrayObj = JSONArray()
            for (ingredient in shoppingList) {
                arrayObj.put(ingredient.toJson())
            }
            jsonObject.put("shoppingList", arrayObj)
            writeToDisk()
        }
    }

    fun copy(): ShoppingList {
        // Create a new ShoppingList instance
        return ShoppingList(
            planned = planned.toList(),
            rdb = rdb,
            ingredientsDb = ingredientsDb,
            ic = ic,
            context = context
        ).apply {
            sortingMethod = sortingMethod
            jsonObject = jsonObject
            shoppingList = shoppingList.toMutableList()
            plannedRecipes = plannedRecipes.toList()
        }
    }

    fun plannedRecipesSize(): Int {
        return try {
            jsonObject.getJSONArray("planned").length()
        } catch (e: Exception) {
            0
        }
    }

    fun countUniqueIngredientNames(): Int {
        // Use a Set to track unique names
        return shoppingList.map { it.ingredient.name }.toSet().size
    }
}
