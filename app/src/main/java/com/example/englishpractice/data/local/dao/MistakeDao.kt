package com.example.englishpractice.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.englishpractice.data.local.entity.MistakeEntity

@Dao
interface MistakeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MistakeEntity>)

    @Query("SELECT * FROM mistakes ORDER BY createdAt DESC")
    suspend fun getAllByNewest(): List<MistakeEntity>

    @Query("SELECT * FROM mistakes WHERE attemptId = :attemptId")
    suspend fun getByAttempt(attemptId: String): List<MistakeEntity>

    @Query("DELETE FROM mistakes WHERE attemptId IN (SELECT id FROM attempts WHERE activityId = :activityId)")
    suspend fun deleteByActivity(activityId: String)
}
