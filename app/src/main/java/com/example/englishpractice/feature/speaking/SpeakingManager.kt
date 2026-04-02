package com.example.englishpractice.feature.speaking

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat

enum class SpeakingAvailability {
    READY,
    PERMISSION_REQUIRED,
    UNSUPPORTED
}

sealed interface SpeakingCaptureState {
    data object Idle : SpeakingCaptureState
    data object Ready : SpeakingCaptureState
    data object Listening : SpeakingCaptureState
    data object Processing : SpeakingCaptureState
    data class PartialTranscript(val transcript: String) : SpeakingCaptureState
    data class TranscriptReady(val transcript: String) : SpeakingCaptureState
    data class Error(val message: String, val partialTranscript: String? = null) : SpeakingCaptureState
}

data class SpeakingCapability(
    val availability: SpeakingAvailability,
    val usesSpeechRecognizer: Boolean,
    val supportedLocales: List<SpeakingLocaleOption>,
    val feedbackDimensions: List<String>,
    val sessionFlow: List<String>
)

data class SpeakingLocaleOption(
    val tag: String,
    val label: String
)

class SpeakingManager(private val context: Context) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var latestPartialTranscript: String? = null
    private var autoRetryUsed = false

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
            supportedLocales = listOf(
                SpeakingLocaleOption("en-US", "English (US)"),
                SpeakingLocaleOption("en-GB", "English (UK)"),
                SpeakingLocaleOption("en-AU", "English (Australia)"),
                SpeakingLocaleOption("en-CA", "English (Canada)")
            ),
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

    fun prepare(onStateChanged: (SpeakingCaptureState) -> Unit) {
        when (capability().availability) {
            SpeakingAvailability.READY -> onStateChanged(SpeakingCaptureState.Ready)
            SpeakingAvailability.PERMISSION_REQUIRED -> {
                onStateChanged(SpeakingCaptureState.Error("Microphone permission is required."))
            }

            SpeakingAvailability.UNSUPPORTED -> {
                onStateChanged(SpeakingCaptureState.Error("Speech recognition is not available on this device."))
            }
        }
    }

    fun startListening(
        localeTag: String,
        onStateChanged: (SpeakingCaptureState) -> Unit
    ) {
        latestPartialTranscript = null
        autoRetryUsed = false
        startListeningInternal(localeTag, onStateChanged)
    }

    private fun startListeningInternal(
        localeTag: String,
        onStateChanged: (SpeakingCaptureState) -> Unit
    ) {
        val currentCapability = capability()
        when (currentCapability.availability) {
            SpeakingAvailability.PERMISSION_REQUIRED -> {
                onStateChanged(SpeakingCaptureState.Error("Microphone permission is required."))
                return
            }

            SpeakingAvailability.UNSUPPORTED -> {
                onStateChanged(SpeakingCaptureState.Error("Speech recognition is not available on this device."))
                return
            }

            SpeakingAvailability.READY -> Unit
        }

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        }

        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeTag)
        }

        speechRecognizer?.setRecognitionListener(
            object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    onStateChanged(SpeakingCaptureState.Listening)
                }

                override fun onBeginningOfSpeech() = Unit

                override fun onRmsChanged(rmsdB: Float) = Unit

                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() {
                    onStateChanged(SpeakingCaptureState.Processing)
                }

                override fun onError(error: Int) {
                    val partialTranscript = latestPartialTranscript

                    if (
                        error == SpeechRecognizer.ERROR_NO_MATCH &&
                        !partialTranscript.isNullOrBlank()
                    ) {
                        onStateChanged(
                            SpeakingCaptureState.Error(
                                message = "No final match was found, but the partial transcript was kept.",
                                partialTranscript = partialTranscript
                            )
                        )
                        return
                    }

                    if (
                        error == SpeechRecognizer.ERROR_NO_MATCH &&
                        !autoRetryUsed
                    ) {
                        autoRetryUsed = true
                        onStateChanged(
                            SpeakingCaptureState.Error(
                                message = "No clear match was found. Retrying once automatically."
                            )
                        )
                        startListeningInternal(localeTag, onStateChanged)
                        return
                    }

                    onStateChanged(
                        SpeakingCaptureState.Error(
                            message = errorMessage(error),
                            partialTranscript = partialTranscript
                        )
                    )
                }

                override fun onResults(results: Bundle?) {
                    val transcript = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.trim()

                    if (transcript.isNullOrBlank()) {
                        onStateChanged(SpeakingCaptureState.Error("No speech was captured."))
                    } else {
                        onStateChanged(SpeakingCaptureState.TranscriptReady(transcript))
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val transcript = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.trim()

                    if (!transcript.isNullOrBlank()) {
                        latestPartialTranscript = transcript
                        onStateChanged(SpeakingCaptureState.PartialTranscript(transcript))
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            }
        )

        onStateChanged(SpeakingCaptureState.Listening)
        speechRecognizer?.startListening(recognizerIntent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
    }

    fun release() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        latestPartialTranscript = null
        autoRetryUsed = false
    }

    private fun errorMessage(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "There was an audio recording problem. Check the microphone and try again."
            SpeechRecognizer.ERROR_CLIENT -> "Speech recognition was interrupted by the app or device. Try again."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is missing. Enable it and retry."
            SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition. Try a more stable connection or another locale."
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition timed out because of the network. Retry or try again later."
            SpeechRecognizer.ERROR_NO_MATCH -> "No clear speech match was found. Try speaking longer, more clearly, or switch the English locale."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognition is busy. Wait a moment, then tap retry."
            SpeechRecognizer.ERROR_SERVER -> "The speech recognition service returned an error. Retry in a moment."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech was detected before the timeout. Start speaking right after listening begins."
            else -> "Speech recognition ended with error code $error."
        }
    }
}
