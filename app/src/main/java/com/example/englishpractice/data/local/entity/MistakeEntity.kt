package com.example.englishpractice.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mistakes")
data class MistakeEntity(
    @PrimaryKey val id: String,
    val attemptId: String,
    val tag: String,
    val note: String,
    val severity: Int,
    val createdAt: Long
)
