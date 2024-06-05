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
    indices = [Index("id"), Index(value = ["url"], unique = true)]
)
@TypeConverters(Converters::class)
data class Recipe(
    @ColumnInfo(name = "title") var title: String = "",
    @ColumnInfo(name = "url") var url: String = "",
    @ColumnInfo(name = "image") var image: String = "",
    @ColumnInfo(name = "kindOfMeal") var kindOfMeal: String = "",
    @ColumnInfo(name = "cookingTime") var cookingTime: Int = 0,
    @ColumnInfo(name = "season") var season: String = "",
    @ColumnInfo(name = "tags") var tags: List<String> = emptyList(),
    @ColumnInfo(name = "ingredients") var ingredients: List<String> = emptyList(),
    @ColumnInfo(name = "steps") var steps: List<String> = emptyList()
)  {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var recipeId: Long = 0
}
