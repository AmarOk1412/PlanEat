package com.planeat.planeat.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Recipe::class], version = 1)
abstract class RecipesDb : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
}
