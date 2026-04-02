package com.example.englishpractice.feature.listening

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ListeningEvaluatorTest {
    @Test
    fun `evaluateSummary rewards strong listening answer with contrast marker and coverage`() {
        val feedback = ListeningEvaluator.evaluateSummary(
            answer = "The speaker supports a hybrid model because junior employees need mentoring and regular feedback, but remote work still offers flexibility and convenience for focused tasks early in the week.",
            expectedKeywords = listOf("hybrid", "mentoring", "junior", "contrast")
        )

        assertEquals(90, feedback.score)
        assertEquals(
            listOf("You captured the main listening point and some contrast."),
            feedback.feedback
        )
        assertTrue(feedback.weakTags.isEmpty())
    }

    @Test
    fun `evaluateSummary flags missing detail contrast and length in weak answer`() {
        val feedback = ListeningEvaluator.evaluateSummary(
            answer = "Remote work is convenient.",
            expectedKeywords = listOf("hybrid", "mentoring", "junior", "contrast")
        )

        assertEquals(33, feedback.score)
        assertEquals(
            listOf(
                "State the final position more directly and mention the contrasting detail.",
                "Signal the contrast clearly with however, but, or although.",
                "Add one more sentence so the summary shows both the conclusion and the nuance."
            ),
            feedback.feedback
        )
        assertEquals(
            listOf("detail recall", "contrast markers", "summary length"),
            feedback.weakTags
        )
    }
}
