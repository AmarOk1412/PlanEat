package com.planeat.planeat.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "agenda",
    indices = [Index("id")]
)
data class Agenda(
    @ColumnInfo(name = "date") val date: Long,
    @ColumnInfo(name = "recipe_id") val recipeId: Long
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var agendaId: Long = 0
}
