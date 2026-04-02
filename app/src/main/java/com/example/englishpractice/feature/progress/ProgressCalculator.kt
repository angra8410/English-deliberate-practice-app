package com.example.englishpractice.feature.progress

object ProgressCalculator {
    fun buildSnapshot(input: SkillProgressInput): SkillProgressSnapshot {
        val target = input.targetActivities.coerceAtLeast(1)
        val completionPercent = ((input.completedActivities.toFloat() / target.toFloat()) * 100f)
            .toInt()
            .coerceIn(0, 100)

        return SkillProgressSnapshot(
            skill = input.skill,
            completionPercent = completionPercent,
            averageScore = input.averageScore.coerceIn(0, 100),
            weakTags = input.weakTags
        )
    }

    fun overallCompletion(snapshots: List<SkillProgressSnapshot>): Int {
        if (snapshots.isEmpty()) return 0
        return snapshots.map { it.completionPercent }.average().toInt()
    }

    fun weakestTags(inputs: List<SkillProgressInput>, limit: Int = 5): List<String> {
        return inputs
            .flatMap { input -> input.weakTags }
            .groupingBy { tag -> tag }
            .eachCount()
            .entries
            .sortedByDescending { entry -> entry.value }
            .take(limit)
            .map { entry -> entry.key }
    }
}
