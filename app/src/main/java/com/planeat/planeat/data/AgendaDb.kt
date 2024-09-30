package com.planeat.planeat.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Agenda::class], version = 2, exportSchema = true,
autoMigrations = [
  AutoMigration (from = 1, to = 2)
]
)
abstract class AgendaDb : RoomDatabase() {
    abstract fun agendaDao(): AgendaDao

    companion object {
        @Volatile
        private var INSTANCE: AgendaDb? = null

        fun getDatabase(context: Context): AgendaDb {
            return INSTANCE ?: synchronized(this) {
                val instance = Room
                    .databaseBuilder(
                        context,
                        AgendaDb::class.java, "AgendaDb"
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

}
