package com.example.englishpractice.ui.app

import com.example.englishpractice.domain.model.ExerciseType
import com.example.englishpractice.domain.model.SkillType

enum class PromptScoringProfile {
    DEFAULT,
    LIST,
    SENTENCE_DRILL,
    REWRITE
}

data class PracticeActivityItem(
    val id: String,
    val unitId: String? = null,
    val skill: SkillType,
    val title: String,
    val instructions: String,
    val prompt: String,
    val exerciseType: ExerciseType,
    val starterText: String = "",
    val audioAssetPath: String? = null,
    val listeningPromptText: String? = null,
    val modelAnswer: String,
    val evaluationTargets: List<String>,
    val supportNote: String,
    val scoringProfile: PromptScoringProfile = PromptScoringProfile.DEFAULT,
    val minimumWordCount: Int? = null,
    val minimumResponseItems: Int? = null,
    val minimumKeywordMatches: Int? = null,
    val requiresToneReference: Boolean? = null,
    val requiresContrastMarker: Boolean? = null
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
