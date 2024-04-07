package com.planeat.planeat.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes")
    fun getAll(): List<Recipe>

    @Query("SELECT * FROM recipes WHERE id IN (:recipesId)")
    fun loadAllByIds(recipesIds: IntArray): List<Recipe>

    @Insert
    fun insertAll(vararg recipe: Recipe)

    @Delete
    fun delete(recipe: Recipe)
}
