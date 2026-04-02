package com.example.englishpractice.ui.screens.activity

object ListeningSubmissionPolicy {
    fun canSubmit(
        isPreparing: Boolean,
        hasStartedPlayback: Boolean,
        hasPlaybackError: Boolean,
        answerText: String
    ): Boolean {
        return !isPreparing &&
            answerText.isNotBlank() &&
            (hasStartedPlayback || hasPlaybackError)
    }
}
