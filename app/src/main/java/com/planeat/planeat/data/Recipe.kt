package com.planeat.planeat.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters


class Converters {
    @TypeConverter
    fun fromString(data: String): List<String> {
        return listOf(*data.split(",").map { it }.toTypedArray())
    }

    @TypeConverter
    fun toListString(list: List<String>): String {
        return list.joinToString(",")
    }
}

@Entity(
    tableName = "recipes",
    indices = [Index("id")]
)
@TypeConverters(Converters::class)
data class Recipe(
    @ColumnInfo(name = "title") val title: String = "",
    @ColumnInfo(name = "url") val url: String = "",
    @ColumnInfo(name = "image") val image: String = "",
    @ColumnInfo(name = "kindOfMeal") val kindOfMeal: String = "",
    @ColumnInfo(name = "cookingTime") val cookingTime: Int = 0,
    @ColumnInfo(name = "season") val season: String = "",
    @ColumnInfo(name = "tags") val tags: List<String> = emptyList(),
    @ColumnInfo(name = "ingredients") val ingredients: List<String> = emptyList(),
    @ColumnInfo(name = "steps") val steps: List<String> = emptyList()
)  {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var recipeId: Long = 0
}
