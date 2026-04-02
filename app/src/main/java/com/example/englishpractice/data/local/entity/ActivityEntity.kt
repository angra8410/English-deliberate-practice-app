package com.example.englishpractice.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activities")
data class ActivityEntity(
    @PrimaryKey val id: String,
    val unitId: String,
    val title: String,
    val level: String,
    val skill: String,
    val exerciseType: String,
    val prompt: String,
    val instructions: String,
    val tagsCsv: String,
    val difficulty: Int,
    val sampleAnswer: String? = null,
    val expectedAnswer: String? = null,
    val audioAsset: String? = null
)
