package com.planeat.planeat.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Agenda::class, Recipe::class], version = 1)
abstract class AgendaDb : RoomDatabase() {
    abstract fun agendaDao(): AgendaDao
}
