package com.planeat.planeat.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Recipe::class], version = 1)
abstract class RecipesDb : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao

    companion object {

        // For Singleton instantiation
        @Volatile private var instance: RecipesDb? = null

        fun getInstance(context: Context): RecipesDb {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        // Create and pre-populate the database. See this article for more details:
        // https://medium.com/google-developers/7-pro-tips-for-room-fbadea4bfbd1#4785
        private fun buildDatabase(context: Context): RecipesDb {
            return Room.databaseBuilder(context, RecipesDb::class.java, "recipesDb")
                .addCallback(
                    object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            Log.d("PlanEat", "populating with data...")
                            // TODO
                        }
                    }
                )
                .build()
        }
    }

}
