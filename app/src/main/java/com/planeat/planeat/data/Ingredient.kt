package com.planeat.planeat.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(
    tableName = "ingredients",
    indices = [Index("id"), Index(value = ["name"], unique = true)]
)
@TypeConverters(Converters::class)
data class Ingredient(
    @ColumnInfo(name = "name") var name: String = "",
    @ColumnInfo(name = "icon") var icon: Int = 0,
    @ColumnInfo(name = "category") var category: String = ""
)  {
    init {
        name = name.lowercase()
    }
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var recipeId: Long = 0
}
