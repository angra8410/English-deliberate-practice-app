package com.example.englishpractice.data.repository

import com.example.englishpractice.domain.model.SkillType
import com.example.englishpractice.feature.progress.SkillProgressInput
import com.example.englishpractice.ui.app.ActivityAttemptRecord
import com.example.englishpractice.ui.app.PracticeActivityItem
import com.example.englishpractice.ui.app.ReviewQueueItem
import com.example.englishpractice.ui.app.WeakPattern

data class PersistedSubmission(
    val activity: PracticeActivityItem,
    val skill: SkillType,
    val answer: String,
    val transcriptText: String?,
    val score: Int,
    val feedback: List<String>,
    val weakTags: List<String>
)

data class PersistedAppSnapshot(
    val recentAttempts: List<ActivityAttemptRecord>,
    val weakPatterns: List<WeakPattern>,
    val reviewQueue: List<ReviewQueueItem>,
    val skillProgressInputs: List<SkillProgressInput>
)
