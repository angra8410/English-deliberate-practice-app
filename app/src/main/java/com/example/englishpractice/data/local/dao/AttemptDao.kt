package com.example.englishpractice.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.englishpractice.data.local.entity.AttemptEntity

@Dao
interface AttemptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: AttemptEntity)

    @Query("SELECT * FROM attempts ORDER BY submittedAt DESC")
    suspend fun getAllByNewest(): List<AttemptEntity>

    @Query("SELECT * FROM attempts WHERE activityId = :activityId ORDER BY submittedAt DESC LIMIT 1")
    suspend fun getLatestByActivity(activityId: String): AttemptEntity?

    @Query("SELECT COUNT(*) FROM attempts WHERE activityId = :activityId")
    suspend fun countByActivity(activityId: String): Int

    @Query("DELETE FROM attempts WHERE activityId = :activityId")
    suspend fun deleteByActivity(activityId: String)
}
