package com.example.englishpractice.data.repository

import android.content.Context
import androidx.room.Room
import com.example.englishpractice.data.local.AppDatabase
import com.example.englishpractice.data.local.entity.AttemptEntity
import com.example.englishpractice.data.local.entity.MistakeEntity
import com.example.englishpractice.data.local.entity.ReviewItemEntity
import com.example.englishpractice.domain.model.SkillType
import com.example.englishpractice.feature.progress.SkillProgressInput
import com.example.englishpractice.feature.review.ReviewScheduler
import com.example.englishpractice.ui.app.ActivityAttemptRecord
import com.example.englishpractice.ui.app.PracticeActivityItem
import com.example.englishpractice.ui.app.ReviewQueueItem
import com.example.englishpractice.ui.app.WeakPattern
import java.util.UUID

class PracticeRepository private constructor(
    private val database: AppDatabase
) {
    suspend fun saveSubmission(submission: PersistedSubmission) {
        val attemptId = UUID.randomUUID().toString()
        val submittedAt = System.currentTimeMillis()

        database.attemptDao().insert(
            AttemptEntity(
                id = attemptId,
                activityId = submission.activity.id,
                answerText = submission.answer,
                transcriptText = submission.transcriptText,
                score = submission.score,
                submittedAt = submittedAt
            )
        )

        val mistakeEntities = submission.weakTags.mapIndexed { index, tag ->
            MistakeEntity(
                id = UUID.randomUUID().toString(),
                attemptId = attemptId,
                tag = tag,
                note = submission.feedback.getOrElse(index) {
                    "Needs another deliberate-practice retry."
                },
                severity = 1,
                createdAt = submittedAt
            )
        }
        if (mistakeEntities.isNotEmpty()) {
            database.mistakeDao().insertAll(mistakeEntities)
        }

        if (submission.weakTags.isNotEmpty()) {
            val previousItem = database.reviewItemDao().getLatestByActivity(submission.activity.id)
            val intervalDays = ReviewScheduler.nextIntervalDays(
                previousIntervalDays = previousItem?.intervalDays ?: 0,
                wasSuccessful = submission.score >= 75
            )

            database.reviewItemDao().insert(
                ReviewItemEntity(
                    id = UUID.randomUUID().toString(),
                    activityId = submission.activity.id,
                    sourceMistakeTag = submission.weakTags.joinToString(),
                    dueAt = submittedAt + intervalDays * DAY_IN_MILLIS,
                    intervalDays = intervalDays
                )
            )
        }
    }

    suspend fun loadSnapshot(activityCatalog: List<PracticeActivityItem>): PersistedAppSnapshot {
        val attempts = database.attemptDao().getAllByNewest()
        val mistakes = database.mistakeDao().getAllByNewest()
        val reviewItems = database.reviewItemDao().getAllByDueAt()
        val mistakesByAttemptId = mistakes.groupBy { mistake -> mistake.attemptId }
        val latestAttemptByActivityId = attempts.associateBy { attempt -> attempt.activityId }

        val recentAttempts = attempts.take(8).map { attempt ->
            val activity = activityCatalog.firstOrNull { item -> item.id == attempt.activityId }
            val attemptMistakes = mistakesByAttemptId[attempt.id].orEmpty()
            val weakTags = attemptMistakes
                .map { mistake -> mistake.tag }

            ActivityAttemptRecord(
                activityId = attempt.activityId,
                skill = activity?.skill ?: SkillType.READING,
                submittedAnswer = attempt.answerText,
                transcriptText = attempt.transcriptText,
                score = attempt.score ?: 0,
                feedback = attemptMistakes.map { mistake -> mistake.note },
                weakTags = weakTags
            )
        }

        val weakPatterns = mistakes
            .groupBy { mistake -> mistake.tag }
            .entries
            .sortedByDescending { entry -> entry.value.size }
            .take(6)
            .map { entry ->
                val relatedAttemptId = entry.value.firstOrNull()?.attemptId
                val relatedActivityId = attempts
                    .firstOrNull { attempt -> attempt.id == relatedAttemptId }
                    ?.activityId
                val relatedActivity = activityCatalog.firstOrNull { activity ->
                    activity.id == relatedActivityId
                }

                WeakPattern(
                    skill = relatedActivity?.skill ?: SkillType.READING,
                    tag = entry.key,
                    note = entry.value.first().note
                )
            }

        val reviewQueue = reviewItems
            .map { reviewItem ->
                val activity = activityCatalog.firstOrNull { item -> item.id == reviewItem.activityId }
                val latestAttempt = latestAttemptByActivityId[reviewItem.activityId]
                val weakTags = reviewItem.sourceMistakeTag
                    .split(",")
                    .map(String::trim)
                    .filter(String::isNotBlank)

                ReviewQueueItem(
                    activityId = reviewItem.activityId,
                    skill = activity?.skill ?: SkillType.READING,
                    title = activity?.title ?: reviewItem.activityId,
                    prompt = activity?.prompt ?: reviewItem.activityId,
                    dueLabel = dueLabel(reviewItem.dueAt),
                    reason = buildReviewReason(weakTags, latestAttempt?.score),
                    sourceLabel = activity?.sourceLabel ?: "Unknown source",
                    weakTags = weakTags,
                    lastScore = latestAttempt?.score
                ) to reviewItem.dueAt
            }
            .sortedWith(
                compareBy<Pair<ReviewQueueItem, Long>>(
                    { it.second },
                    { it.first.lastScore ?: Int.MAX_VALUE }
                )
            )
            .map { it.first }
            .take(6)

        val progressInputs = activityCatalog.map { activity ->
            val activityAttempts = attempts.filter { attempt -> attempt.activityId == activity.id }
            val averageScore = activityAttempts
                .mapNotNull { attempt -> attempt.score }
                .average()
                .takeIf { !it.isNaN() }
                ?.toInt()
                ?: 0

            SkillProgressInput(
                skill = activity.skill,
                completedActivities = activityAttempts.size,
                targetActivities = 10,
                averageScore = averageScore,
                weakTags = mistakes
                    .filter { mistake ->
                        activityAttempts.any { attempt -> attempt.id == mistake.attemptId }
                    }
                    .map { mistake -> mistake.tag }
                    .distinct()
            )
        }

        return PersistedAppSnapshot(
            recentAttempts = recentAttempts,
            weakPatterns = weakPatterns,
            reviewQueue = reviewQueue,
            skillProgressInputs = progressInputs
        )
    }

    companion object {
        private const val DAY_IN_MILLIS = 24L * 60L * 60L * 1000L

        fun create(context: Context): PracticeRepository {
            val database = Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "english-practice.db"
            ).build()

            return PracticeRepository(database)
        }

        private fun dueLabel(dueAt: Long): String {
            val remainingMillis = dueAt - System.currentTimeMillis()
            return when {
                remainingMillis <= 0L -> "Due now"
                remainingMillis <= DAY_IN_MILLIS -> "Today"
                remainingMillis <= 2L * DAY_IN_MILLIS -> "Tomorrow"
                else -> "Soon"
            }
        }

        private fun buildReviewReason(weakTags: List<String>, lastScore: Int?): String {
            val tagSummary = weakTags.joinToString().ifBlank { "recent weak patterns" }
            return if (lastScore != null) {
                "Retry after score $lastScore with focus on $tagSummary."
            } else {
                "Retry for $tagSummary."
            }
        }
    }
}
