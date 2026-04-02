package com.example.englishpractice.feature.listening

import com.example.englishpractice.feature.practice.PracticeFeedback

object ListeningEvaluator {
    fun evaluateSummary(
        answer: String,
        expectedKeywords: List<String>,
        minimumKeywordMatches: Int = 2,
        minimumWordCount: Int = 25,
        requiresContrastMarker: Boolean = true
    ): PracticeFeedback {
        val normalizedAnswer = answer.lowercase()
        val matchedKeywords = expectedKeywords.count { keyword ->
            normalizedAnswer.contains(keyword.lowercase())
        }
        val feedback = mutableListOf<String>()
        val weakTags = mutableListOf<String>()

        if (matchedKeywords < minimumKeywordMatches) {
            feedback += "State the final position more directly and mention the contrasting detail."
            weakTags += "detail recall"
        } else {
            feedback += "You captured the main listening point and some contrast."
        }

        if (
            requiresContrastMarker &&
            !normalizedAnswer.contains("however") &&
            !normalizedAnswer.contains("but") &&
            !normalizedAnswer.contains("although")
        ) {
            feedback += "Signal the contrast clearly with however, but, or although."
            weakTags += "contrast markers"
        }

        if (answer.split(" ").size < minimumWordCount) {
            feedback += "Add one more sentence so the summary shows both the conclusion and the nuance."
            weakTags += "summary length"
        }

        val score = (48 + matchedKeywords * 14 - weakTags.size * 5).coerceIn(0, 100)
        return PracticeFeedback(score, feedback.distinct(), weakTags.distinct())
    }
}
