package com.example.englishpractice.feature.reading

import com.example.englishpractice.feature.practice.PracticeFeedback

object ReadingEvaluator {
    fun evaluateSummary(
        answer: String,
        expectedKeywords: List<String>
    ): PracticeFeedback {
        val normalizedAnswer = answer.lowercase()
        val matchedKeywords = expectedKeywords.count { keyword ->
            normalizedAnswer.contains(keyword.lowercase())
        }

        val feedback = mutableListOf<String>()
        val weakTags = mutableListOf<String>()

        if (answer.split(" ").size < 35) {
            feedback += "Your summary is too short to show the main idea and supporting detail."
            weakTags += "summary length"
        }

        if (matchedKeywords < 2) {
            feedback += "Include the main claim and at least one supporting idea from the text."
            weakTags += "supporting ideas"
        } else {
            feedback += "You captured some key ideas from the passage."
        }

        if (!normalizedAnswer.contains("tone") && !normalizedAnswer.contains("stance")) {
            feedback += "Add one sentence about the writer's tone or position."
            weakTags += "tone inference"
        }

        val score = (45 + matchedKeywords * 15 - weakTags.size * 5).coerceIn(0, 100)

        return PracticeFeedback(
            score = score,
            feedback = feedback.distinct(),
            weakTags = weakTags.distinct()
        )
    }
}
