package com.planeat.planeat.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject

class Converters {
    @TypeConverter
    fun fromString(data: String): List<String> {
        return listOf(*data.split("\n@@@\n").map { it }.toTypedArray())
    }

    @TypeConverter
    fun toListString(list: List<String>): String {
        return list.joinToString("\n@@@\n")
    }


    @TypeConverter
    fun fromIngredientItemList(ingredients: List<IngredientItem>): String {
        return Json.encodeToString(ListSerializer(IngredientItem.serializer()), ingredients)
    }

    @TypeConverter
    fun toIngredientItemList(data: String): List<IngredientItem> {
        return Json.decodeFromString(ListSerializer(IngredientItem.serializer()), data)
    }
}

@Entity(
    tableName = "recipes",
    indices = [Index("id"), Index(value = ["url"], unique = true)]
)
@TypeConverters(Converters::class)
@Serializable
data class Recipe(
    @ColumnInfo(name = "title") var title: String = "",
    @ColumnInfo(name = "url") var url: String = "",
    @ColumnInfo(name = "image") var image: String = "",
    @ColumnInfo(name = "kindOfMeal") var kindOfMeal: String = "",
    @ColumnInfo(name = "cookingTime") var cookingTime: Int = 0,
    @ColumnInfo(name = "planified") var planified: Int = 0,
    @ColumnInfo(name = "season") var season: String = "",
    @ColumnInfo(name = "tags") var tags: List<String> = emptyList(),
    @ColumnInfo(name = "ingredients") var ingredients: List<String> = emptyList(),
    @ColumnInfo(name = "steps") var steps: String = "",
    @ColumnInfo(name = "parsed_ingredients") var parsed_ingredients: List<IngredientItem> = emptyList()
)  {
    fun toSmallJson(): Any {
        val jsonObject = JSONObject()
        jsonObject.put("title", title)
        jsonObject.put("tags", JSONArray(tags))
        jsonObject.put("ingredients", JSONArray(ingredients))
        return jsonObject
    }

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var recipeId: Long = 0
}
