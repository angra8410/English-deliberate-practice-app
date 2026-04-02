package com.example.englishpractice.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attempts")
data class AttemptEntity(
    @PrimaryKey val id: String,
    val activityId: String,
    val answerText: String,
    val transcriptText: String? = null,
    val score: Int? = null,
    val submittedAt: Long
)
