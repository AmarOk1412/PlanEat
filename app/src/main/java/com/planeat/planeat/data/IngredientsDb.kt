package com.planeat.planeat.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Ingredient::class], version = 1,
          exportSchema = true // Ensure that the schema is exported
)
abstract class IngredientsDb : RoomDatabase() {
    abstract fun ingredientDao(): IngredientDao

    companion object {
        @Volatile
        private var INSTANCE: IngredientsDb? = null

        fun getDatabase(context: Context): IngredientsDb {

            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    IngredientsDb::class.java,
                    "ingredients_database"
                )
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}