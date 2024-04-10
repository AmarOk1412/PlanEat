package com.planeat.planeat.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes")
    fun getAll(): List<Recipe>

    @Query("SELECT * FROM recipes WHERE id IN (:id)")
    fun findById(id: Long): Recipe

    @Query("SELECT * FROM recipes WHERE url IN (:url)")
    fun findByUrl(url: String): Recipe

    @Query("SELECT * FROM recipes WHERE title IN (:title)")
    fun findByTitle(title: String): Recipe

    @Insert
    fun insertAll(vararg recipe: Recipe)

    @Delete
    fun delete(recipe: Recipe)
}
