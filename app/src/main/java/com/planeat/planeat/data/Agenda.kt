package com.planeat.planeat.data

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.time.Instant
import java.util.Date

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
