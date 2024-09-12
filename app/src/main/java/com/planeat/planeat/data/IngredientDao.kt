package com.planeat.planeat.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface IngredientDao {
    @Query("SELECT * FROM ingredients")
    fun getAll(): List<Ingredient>

    @Query("SELECT * FROM ingredients WHERE name IN (:name)")
    fun findByName(name: String): Ingredient

    @Query("SELECT * FROM ingredients")
    fun selectAll(): List<Ingredient>

    @Insert
    fun insertAll(vararg ingredient: Ingredient)

    @Update
    fun update(ingredient: Ingredient)

    @Delete
    fun delete(ingredient: Ingredient)
}