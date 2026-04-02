package com.example.englishpractice.feature.speaking

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat

enum class SpeakingAvailability {
    READY,
    PERMISSION_REQUIRED,
    UNSUPPORTED
}

sealed interface SpeakingCaptureState {
    data object Idle : SpeakingCaptureState
    data object Listening : SpeakingCaptureState
    data class TranscriptReady(val transcript: String) : SpeakingCaptureState
    data class Error(val message: String) : SpeakingCaptureState
}

data class SpeakingCapability(
    val availability: SpeakingAvailability,
    val usesSpeechRecognizer: Boolean,
    val feedbackDimensions: List<String>,
    val sessionFlow: List<String>
)

class SpeakingManager(private val context: Context) {
    fun capability(): SpeakingCapability {
        val speechAvailable = SpeechRecognizer.isRecognitionAvailable(context)
        val microphoneGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val availability = when {
            !speechAvailable -> SpeakingAvailability.UNSUPPORTED
            !microphoneGranted -> SpeakingAvailability.PERMISSION_REQUIRED
            else -> SpeakingAvailability.READY
        }

        return SpeakingCapability(
            availability = availability,
            usesSpeechRecognizer = speechAvailable,
            feedbackDimensions = listOf(
                "grammar",
                "vocabulary range",
                "fluency markers",
                "response length",
                "task relevance"
            ),
            sessionFlow = listOf(
                "show prompt",
                "capture speech",
                "save transcript",
                "run structured feedback",
                "compare with model answer",
                "push weak tags into review"
            )
        )
    }
}
