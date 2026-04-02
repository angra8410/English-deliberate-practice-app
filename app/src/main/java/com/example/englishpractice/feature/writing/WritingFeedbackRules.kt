package com.example.englishpractice.feature.writing

import com.example.englishpractice.feature.practice.PracticeFeedback

object WritingFeedbackRules {
    private val strongerConnectors = listOf("however", "therefore", "moreover", "although", "while")
    private val basicWords = listOf("good", "bad", "nice", "thing", "very")

    fun evaluateAnswer(answer: String): PracticeFeedback {
        val normalizedAnswer = answer.lowercase()
        val words = answer.split(Regex("\\s+")).filter { it.isNotBlank() }
        val feedback = mutableListOf<String>()
        val weakTags = mutableListOf<String>()

        if (words.size < 45) {
            feedback += "Expand the response with a clearer reason and one supporting example."
            weakTags += "response length"
        }

        if (strongerConnectors.none { connector -> normalizedAnswer.contains(connector) }) {
            feedback += "Use a stronger connector such as however, therefore, or although."
            weakTags += "connectors"
        } else {
            feedback += "You are starting to structure the response with linking language."
        }

        if (basicWords.count { word -> normalizedAnswer.contains(word) } >= 3) {
            feedback += "Replace repeated basic vocabulary with more precise wording."
            weakTags += "word choice"
        }

        if (!normalizedAnswer.contains("should") && !normalizedAnswer.contains("need")) {
            feedback += "Make your opinion more explicit so the position is easy to follow."
            weakTags += "task response"
        }

        val score = (50 + words.size / 4 - weakTags.size * 6).coerceIn(0, 100)

        return PracticeFeedback(
            score = score,
            feedback = feedback.distinct(),
            weakTags = weakTags.distinct()
        )
    }
}
