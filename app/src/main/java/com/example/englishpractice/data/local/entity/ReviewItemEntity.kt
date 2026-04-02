package com.example.englishpractice.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "review_items")
data class ReviewItemEntity(
    @PrimaryKey val id: String,
    val activityId: String,
    val sourceMistakeTag: String,
    val dueAt: Long,
    val intervalDays: Int
)
