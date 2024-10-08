package com.planeat.planeat.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Recipe::class], version = 2,
          exportSchema = true // Ensure that the schema is exported
)
abstract class RecipesDb : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao

    companion object {
        @Volatile
        private var INSTANCE: RecipesDb? = null

        fun getDatabase(context: Context): RecipesDb {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RecipesDb::class.java,
                    "recipes_database"
                )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        // Define the migration from version 1 to version 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add the new columns 'edited' and 'favorite' to the existing table
                database.execSQL("ALTER TABLE recipes ADD COLUMN edited INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE recipes ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0")

                // Set the new fields to true (1 in SQLite for BOOLEAN)
                database.execSQL("UPDATE recipes SET favorite = 1")
            }
        }
    }
}