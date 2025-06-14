package com.planeat.planeat.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface AgendaDao {
    @Query("SELECT * FROM agenda")
    fun getAll(): List<Agenda>

    @Query("SELECT * FROM agenda WHERE date IN (:date)")
    fun findByDate(date: Long): List<Agenda>

    @Query("SELECT * FROM agenda WHERE date BETWEEN :startDate AND :endDate")
    fun findBetweenDates(startDate: Long, endDate: Long): List<Agenda>

    @Transaction
    @Insert
    fun insertAll(agenda: List<Agenda>)

    @Delete
    fun delete(agenda: Agenda)
}
