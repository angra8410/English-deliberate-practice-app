package com.example.englishpractice.feature.listening

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import java.util.Locale

data class ListeningCapability(
    val playbackEngine: String,
    val supportsBundledAudio: Boolean,
    val supportsPromptPlayback: Boolean,
    val workflowSteps: List<String>
)

enum class ListeningPlaybackMode {
    BUNDLED_AUDIO,
    PROMPT_SYNTHESIS
}

sealed interface ListeningPlaybackState {
    data object Idle : ListeningPlaybackState
    data class Preparing(
        val mode: ListeningPlaybackMode,
        val sourceLabel: String,
        val positionMs: Long = 0L,
        val durationMs: Long = 0L
    ) : ListeningPlaybackState

    data class Ready(
        val mode: ListeningPlaybackMode,
        val sourceLabel: String,
        val positionMs: Long = 0L,
        val durationMs: Long = 0L
    ) : ListeningPlaybackState

    data class Playing(
        val mode: ListeningPlaybackMode,
        val sourceLabel: String,
        val positionMs: Long,
        val durationMs: Long
    ) : ListeningPlaybackState

    data class Paused(
        val mode: ListeningPlaybackMode,
        val sourceLabel: String,
        val positionMs: Long,
        val durationMs: Long
    ) : ListeningPlaybackState

    data class Completed(
        val mode: ListeningPlaybackMode,
        val sourceLabel: String,
        val positionMs: Long,
        val durationMs: Long
    ) : ListeningPlaybackState

    data class Error(val message: String) : ListeningPlaybackState
}

