package com.example.englishpractice.ui.screens.activity

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ListeningSubmissionPolicyTest {
    @Test
    fun `can submit after playback starts`() {
        assertTrue(
            ListeningSubmissionPolicy.canSubmit(
                isPreparing = false,
                hasStartedPlayback = true,
                hasPlaybackError = false,
                answerText = "The speaker supports hybrid work."
            )
        )
    }

    @Test
    fun `can submit when playback fails but written fallback is available`() {
        assertTrue(
            ListeningSubmissionPolicy.canSubmit(
                isPreparing = false,
                hasStartedPlayback = false,
                hasPlaybackError = true,
                answerText = "The speaker prefers mentoring for junior staff."
            )
        )
    }

    @Test
    fun `cannot submit while preparing without fallback or with blank answer`() {
        assertFalse(
            ListeningSubmissionPolicy.canSubmit(
                isPreparing = true,
                hasStartedPlayback = true,
                hasPlaybackError = false,
                answerText = "Some answer"
            )
        )
        assertFalse(
            ListeningSubmissionPolicy.canSubmit(
                isPreparing = false,
                hasStartedPlayback = false,
                hasPlaybackError = false,
                answerText = "Some answer"
            )
        )
        assertFalse(
            ListeningSubmissionPolicy.canSubmit(
                isPreparing = false,
                hasStartedPlayback = true,
                hasPlaybackError = false,
                answerText = "   "
            )
        )
    }
}
