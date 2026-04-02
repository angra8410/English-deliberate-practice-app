package com.example.englishpractice.ui.app

import com.example.englishpractice.domain.model.CefrLevel
import com.example.englishpractice.domain.model.ExerciseType
import com.example.englishpractice.domain.model.SkillType
import com.example.englishpractice.feature.listening.ListeningCapability
import com.example.englishpractice.feature.progress.SkillProgressSnapshot
import com.example.englishpractice.feature.speaking.SpeakingCapability

data class AppUiState(
    val currentLevel: CefrLevel,
    val targetLevel: CefrLevel,
    val streakDays: Int,
    val dailyGoalMinutes: Int,
    val pilotLevels: List<CefrLevel>,
    val overallCompletion: Int,
    val dailyPlan: List<DailyPracticeItem>,
    val skillProgress: List<SkillProgressSnapshot>,
    val weakPatterns: List<WeakPattern>,
    val reviewSummary: ReviewSummary,
    val reviewQueue: List<ReviewQueueItem>,
    val activityCatalog: List<PracticeActivityItem>,
    val contentBrowserItems: List<ContentBrowserItem>,
    val recentAttempts: List<ActivityAttemptRecord>,
    val selectedSpeakingLocaleTag: String,
    val speakingCapability: SpeakingCapability,
    val listeningCapability: ListeningCapability
)

data class DailyPracticeItem(
    val skill: SkillType,
    val title: String,
    val focus: String,
    val exerciseType: ExerciseType,
    val estimatedMinutes: Int,
    val sourceLabel: String = "Built-in assets"
)

data class WeakPattern(
    val skill: SkillType,
    val tag: String,
    val note: String
)

data class ReviewSummary(
    val dueToday: Int,
    val recurringPatterns: Int,
    val nextCheckpointDays: Int
)

data class ReviewQueueItem(
    val skill: SkillType,
    val prompt: String,
    val dueLabel: String,
    val reason: String
)

data class ContentBrowserItem(
    val activityId: String,
    val collectionTitle: String?,
    val unitTitle: String,
    val title: String,
    val skill: SkillType,
    val exerciseType: ExerciseType,
    val sourceLabel: String,
    val tags: List<String>,
    val difficulty: Int?,
    val focus: String,
    val promptPreview: String,
    val effortLabel: String,
    val responseTargetLabel: String
)
