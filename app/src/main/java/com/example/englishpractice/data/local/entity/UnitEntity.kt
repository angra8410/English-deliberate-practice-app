package com.example.englishpractice.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "units")
data class UnitEntity(
    @PrimaryKey val id: String,
    val title: String,
    val level: String,
    val skill: String,
    val description: String
)
