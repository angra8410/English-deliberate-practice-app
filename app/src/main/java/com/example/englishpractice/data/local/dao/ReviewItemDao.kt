package com.example.englishpractice.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.englishpractice.data.local.entity.ReviewItemEntity

@Dao
interface ReviewItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ReviewItemEntity)

    @Query("SELECT * FROM review_items ORDER BY dueAt ASC")
    suspend fun getAllByDueAt(): List<ReviewItemEntity>

    @Query("SELECT * FROM review_items WHERE activityId = :activityId ORDER BY dueAt DESC LIMIT 1")
    suspend fun getLatestByActivity(activityId: String): ReviewItemEntity?
}
