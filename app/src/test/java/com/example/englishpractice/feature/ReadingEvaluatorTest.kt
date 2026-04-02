package com.example.englishpractice.feature.reading

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReadingEvaluatorTest {
    @Test
    fun `evaluateSummary respects optional tone requirement`() {
        val feedback = ReadingEvaluator.evaluateSummary(
            answer = "The article argues that clearer regulation is necessary because the current rules confuse consumers and businesses.",
            expectedKeywords = listOf("regulation", "consumers"),
            minimumKeywordMatches = 1,
            minimumWordCount = 10,
            requiresToneReference = false
        )

        assertEquals(75, feedback.score)
        assertEquals(
            listOf("You captured some key ideas from the passage."),
            feedback.feedback
        )
        assertTrue(feedback.weakTags.isEmpty())
    }

    @Test
    fun `evaluateSummary still flags missing tone when required`() {
        val feedback = ReadingEvaluator.evaluateSummary(
            answer = "The article argues that clearer regulation is necessary because the current rules confuse consumers and businesses.",
            expectedKeywords = listOf("regulation", "consumers"),
            minimumKeywordMatches = 1,
            minimumWordCount = 10,
            requiresToneReference = true
        )

        assertEquals(listOf("tone inference"), feedback.weakTags)
        assertEquals(
            listOf(
                "You captured some key ideas from the passage.",
                "Add one sentence about the writer's tone or position."
            ),
            feedback.feedback
        )
    }
}
