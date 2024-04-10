package com.planeat.planeat.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Agenda::class, Recipe::class], version = 1)
abstract class AgendaDb : RoomDatabase() {
    abstract fun agendaDao(): AgendaDao
}
