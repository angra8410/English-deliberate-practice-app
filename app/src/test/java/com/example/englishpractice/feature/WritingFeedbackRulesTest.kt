package com.example.englishpractice.feature.writing

import com.example.englishpractice.ui.app.PromptScoringProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WritingFeedbackRulesTest {
    @Test
    fun `evaluateAnswer handles vocabulary list task without opinion penalty`() {
        val feedback = WritingFeedbackRules.evaluateAnswer(
            answer = "competitive salary, excellent career prospects, full training provided, relevant prior experience, strong communication skills",
            expectedKeywords = listOf(
                "competitive salary",
                "career prospects",
                "training provided",
                "prior experience",
                "communication skills"
            ),
            scoringProfile = PromptScoringProfile.LIST,
            minimumWordCount = 10,
            minimumResponseItems = 5
        )

        assertTrue(feedback.score >= 75)
        assertTrue(feedback.weakTags.isEmpty())
        assertEquals(
            listOf("The response captures several useful target expressions."),
            feedback.feedback
        )
    }

    @Test
    fun `evaluateAnswer flags missing pairs in sentence drill`() {
        val feedback = WritingFeedbackRules.evaluateAnswer(
            answer = "She works in London. She is working from home this week.",
            expectedKeywords = listOf("present simple", "present continuous"),
            scoringProfile = PromptScoringProfile.SENTENCE_DRILL,
            minimumWordCount = 18,
            minimumResponseItems = 3
        )

        assertEquals(
            listOf("sentence pairs", "response length"),
            feedback.weakTags
        )
        assertEquals(
            listOf(
                "Add the missing sentence pairs so both forms are practiced enough times.",
                "Expand the drill so each pair is expressed as a full sentence."
            ),
            feedback.feedback
        )
    }
}
