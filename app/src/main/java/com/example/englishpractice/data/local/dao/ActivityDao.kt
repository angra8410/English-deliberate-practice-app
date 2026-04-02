package com.example.englishpractice.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.englishpractice.data.local.entity.ActivityEntity

@Dao
interface ActivityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ActivityEntity>)

    @Query("SELECT * FROM activities WHERE level = :level AND skill = :skill")
    suspend fun getByLevelAndSkill(level: String, skill: String): List<ActivityEntity>
}
