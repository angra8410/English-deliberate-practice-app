package com.example.englishpractice.feature.speaking

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpeakingEvaluatorTest {
    @Test
    fun `evaluateResponse respects speaking metadata thresholds`() {
        val feedback = SpeakingEvaluator.evaluateResponse(
            answer = "In my previous role, I supported clients, handled schedules, and learned how to stay calm under pressure. However, I also improved my communication by presenting weekly updates to the team.",
            transcriptText = "In my previous role, I supported clients, handled schedules, and learned how to stay calm under pressure. However, I also improved my communication by presenting weekly updates to the team.",
            expectedKeywords = listOf("previous role", "communication", "clients"),
            minimumKeywordMatches = 1,
            minimumWordCount = 20
        )

        assertEquals(91, feedback.score)
        assertEquals(
            listOf(
                "The response stays on topic and addresses the prompt.",
                "Good for v1: the transcript is stored and ready for comparison with the model answer."
            ),
            feedback.feedback
        )
        assertTrue(feedback.weakTags.isEmpty())
    }

    @Test
    fun `evaluateResponse flags short off-topic speaking answer`() {
        val feedback = SpeakingEvaluator.evaluateResponse(
            answer = "I think I am ready.",
            transcriptText = "I think I am ready.",
            expectedKeywords = listOf("previous role", "communication", "clients"),
            minimumKeywordMatches = 2,
            minimumWordCount = 20
        )

        assertEquals(
            listOf("response length", "task relevance", "connector range"),
            feedback.weakTags
        )
        assertEquals(
            listOf(
                "Extend the response with a stronger explanation and one concrete example.",
                "Stay closer to the task by naming the main context, your role, or the exact point you are making.",
                "Add a connector such as however or although to improve fluency and range.",
                "Good for v1: the transcript is stored and ready for comparison with the model answer."
            ),
            feedback.feedback
        )
    }
}
