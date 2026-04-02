package com.example.englishpractice.feature.progress

import com.example.englishpractice.domain.model.SkillType

data class SkillProgressInput(
    val skill: SkillType,
    val completedActivities: Int,
    val targetActivities: Int,
    val averageScore: Int,
    val weakTags: List<String>
)

data class SkillProgressSnapshot(
    val skill: SkillType,
    val completionPercent: Int,
    val averageScore: Int,
    val weakTags: List<String>
)
