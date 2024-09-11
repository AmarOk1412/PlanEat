package com.planeat.planeat.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Ingredient::class], version = 2,
          exportSchema = true // Ensure that the schema is exported
)
abstract class IngredientsDb : RoomDatabase() {
    abstract fun ingredientDao(): IngredientDao

    companion object {
        @Volatile
        private var INSTANCE: IngredientsDb? = null

        fun getDatabase(context: Context): IngredientsDb {
            val MIGRATION_1_2 = object : Migration(1, 2) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    // Add the new 'category' column with a default value, e.g., an empty string
                    database.execSQL("ALTER TABLE ingredients ADD COLUMN category TEXT NOT NULL DEFAULT ''")
                }
            }

            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    IngredientsDb::class.java,
                    "ingredients_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}