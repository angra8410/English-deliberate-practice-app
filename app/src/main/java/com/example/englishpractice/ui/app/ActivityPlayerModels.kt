package com.example.englishpractice.ui.app

import com.example.englishpractice.domain.model.ExerciseType
import com.example.englishpractice.domain.model.SkillType

data class PracticeActivityItem(
    val id: String,
    val skill: SkillType,
    val title: String,
    val instructions: String,
    val prompt: String,
    val exerciseType: ExerciseType,
    val starterText: String = "",
    val modelAnswer: String,
    val evaluationTargets: List<String>,
    val supportNote: String
)

data class ActivityAttemptRecord(
    val activityId: String,
    val skill: SkillType,
    val submittedAnswer: String,
    val transcriptText: String? = null,
    val score: Int,
    val feedback: List<String>,
    val weakTags: List<String>
)

data class ActivitySubmissionResult(
    val activityId: String,
    val score: Int,
    val feedback: List<String>,
    val weakTags: List<String>,
    val savedTranscript: String? = null
)
