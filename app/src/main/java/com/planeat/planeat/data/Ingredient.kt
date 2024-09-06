package com.planeat.planeat.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Entity(
    tableName = "ingredients",
    indices = [Index("id"), Index(value = ["name"], unique = true)]
)
@TypeConverters(Converters::class)
data class Ingredient(
    @ColumnInfo(name = "name") var name: String = "",
    @ColumnInfo(name = "icon") var icon: Int = 0,
)  {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var recipeId: Long = 0
}
