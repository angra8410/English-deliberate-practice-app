package com.example.englishpractice.feature.speaking

import com.example.englishpractice.feature.practice.PracticeFeedback

object SpeakingEvaluator {
    fun evaluateResponse(
        answer: String,
        transcriptText: String,
        expectedKeywords: List<String>,
        minimumKeywordMatches: Int = 2,
        minimumWordCount: Int = 35
    ): PracticeFeedback {
        val normalizedTranscript = transcriptText.lowercase()
        val matchedKeywords = expectedKeywords.count { keyword ->
            normalizedTranscript.contains(keyword.lowercase())
        }
        val feedback = mutableListOf<String>()
        val weakTags = mutableListOf<String>()

        if (transcriptText.split(" ").size < minimumWordCount) {
            feedback += "Extend the response with a stronger explanation and one concrete example."
            weakTags += "response length"
        }

        if (matchedKeywords < minimumKeywordMatches) {
            feedback += "Stay closer to the task by naming the main context, your role, or the exact point you are making."
            weakTags += "task relevance"
        } else {
            feedback += "The response stays on topic and addresses the prompt."
        }

        if (!normalizedTranscript.contains("however") && !normalizedTranscript.contains("although")) {
            feedback += "Add a connector such as however or although to improve fluency and range."
            weakTags += "connector range"
        }

        if (answer == transcriptText) {
            feedback += "Good for v1: the transcript is stored and ready for comparison with the model answer."
        }

        val score = (46 + matchedKeywords * 15 - weakTags.size * 5).coerceIn(0, 100)
        return PracticeFeedback(score, feedback.distinct(), weakTags.distinct())
    }
}