class ListeningPlayer(private val context: Context) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    private var exoPlayer: ExoPlayer? = null
    private var textToSpeech: TextToSpeech? = null
    private var preparedMode: ListeningPlaybackMode? = null
    private var preparedSourceLabel: String? = null
    private var preparedPromptText: String? = null
    private var preparedLocaleTag: String = DEFAULT_LOCALE_TAG
    private var playbackCallback: ((ListeningPlaybackState) -> Unit)? = null
    private var ttsReady = false
    private var ttsInitPending = false
    private var ttsRestartPending = false
    private var promptStartedAtMs: Long? = null
    private var promptPausedAtMs: Long = 0L
    private var estimatedPromptDurationMs: Long = 0L
    private val progressUpdateRunnable = object : Runnable {
        override fun run() {
            emitProgressState()
            if (
                (preparedMode == ListeningPlaybackMode.BUNDLED_AUDIO && exoPlayer?.isPlaying == true) ||
                (preparedMode == ListeningPlaybackMode.PROMPT_SYNTHESIS && promptStartedAtMs != null)
            ) {
                mainHandler.postDelayed(this, PROGRESS_UPDATE_INTERVAL_MS)
            }
        }
    }

    fun capability(): ListeningCapability {
        return ListeningCapability(
            playbackEngine = "Media3 ExoPlayer with prompt synthesis fallback",
            supportsBundledAudio = true,
            supportsPromptPlayback = true,
            workflowSteps = listOf(
                "prepare source",
                "play or replay prompt audio",
                "pause if needed",
                "capture comprehension response",
                "save summary or answers",
                "tag weak details for review"
            )
        )
    }

    fun prepare(
        audioAssetPath: String?,
        promptText: String?,
        localeTag: String = DEFAULT_LOCALE_TAG,
        onStateChanged: (ListeningPlaybackState) -> Unit
    ) {
        playbackCallback = onStateChanged
        preparedLocaleTag = localeTag

        val assetPath = audioAssetPath?.takeIf(::assetExists)
        when {
            assetPath != null -> prepareBundledAudio(assetPath)
            !promptText.isNullOrBlank() -> preparePromptSynthesis(promptText.trim(), localeTag)
            else -> dispatchState(ListeningPlaybackState.Error("No listening audio source is available yet."))
        }
    }

    fun play() {
        when (preparedMode) {
            ListeningPlaybackMode.BUNDLED_AUDIO -> {
                val player = exoPlayer ?: return
                player.playWhenReady = true
                player.play()
            }

            ListeningPlaybackMode.PROMPT_SYNTHESIS -> speakPrompt(fromStart = true)
            null -> dispatchState(ListeningPlaybackState.Error("Prepare playback before pressing play."))
        }
    }

    fun pause() {
        when (preparedMode) {
            ListeningPlaybackMode.BUNDLED_AUDIO -> exoPlayer?.pause()
            ListeningPlaybackMode.PROMPT_SYNTHESIS -> {
                if (preparedSourceLabel != null) {
                    promptPausedAtMs = promptCurrentPositionMs()
                    promptStartedAtMs = null
                    stopProgressUpdates()
                    textToSpeech?.stop()
                    dispatchState(
                        ListeningPlaybackState.Paused(
                            mode = ListeningPlaybackMode.PROMPT_SYNTHESIS,
                            sourceLabel = preparedSourceLabel!!,
                            positionMs = promptPausedAtMs,
                            durationMs = estimatedPromptDurationMs
                        )
                    )
                }
            }

            null -> Unit
        }
    }

    fun replay() {
        when (preparedMode) {
            ListeningPlaybackMode.BUNDLED_AUDIO -> {
                val player = exoPlayer ?: return
                player.seekTo(0)
                player.playWhenReady = true
                player.play()
            }

            ListeningPlaybackMode.PROMPT_SYNTHESIS -> speakPrompt(fromStart = true)
            null -> dispatchState(ListeningPlaybackState.Error("Prepare playback before replaying audio."))
        }
    }

    fun release() {
        stopProgressUpdates()
        exoPlayer?.release()
        exoPlayer = null

        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null

        preparedMode = null
        preparedSourceLabel = null
        preparedPromptText = null
        playbackCallback = null
        ttsReady = false
        ttsInitPending = false
        ttsRestartPending = false
        promptStartedAtMs = null
        promptPausedAtMs = 0L
        estimatedPromptDurationMs = 0L
    }

    private fun prepareBundledAudio(assetPath: String) {
        preparedMode = ListeningPlaybackMode.BUNDLED_AUDIO
        preparedSourceLabel = "Bundled asset: ${assetPath.substringAfterLast('/')}"
        preparedPromptText = null
        promptStartedAtMs = null
        promptPausedAtMs = 0L
        estimatedPromptDurationMs = 0L
        dispatchState(
            ListeningPlaybackState.Preparing(
                mode = ListeningPlaybackMode.BUNDLED_AUDIO,
                sourceLabel = preparedSourceLabel!!,
                durationMs = 0L
            )
        )

        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(appContext).build().also { player ->
                player.addListener(
                    object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            val sourceLabel = preparedSourceLabel ?: return
                            when (playbackState) {
                                Player.STATE_READY -> {
                                    if (!player.isPlaying) {
                                        dispatchState(
                                            ListeningPlaybackState.Ready(
                                                mode = ListeningPlaybackMode.BUNDLED_AUDIO,
                                                sourceLabel = sourceLabel,
                                                positionMs = player.currentPosition.coerceAtLeast(0L),
                                                durationMs = player.durationOrZero()
                                            )
                                        )
                                    }
                                }

                                Player.STATE_ENDED -> {
                                    stopProgressUpdates()
                                    dispatchState(
                                        ListeningPlaybackState.Completed(
                                            mode = ListeningPlaybackMode.BUNDLED_AUDIO,
                                            sourceLabel = sourceLabel,
                                            positionMs = player.durationOrZero(),
                                            durationMs = player.durationOrZero()
                                        )
                                    )
                                }

                                else -> Unit
                            }
                        }

                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            val sourceLabel = preparedSourceLabel ?: return
                            dispatchState(
                                if (isPlaying) {
                                    startProgressUpdates()
                                    ListeningPlaybackState.Playing(
                                        mode = ListeningPlaybackMode.BUNDLED_AUDIO,
                                        sourceLabel = sourceLabel,
                                        positionMs = player.currentPosition.coerceAtLeast(0L),
                                        durationMs = player.durationOrZero()
                                    )
                                } else if (player.playbackState == Player.STATE_READY) {
                                    stopProgressUpdates()
                                    ListeningPlaybackState.Paused(
                                        mode = ListeningPlaybackMode.BUNDLED_AUDIO,
                                        sourceLabel = sourceLabel,
                                        positionMs = player.currentPosition.coerceAtLeast(0L),
                                        durationMs = player.durationOrZero()
                                    )
                                } else {
                                    return
                                }
                            )
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            stopProgressUpdates()
                            dispatchState(
                                ListeningPlaybackState.Error(
                                    "Audio playback failed. ${error.message ?: "Try replaying the prompt."}"
                                )
                            )
                        }
                    }
                )
            }
        }

        val mediaItem = MediaItem.fromUri(Uri.parse("asset:///$assetPath"))
        exoPlayer?.apply {
            setMediaItem(mediaItem)
            prepare()
        }
    }

    private fun preparePromptSynthesis(promptText: String, localeTag: String) {
        preparedMode = ListeningPlaybackMode.PROMPT_SYNTHESIS
        preparedSourceLabel = "Prompt playback"
        preparedPromptText = promptText
        promptStartedAtMs = null
        promptPausedAtMs = 0L
        estimatedPromptDurationMs = estimatePromptDurationMs(promptText)
        dispatchState(
            ListeningPlaybackState.Preparing(
                mode = ListeningPlaybackMode.PROMPT_SYNTHESIS,
                sourceLabel = preparedSourceLabel!!,
                durationMs = estimatedPromptDurationMs
            )
        )

        if (textToSpeech == null && !ttsInitPending) {
            ttsInitPending = true
            textToSpeech = TextToSpeech(appContext) { status ->
                ttsInitPending = false
                if (status == TextToSpeech.SUCCESS) {
                    ttsReady = true
                    textToSpeech?.language = Locale.forLanguageTag(localeTag)
                    textToSpeech?.setOnUtteranceProgressListener(
                        object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {
                                val sourceLabel = preparedSourceLabel ?: return
                                promptStartedAtMs = SystemClock.elapsedRealtime() - promptPausedAtMs
                                startProgressUpdates()
                                dispatchState(
                                    ListeningPlaybackState.Playing(
                                        mode = ListeningPlaybackMode.PROMPT_SYNTHESIS,
                                        sourceLabel = sourceLabel,
                                        positionMs = promptCurrentPositionMs(),
                                        durationMs = estimatedPromptDurationMs
                                    )
                                )
                            }

                            override fun onDone(utteranceId: String?) {
                                val sourceLabel = preparedSourceLabel ?: return
                                stopProgressUpdates()
                                promptStartedAtMs = null
                                promptPausedAtMs = estimatedPromptDurationMs
                                dispatchState(
                                    ListeningPlaybackState.Completed(
                                        mode = ListeningPlaybackMode.PROMPT_SYNTHESIS,
                                        sourceLabel = sourceLabel,
                                        positionMs = estimatedPromptDurationMs,
                                        durationMs = estimatedPromptDurationMs
                                    )
                                )
                            }

                            @Deprecated("Deprecated in Java")
                            override fun onError(utteranceId: String?) {
                                stopProgressUpdates()
                                dispatchState(
                                    ListeningPlaybackState.Error(
                                        "Prompt playback failed. Try replaying the prompt."
                                    )
                                )
                            }
                        }
                    )
                    dispatchState(
                        ListeningPlaybackState.Ready(
                            mode = ListeningPlaybackMode.PROMPT_SYNTHESIS,
                            sourceLabel = preparedSourceLabel!!,
                            positionMs = promptPausedAtMs,
                            durationMs = estimatedPromptDurationMs
                        )
                    )
                    if (ttsRestartPending) {
                        ttsRestartPending = false
                        speakPrompt(fromStart = true)
                    }
                } else {
                    dispatchState(
                        ListeningPlaybackState.Error(
                            "Prompt playback could not initialize on this device."
                        )
                    )
                }
            }
        } else if (ttsReady) {
            textToSpeech?.language = Locale.forLanguageTag(localeTag)
            dispatchState(
                ListeningPlaybackState.Ready(
                    mode = ListeningPlaybackMode.PROMPT_SYNTHESIS,
                    sourceLabel = preparedSourceLabel!!,
                    positionMs = promptPausedAtMs,
                    durationMs = estimatedPromptDurationMs
                )
            )
        }
    }

    private fun speakPrompt(fromStart: Boolean) {
        if (preparedMode != ListeningPlaybackMode.PROMPT_SYNTHESIS) return
        val promptText = preparedPromptText
        val sourceLabel = preparedSourceLabel
        if (promptText.isNullOrBlank() || sourceLabel.isNullOrBlank()) {
            dispatchState(ListeningPlaybackState.Error("No prompt text is available for playback."))
            return
        }

        if (!ttsReady) {
            ttsRestartPending = true
            dispatchState(
                ListeningPlaybackState.Preparing(
                    mode = ListeningPlaybackMode.PROMPT_SYNTHESIS,
                    sourceLabel = sourceLabel,
                    positionMs = promptPausedAtMs,
                    durationMs = estimatedPromptDurationMs
                )
            )
            return
        }

        if (fromStart) {
            promptPausedAtMs = 0L
            promptStartedAtMs = null
            textToSpeech?.stop()
        }
        val speakResult = textToSpeech?.speak(
            promptText,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "listening_prompt"
        ) ?: TextToSpeech.ERROR

        if (speakResult == TextToSpeech.ERROR) {
            dispatchState(
                ListeningPlaybackState.Error(
                    "Prompt playback could not start. Try replaying the prompt."
                )
            )
        }
    }

    private fun dispatchState(state: ListeningPlaybackState) {
        mainHandler.post {
            playbackCallback?.invoke(state)
        }
    }

    private fun startProgressUpdates() {
        mainHandler.removeCallbacks(progressUpdateRunnable)
        mainHandler.post(progressUpdateRunnable)
    }

    private fun stopProgressUpdates() {
        mainHandler.removeCallbacks(progressUpdateRunnable)
    }

    private fun emitProgressState() {
        val mode = preparedMode ?: return
        val sourceLabel = preparedSourceLabel ?: return
        when (mode) {
            ListeningPlaybackMode.BUNDLED_AUDIO -> {
                val player = exoPlayer ?: return
                if (!player.isPlaying) return
                dispatchState(
                    ListeningPlaybackState.Playing(
                        mode = mode,
                        sourceLabel = sourceLabel,
                        positionMs = player.currentPosition.coerceAtLeast(0L),
                        durationMs = player.durationOrZero()
                    )
                )
            }

            ListeningPlaybackMode.PROMPT_SYNTHESIS -> {
                if (promptStartedAtMs == null) return
                val positionMs = promptCurrentPositionMs()
                dispatchState(
                    ListeningPlaybackState.Playing(
                        mode = mode,
                        sourceLabel = sourceLabel,
                        positionMs = positionMs,
                        durationMs = estimatedPromptDurationMs
                    )
                )
                if (positionMs >= estimatedPromptDurationMs && estimatedPromptDurationMs > 0L) {
                    promptPausedAtMs = estimatedPromptDurationMs
                }
            }
        }
    }

    private fun promptCurrentPositionMs(): Long {
        val startAt = promptStartedAtMs ?: return promptPausedAtMs
        return (SystemClock.elapsedRealtime() - startAt).coerceAtMost(estimatedPromptDurationMs)
    }

    private fun estimatePromptDurationMs(promptText: String): Long {
        val wordCount = promptText.trim().split(Regex("\\s+")).count { it.isNotBlank() }
        if (wordCount == 0) return 0L
        return ((wordCount.toDouble() / SYNTHESIS_WORDS_PER_MINUTE) * 60_000L)
            .toLong()
            .coerceAtLeast(MIN_PROMPT_DURATION_MS)
    }

    private fun assetExists(assetPath: String): Boolean {
        return runCatching {
            appContext.assets.open(assetPath).close()
        }.isSuccess
    }

    private fun ExoPlayer.durationOrZero(): Long {
        return duration.takeIf { it > 0L } ?: 0L
    }

    companion object {
        private const val DEFAULT_LOCALE_TAG = "en-US"
        private const val PROGRESS_UPDATE_INTERVAL_MS = 250L
        private const val SYNTHESIS_WORDS_PER_MINUTE = 155.0
        private const val MIN_PROMPT_DURATION_MS = 2_000L
    }
}
