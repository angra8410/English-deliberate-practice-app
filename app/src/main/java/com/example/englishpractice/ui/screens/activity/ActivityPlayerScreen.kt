package com.example.englishpractice.ui.screens.activity

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.englishpractice.domain.model.ExerciseType
import com.example.englishpractice.domain.model.SkillType
import com.example.englishpractice.feature.listening.*
import com.example.englishpractice.feature.speaking.*
import com.example.englishpractice.ui.app.ActivityAttemptRecord
import com.example.englishpractice.ui.app.PracticeActivityItem
import com.example.englishpractice.ui.components.*

private enum class PlayerStage { Brief, Exercise, Response, Feedback }

@Composable
fun ActivityPlayerScreen(
    activity: PracticeActivityItem?,
    availableListeningActivities: List<PracticeActivityItem>,
    lastAttempt: ActivityAttemptRecord?,
    selectedSpeakingLocaleTag: String,
    speakingCapability: SpeakingCapability,
    listeningCapability: ListeningCapability,
    onBack: () -> Unit,
    onSpeakingLocaleSelected: (String) -> Unit,
    onListeningActivitySelected: (String) -> Unit,
    onSubmit: (answer: String, transcriptText: String?) -> Unit
) {
    if (activity == null) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("The selected activity could not be loaded.")
        }
        return
    }

    val initialStage = if (lastAttempt?.activityId == activity.id) PlayerStage.Feedback else PlayerStage.Brief
    var playerStageName by rememberSaveable(activity.id, lastAttempt?.activityId) {
        mutableStateOf(initialStage.name)
    }
    val playerStage = PlayerStage.valueOf(playerStageName)
    var answerText by rememberSaveable(activity.id) { mutableStateOf(activity.starterText) }
    var listeningMode by rememberSaveable(activity.id) { mutableStateOf<ListeningPlaybackMode?>(null) }
    var listeningStatus by rememberSaveable(activity.id) { mutableStateOf("Idle") }
    var listeningHint by rememberSaveable(activity.id) { mutableStateOf("Press play to hear the prompt, then continue to the response stage.") }
    var listeningSourceLabel by rememberSaveable(activity.id) { mutableStateOf("No source prepared") }
    var listeningPositionMs by rememberSaveable(activity.id) { mutableStateOf(0L) }
    var listeningDurationMs by rememberSaveable(activity.id) { mutableStateOf(0L) }
    var isListeningPreparing by rememberSaveable(activity.id) { mutableStateOf(false) }
    var isListeningPlaying by rememberSaveable(activity.id) { mutableStateOf(false) }
    var hasStartedListeningPlayback by rememberSaveable(activity.id) { mutableStateOf(false) }
    var hasCompletedListeningPlayback by rememberSaveable(activity.id) { mutableStateOf(false) }
    var hasListeningPlaybackError by rememberSaveable(activity.id) { mutableStateOf(false) }
    var speakingStatus by rememberSaveable(activity.id) { mutableStateOf("Idle") }
    var capturedTranscript by rememberSaveable(activity.id) { mutableStateOf<String?>(null) }
    var lastAppliedTranscript by rememberSaveable(activity.id) { mutableStateOf<String?>(null) }
    var isSpeakingListening by rememberSaveable(activity.id) { mutableStateOf(false) }
    var isSpeakingProcessing by rememberSaveable(activity.id) { mutableStateOf(false) }
    var canRetrySpeaking by rememberSaveable(activity.id) { mutableStateOf(false) }
    var hasRequestedMicPermission by rememberSaveable(activity.id) { mutableStateOf(false) }
    var showOpenSettings by rememberSaveable(activity.id) { mutableStateOf(false) }
    var speakingAvailability by rememberSaveable(activity.id) { mutableStateOf(speakingCapability.availability) }
    var speakingHint by rememberSaveable(activity.id) { mutableStateOf("Tap start listening, speak in one clear take, then review the transcript.") }
    var showListeningLibrary by rememberSaveable(activity.id) { mutableStateOf(false) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val listeningPlayer = remember(context) { ListeningPlayer(context) }
    val speakingManager = remember(context) { SpeakingManager(context) }
    val isListeningBusy = isListeningPreparing || isListeningPlaying
    val isSpeakingBusy = isSpeakingListening || isSpeakingProcessing
    val tone = skillTone(activity.skill)
    val stageProgress = when (playerStage) {
        PlayerStage.Brief -> 0.2f
        PlayerStage.Exercise -> 0.48f
        PlayerStage.Response -> 0.76f
        PlayerStage.Feedback -> 1f
    }
    val stageLabel = when (playerStage) {
        PlayerStage.Brief -> "Mission brief"
        PlayerStage.Exercise -> "Exercise"
        PlayerStage.Response -> "Response"
        PlayerStage.Feedback -> "Review"
    }
    val fieldLabel = when (activity.skill) {
        SkillType.SPEAKING -> "Transcript or speaking notes"
        SkillType.LISTENING -> "Listening summary"
        else -> "Your response"
    }
    val listeningItems = remember(availableListeningActivities) {
        availableListeningActivities.filter { it.skill == SkillType.LISTENING && (it.audioAssetPath != null || it.listeningPromptText != null) }
    }
    val currentListeningIndex = listeningItems.indexOfFirst { it.id == activity.id }
    val focusTokens = remember(activity.id) { buildFocusTokens(activity) }
    val submitEnabled = when (activity.skill) {
        SkillType.SPEAKING -> !isSpeakingBusy && answerText.isNotBlank()
        SkillType.LISTENING -> ListeningSubmissionPolicy.canSubmit(
            isPreparing = isListeningPreparing,
            hasStartedPlayback = hasStartedListeningPlayback,
            hasPlaybackError = hasListeningPlaybackError,
            answerText = answerText
        )
        else -> answerText.isNotBlank()
    }

    fun goToPreviousStageOrBack() {
        playerStageName = when (playerStage) {
            PlayerStage.Brief -> { onBack(); PlayerStage.Brief.name }
            PlayerStage.Exercise -> PlayerStage.Brief.name
            PlayerStage.Response -> PlayerStage.Exercise.name
            PlayerStage.Feedback -> PlayerStage.Response.name
        }
    }

    fun applyTranscript(transcript: String) {
        val safeTranscript = transcript.trim()
        if (safeTranscript.isBlank()) return
        val canOverwriteDraft = answerText.isBlank() || answerText == activity.starterText || answerText == lastAppliedTranscript
        capturedTranscript = safeTranscript
        if (canOverwriteDraft) answerText = safeTranscript
        lastAppliedTranscript = safeTranscript
    }

    fun handleListeningState(state: ListeningPlaybackState) {
        when (state) {
            ListeningPlaybackState.Idle -> {
                listeningMode = null
                listeningStatus = "Idle"
                listeningSourceLabel = "No source prepared"
                listeningPositionMs = 0L
                listeningDurationMs = 0L
                isListeningPreparing = false
                isListeningPlaying = false
                hasStartedListeningPlayback = false
                hasCompletedListeningPlayback = false
                hasListeningPlaybackError = false
                listeningHint = "Press play to hear the prompt, then continue to the response stage."
            }
            is ListeningPlaybackState.Preparing -> {
                listeningMode = state.mode
                listeningStatus = "Preparing playback..."
                listeningSourceLabel = state.sourceLabel
                listeningPositionMs = state.positionMs
                listeningDurationMs = state.durationMs
                isListeningPreparing = true
                isListeningPlaying = false
                hasListeningPlaybackError = false
                listeningHint = if (state.mode == ListeningPlaybackMode.BUNDLED_AUDIO) {
                    "Bundled audio is loading. The main control will unlock as soon as playback is ready."
                } else {
                    "Prompt synthesis is preparing. The spoken fallback will use the current English locale."
                }
            }
            is ListeningPlaybackState.Ready -> {
                listeningMode = state.mode
                listeningStatus = "Ready"
                listeningSourceLabel = state.sourceLabel
                listeningPositionMs = state.positionMs
                listeningDurationMs = state.durationMs
                isListeningPreparing = false
                isListeningPlaying = false
                hasListeningPlaybackError = false
                listeningHint = if (state.mode == ListeningPlaybackMode.BUNDLED_AUDIO) {
                    "Listen once for the overall message, then replay if you need the contrasting detail."
                } else {
                    "Play the spoken prompt, catch the main position, then move into the summary stage."
                }
            }
            is ListeningPlaybackState.Playing -> {
                listeningMode = state.mode
                listeningStatus = "Playing..."
                listeningSourceLabel = state.sourceLabel
                listeningPositionMs = state.positionMs
                listeningDurationMs = state.durationMs
                isListeningPreparing = false
                isListeningPlaying = true
                hasStartedListeningPlayback = true
                hasListeningPlaybackError = false
                listeningHint = "Stay in listening mode until the speaker finishes one full pass."
            }
            is ListeningPlaybackState.Paused -> {
                listeningMode = state.mode
                listeningStatus = "Paused"
                listeningSourceLabel = state.sourceLabel
                listeningPositionMs = state.positionMs
                listeningDurationMs = state.durationMs
                isListeningPreparing = false
                isListeningPlaying = false
                hasStartedListeningPlayback = true
                hasListeningPlaybackError = false
                listeningHint = if (state.mode == ListeningPlaybackMode.BUNDLED_AUDIO) {
                    "Resume from play or replay the full clip before writing your summary."
                } else {
                    "Prompt playback restarts from the beginning whenever you play again."
                }
            }
            is ListeningPlaybackState.Completed -> {
                listeningMode = state.mode
                listeningStatus = "Completed"
                listeningSourceLabel = state.sourceLabel
                listeningPositionMs = state.positionMs
                listeningDurationMs = state.durationMs
                isListeningPreparing = false
                isListeningPlaying = false
                hasStartedListeningPlayback = true
                hasCompletedListeningPlayback = true
                hasListeningPlaybackError = false
                listeningHint = "Good. Move into the response stage while the argument is still fresh."
            }
            is ListeningPlaybackState.Error -> {
                listeningStatus = state.message
                isListeningPreparing = false
                isListeningPlaying = false
                hasListeningPlaybackError = true
                listeningHint = "Audio is unavailable. Continue with the written prompt and still submit your summary."
            }
        }
    }

    fun handleSpeakingState(state: SpeakingCaptureState) {
        when (state) {
            SpeakingCaptureState.Idle -> {
                speakingStatus = "Idle"
                isSpeakingListening = false
                isSpeakingProcessing = false
                canRetrySpeaking = false
                speakingHint = "The recognizer is idle. Start a fresh capture when you are ready."
            }
            SpeakingCaptureState.Ready -> {
                speakingStatus = "Ready to listen"
                isSpeakingListening = false
                isSpeakingProcessing = false
                canRetrySpeaking = false
                speakingHint = "Speak naturally for 4 to 6 seconds and answer the prompt directly."
            }
            SpeakingCaptureState.Listening -> {
                speakingStatus = "Listening..."
                isSpeakingListening = true
                isSpeakingProcessing = false
                canRetrySpeaking = false
                speakingHint = "Keep speaking until you complete one full answer."
            }
            SpeakingCaptureState.Processing -> {
                speakingStatus = "Processing speech..."
                isSpeakingListening = false
                isSpeakingProcessing = true
                canRetrySpeaking = false
                speakingHint = "Hold on while the recognizer turns your speech into text."
            }
            is SpeakingCaptureState.PartialTranscript -> {
                speakingStatus = "Partial transcript captured"
                applyTranscript(state.transcript)
                canRetrySpeaking = false
                speakingHint = "Partial speech was captured. Keep talking or stop when your thought is complete."
            }
            is SpeakingCaptureState.TranscriptReady -> {
                speakingStatus = "Transcript captured"
                isSpeakingListening = false
                isSpeakingProcessing = false
                applyTranscript(state.transcript)
                canRetrySpeaking = false
                speakingHint = "Review the transcript, adjust it if needed, then continue to the response stage."
            }
            is SpeakingCaptureState.Error -> {
                speakingStatus = state.message
                isSpeakingListening = false
                isSpeakingProcessing = false
                canRetrySpeaking = !state.message.contains("Retrying once automatically")
                if (!state.partialTranscript.isNullOrBlank()) applyTranscript(state.partialTranscript)
                speakingHint = if (canRetrySpeaking) {
                    "Retry immediately or keep editing the transcript already captured."
                } else {
                    "The recognizer is recovering automatically."
                }
            }
        }
    }

    fun refreshSpeakingReadiness() {
        speakingAvailability = speakingManager.capability().availability
        speakingManager.prepare(::handleSpeakingState)
    }

    fun refreshListeningPlayback() {
        listeningPlayer.prepare(
            audioAssetPath = activity.audioAssetPath,
            promptText = activity.listeningPromptText ?: activity.prompt,
            localeTag = selectedSpeakingLocaleTag,
            onStateChanged = ::handleListeningState
        )
    }

    BackHandler(enabled = playerStage != PlayerStage.Brief) { goToPreviousStageOrBack() }

    DisposableEffect(speakingManager, activity.skill) {
        if (activity.skill == SkillType.SPEAKING) refreshSpeakingReadiness()
        onDispose { speakingManager.release() }
    }

    DisposableEffect(listeningPlayer, activity.skill, activity.audioAssetPath, activity.listeningPromptText, selectedSpeakingLocaleTag) {
        if (activity.skill == SkillType.LISTENING) refreshListeningPlayback()
        onDispose { listeningPlayer.release() }
    }

    DisposableEffect(lifecycleOwner, speakingManager, activity.skill) {
        if (activity.skill != SkillType.SPEAKING) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) refreshSpeakingReadiness()
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
    }

    LaunchedEffect(lastAttempt?.activityId, activity.id) {
        if (lastAttempt?.activityId == activity.id) playerStageName = PlayerStage.Feedback.name
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            showOpenSettings = false
            refreshSpeakingReadiness()
            speakingManager.startListening(selectedSpeakingLocaleTag, ::handleSpeakingState)
        } else {
            speakingAvailability = speakingManager.capability().availability
            val hostActivity = context.findActivity()
            val permanentlyDenied = hasRequestedMicPermission &&
                hostActivity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(hostActivity, Manifest.permission.RECORD_AUDIO)
            showOpenSettings = permanentlyDenied
            speakingStatus = if (permanentlyDenied) "Microphone permission is blocked" else "Microphone permission denied"
            canRetrySpeaking = false
            speakingHint = if (permanentlyDenied) {
                "Open app settings, enable microphone access, then return to start speaking."
            } else {
                "Allow microphone permission to capture speech, or continue with typed speaking practice."
            }
        }
    }

    ImmersiveScreen(contentPadding = PaddingValues(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 26.dp)) {
        PlayerHeader(activity = activity, stageLabel = stageLabel, stageProgress = stageProgress, onBack = { goToPreviousStageOrBack() })

        AnimatedContent(targetState = playerStage, label = "activity-player-stage") { stage ->
            when (stage) {
                PlayerStage.Brief -> MissionBriefStage(
                    activity = activity,
                    listeningCapability = listeningCapability,
                    toneColor = tone.accent,
                    onStart = { playerStageName = PlayerStage.Exercise.name }
                )
                PlayerStage.Exercise -> ExerciseStage(
                    activity = activity,
                    focusTokens = focusTokens,
                    listeningCapability = listeningCapability,
                    listeningMode = listeningMode,
                    listeningStatus = listeningStatus,
                    listeningHint = listeningHint,
                    listeningSourceLabel = listeningSourceLabel,
                    listeningPositionMs = listeningPositionMs,
                    listeningDurationMs = listeningDurationMs,
                    isListeningPreparing = isListeningPreparing,
                    isListeningPlaying = isListeningPlaying,
                    hasStartedListeningPlayback = hasStartedListeningPlayback,
                    hasCompletedListeningPlayback = hasCompletedListeningPlayback,
                    hasListeningPlaybackError = hasListeningPlaybackError,
                    onPlayListening = { listeningPlayer.play() },
                    onPauseListening = { listeningPlayer.pause() },
                    onReplayListening = { listeningPlayer.replay() },
                    listeningItems = listeningItems,
                    currentListeningIndex = currentListeningIndex,
                    showListeningLibrary = showListeningLibrary,
                    onToggleListeningLibrary = { showListeningLibrary = !showListeningLibrary },
                    onListeningActivitySelected = onListeningActivitySelected,
                    speakingAvailability = speakingAvailability,
                    speakingCapability = speakingCapability,
                    selectedSpeakingLocaleTag = selectedSpeakingLocaleTag,
                    speakingStatus = speakingStatus,
                    speakingHint = speakingHint,
                    capturedTranscript = capturedTranscript,
                    showOpenSettings = showOpenSettings,
                    isSpeakingBusy = isSpeakingBusy,
                    isSpeakingListening = isSpeakingListening,
                    isSpeakingProcessing = isSpeakingProcessing,
                    canRetrySpeaking = canRetrySpeaking,
                    onSpeakingLocaleSelected = onSpeakingLocaleSelected,
                    onRequestMicPermission = {
                        hasRequestedMicPermission = true
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onStartListening = {
                        showOpenSettings = false
                        speakingManager.startListening(selectedSpeakingLocaleTag, ::handleSpeakingState)
                    },
                    onStopListening = {
                        speakingManager.stopListening()
                        speakingStatus = "Stopped"
                        isSpeakingListening = false
                        isSpeakingProcessing = false
                        speakingHint = "Capture stopped. Review what you have or start another attempt."
                    },
                    onUseCapturedTranscript = {
                        capturedTranscript?.let {
                            answerText = it
                            lastAppliedTranscript = it
                        }
                    },
                    onClearCapturedTranscript = {
                        if (answerText == capturedTranscript) answerText = ""
                        capturedTranscript = null
                        lastAppliedTranscript = null
                        canRetrySpeaking = false
                        speakingStatus = "Transcript cleared"
                        speakingHint = "Start a fresh capture or type your answer manually."
                    },
                    onRetrySpeaking = {
                        capturedTranscript = null
                        lastAppliedTranscript = null
                        speakingManager.startListening(selectedSpeakingLocaleTag, ::handleSpeakingState)
                    },
                    onOpenSettings = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                        )
                    },
                    onContinue = { playerStageName = PlayerStage.Response.name }
                )
                PlayerStage.Response -> ResponseStage(
                    activity = activity,
                    fieldLabel = fieldLabel,
                    answerText = answerText,
                    onAnswerChanged = { answerText = it },
                    submitEnabled = submitEnabled,
                    capturedTranscript = capturedTranscript,
                    onBackToExercise = { playerStageName = PlayerStage.Exercise.name },
                    onSubmit = {
                        val transcript = if (activity.skill == SkillType.SPEAKING) capturedTranscript ?: answerText else null
                        onSubmit(answerText, transcript)
                        playerStageName = PlayerStage.Feedback.name
                    }
                )
                PlayerStage.Feedback -> FeedbackStage(
                    activity = activity,
                    lastAttempt = lastAttempt,
                    onTryAgain = { playerStageName = PlayerStage.Response.name },
                    onBackToPath = onBack
                )
            }
        }

        AnimatedVisibility(visible = playerStage != PlayerStage.Feedback && lastAttempt?.activityId == activity.id) {
            lastAttempt?.let { CompactAttemptSummary(attempt = it, modelAnswer = activity.modelAnswer) }
        }
    }
}

@Composable
private fun PlayerHeader(
    activity: PracticeActivityItem,
    stageLabel: String,
    stageProgress: Float,
    onBack: () -> Unit
) {
    val tone = skillTone(activity.skill)
    GlassPanel(accent = tone.accent.copy(alpha = 0.28f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stageLabel, style = MaterialTheme.typography.labelLarge, color = tone.accent)
                    Text(activity.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                }
            }
            StageStars(activity)
        }
        LinearProgressIndicator(
            progress = { stageProgress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(999.dp)),
            color = tone.accent,
            trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
        )
    }
}

@Composable
private fun StageStars(activity: PracticeActivityItem) {
    val starCount = when {
        activity.difficulty != null && activity.difficulty >= 4 -> 5
        activity.difficulty == 3 -> 4
        else -> 3
    }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(starCount) {
            Text("★", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
private fun MissionBriefStage(
    activity: PracticeActivityItem,
    listeningCapability: ListeningCapability,
    toneColor: Color,
    onStart: () -> Unit
) {
    GlassPanel(accent = toneColor.copy(alpha = 0.26f)) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusPill(text = activity.skill.label, accent = toneColor)
            StatusPill(text = activity.exerciseType.uiLabel(), accent = MaterialTheme.colorScheme.tertiary)
            StatusPill(text = activity.sourceLabel, accent = MaterialTheme.colorScheme.secondary)
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(activity.instructions, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            Text(activity.supportNote, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        MissionPromptCard(
            title = when (activity.skill) {
                SkillType.LISTENING -> "What you need to catch"
                SkillType.SPEAKING -> "What you need to say"
                SkillType.READING -> "What you need to read"
                SkillType.WRITING -> "What you need to argue"
            },
            body = activity.prompt
        )
        ContentProvenanceBlock(
            sourceLabel = activity.sourceLabel,
            collectionTitle = activity.collectionTitle,
            currentTitle = activity.unitTitle ?: activity.title
        )
        MissionMetaRow(activity, listeningCapability)
        GlowButton(text = "Start mission", onClick = onStart, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun MissionMetaRow(activity: PracticeActivityItem, listeningCapability: ListeningCapability) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        activity.minimumWordCount?.let { StatusPill(text = "$it+ words", accent = MaterialTheme.colorScheme.primary) }
        activity.minimumResponseItems?.let { StatusPill(text = "$it+ points", accent = MaterialTheme.colorScheme.primary) }
        if (activity.skill == SkillType.LISTENING) {
            StatusPill(text = engineLabel(listeningCapability.playbackEngine), accent = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun ExerciseStage(
    activity: PracticeActivityItem,
    focusTokens: List<String>,
    listeningCapability: ListeningCapability,
    listeningMode: ListeningPlaybackMode?,
    listeningStatus: String,
    listeningHint: String,
    listeningSourceLabel: String,
    listeningPositionMs: Long,
    listeningDurationMs: Long,
    isListeningPreparing: Boolean,
    isListeningPlaying: Boolean,
    hasStartedListeningPlayback: Boolean,
    hasCompletedListeningPlayback: Boolean,
    hasListeningPlaybackError: Boolean,
    onPlayListening: () -> Unit,
    onPauseListening: () -> Unit,
    onReplayListening: () -> Unit,
    listeningItems: List<PracticeActivityItem>,
    currentListeningIndex: Int,
    showListeningLibrary: Boolean,
    onToggleListeningLibrary: () -> Unit,
    onListeningActivitySelected: (String) -> Unit,
    speakingAvailability: SpeakingAvailability,
    speakingCapability: SpeakingCapability,
    selectedSpeakingLocaleTag: String,
    speakingStatus: String,
    speakingHint: String,
    capturedTranscript: String?,
    showOpenSettings: Boolean,
    isSpeakingBusy: Boolean,
    isSpeakingListening: Boolean,
    isSpeakingProcessing: Boolean,
    canRetrySpeaking: Boolean,
    onSpeakingLocaleSelected: (String) -> Unit,
    onRequestMicPermission: () -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onUseCapturedTranscript: () -> Unit,
    onClearCapturedTranscript: () -> Unit,
    onRetrySpeaking: () -> Unit,
    onOpenSettings: () -> Unit,
    onContinue: () -> Unit
) {
    when (activity.skill) {
        SkillType.LISTENING -> ListeningExerciseStage(
            activity = activity,
            focusTokens = focusTokens,
            listeningCapability = listeningCapability,
            listeningMode = listeningMode,
            listeningStatus = listeningStatus,
            listeningHint = listeningHint,
            listeningSourceLabel = listeningSourceLabel,
            listeningPositionMs = listeningPositionMs,
            listeningDurationMs = listeningDurationMs,
            isListeningPreparing = isListeningPreparing,
            isListeningPlaying = isListeningPlaying,
            hasStartedListeningPlayback = hasStartedListeningPlayback,
            hasCompletedListeningPlayback = hasCompletedListeningPlayback,
            hasListeningPlaybackError = hasListeningPlaybackError,
            onPlayListening = onPlayListening,
            onPauseListening = onPauseListening,
            onReplayListening = onReplayListening,
            listeningItems = listeningItems,
            currentListeningIndex = currentListeningIndex,
            showListeningLibrary = showListeningLibrary,
            onToggleListeningLibrary = onToggleListeningLibrary,
            onListeningActivitySelected = onListeningActivitySelected,
            onContinue = onContinue
        )
        SkillType.SPEAKING -> SpeakingExerciseStage(
            activity = activity,
            focusTokens = focusTokens,
            speakingAvailability = speakingAvailability,
            speakingCapability = speakingCapability,
            selectedSpeakingLocaleTag = selectedSpeakingLocaleTag,
            speakingStatus = speakingStatus,
            speakingHint = speakingHint,
            capturedTranscript = capturedTranscript,
            showOpenSettings = showOpenSettings,
            isSpeakingBusy = isSpeakingBusy,
            isSpeakingListening = isSpeakingListening,
            isSpeakingProcessing = isSpeakingProcessing,
            canRetrySpeaking = canRetrySpeaking,
            onSpeakingLocaleSelected = onSpeakingLocaleSelected,
            onRequestMicPermission = onRequestMicPermission,
            onStartListening = onStartListening,
            onStopListening = onStopListening,
            onUseCapturedTranscript = onUseCapturedTranscript,
            onClearCapturedTranscript = onClearCapturedTranscript,
            onRetrySpeaking = onRetrySpeaking,
            onOpenSettings = onOpenSettings,
            onContinue = onContinue
        )
        SkillType.READING, SkillType.WRITING -> TextExerciseStage(activity = activity, focusTokens = focusTokens, onContinue = onContinue)
    }
}

@Composable
private fun TextExerciseStage(activity: PracticeActivityItem, focusTokens: List<String>, onContinue: () -> Unit) {
    val tone = skillTone(activity.skill)
    GlassPanel(accent = tone.accent.copy(alpha = 0.24f)) {
        Text(
            text = if (activity.skill == SkillType.READING) "Focus on the text" else "Frame your position",
            style = MaterialTheme.typography.labelLarge,
            color = tone.accent
        )
        MissionPromptCard(title = activity.title, body = activity.prompt)
        TokenCloud(tokens = focusTokens, accent = tone.accent)
        GlowButton(text = "Continue to response", onClick = onContinue, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ListeningExerciseStage(
    activity: PracticeActivityItem,
    focusTokens: List<String>,
    listeningCapability: ListeningCapability,
    listeningMode: ListeningPlaybackMode?,
    listeningStatus: String,
    listeningHint: String,
    listeningSourceLabel: String,
    listeningPositionMs: Long,
    listeningDurationMs: Long,
    isListeningPreparing: Boolean,
    isListeningPlaying: Boolean,
    hasStartedListeningPlayback: Boolean,
    hasCompletedListeningPlayback: Boolean,
    hasListeningPlaybackError: Boolean,
    onPlayListening: () -> Unit,
    onPauseListening: () -> Unit,
    onReplayListening: () -> Unit,
    listeningItems: List<PracticeActivityItem>,
    currentListeningIndex: Int,
    showListeningLibrary: Boolean,
    onToggleListeningLibrary: () -> Unit,
    onListeningActivitySelected: (String) -> Unit,
    onContinue: () -> Unit
) {
    val tone = skillTone(activity.skill)
    GlassPanel(accent = tone.accent.copy(alpha = 0.24f)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Listen and catch the core message", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            ListeningOrb(isPlaying = isListeningPlaying, onPlayListening = onPlayListening)
            Text(listeningStatus, style = MaterialTheme.typography.titleMedium, color = tone.accent)
            Text(
                text = listeningHint,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TokenCloud(tokens = focusTokens, accent = tone.accent)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LinearProgressIndicator(
                progress = {
                    if (listeningDurationMs > 0L) {
                        (listeningPositionMs.toFloat() / listeningDurationMs.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(999.dp)),
                color = tone.accent,
                trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
            )
            Text("${formatPlaybackTime(listeningPositionMs)} / ${formatPlaybackTime(listeningDurationMs)}", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlaybackButton(
                    label = when {
                        isListeningPlaying -> "Playing"
                        listeningMode == ListeningPlaybackMode.PROMPT_SYNTHESIS && listeningPositionMs > 0L -> "Play again"
                        else -> "Play"
                    },
                    icon = Icons.Rounded.PlayArrow,
                    enabled = !isListeningPreparing,
                    onClick = onPlayListening,
                    modifier = Modifier.weight(1f)
                )
                PlaybackButton(
                    label = "Pause",
                    icon = Icons.Rounded.Pause,
                    enabled = hasStartedListeningPlayback && !isListeningPreparing,
                    onClick = onPauseListening,
                    modifier = Modifier.weight(1f)
                )
                PlaybackButton(
                    label = "Replay",
                    icon = Icons.Rounded.Replay,
                    enabled = hasStartedListeningPlayback && !isListeningPreparing,
                    onClick = onReplayListening,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        StatusPill(text = "${engineLabel(listeningCapability.playbackEngine)} • $listeningSourceLabel", accent = MaterialTheme.colorScheme.secondary)
        if (listeningItems.isNotEmpty()) {
            Button(onClick = onToggleListeningLibrary, modifier = Modifier.fillMaxWidth()) {
                Text(if (showListeningLibrary) "Hide listening library" else "Open listening library")
            }
        }
        AnimatedVisibility(visible = showListeningLibrary && listeningItems.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (currentListeningIndex >= 0) "Current file ${currentListeningIndex + 1} of ${listeningItems.size}" else "${listeningItems.size} listening files available",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                listeningItems.forEach { listeningItem ->
                    val isCurrent = listeningItem.id == activity.id
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = if (isCurrent) tone.soft else MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = listeningItem.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isCurrent) tone.accent else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = listeningItem.audioAssetPath?.substringAfterLast('/') ?: "Prompt fallback only",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Button(
                                enabled = !isCurrent,
                                onClick = { onListeningActivitySelected(listeningItem.id) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isCurrent) "Current mission" else "Open this mission")
                            }
                        }
                    }
                }
            }
        }
        GlowButton(
            text = if (hasCompletedListeningPlayback || hasListeningPlaybackError) "Continue to summary" else "Continue anyway",
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ListeningOrb(isPlaying: Boolean, onPlayListening: () -> Unit) {
    Surface(onClick = onPlayListening, shape = CircleShape, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), shadowElevation = 12.dp) {
        Box(
            modifier = Modifier.size(112.dp).background(
                Brush.radialGradient(
                    colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f), MaterialTheme.colorScheme.primaryContainer)
                )
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.GraphicEq else Icons.Rounded.PlayArrow,
                contentDescription = "Play listening prompt",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
private fun SpeakingExerciseStage(
    activity: PracticeActivityItem,
    focusTokens: List<String>,
    speakingAvailability: SpeakingAvailability,
    speakingCapability: SpeakingCapability,
    selectedSpeakingLocaleTag: String,
    speakingStatus: String,
    speakingHint: String,
    capturedTranscript: String?,
    showOpenSettings: Boolean,
    isSpeakingBusy: Boolean,
    isSpeakingListening: Boolean,
    isSpeakingProcessing: Boolean,
    canRetrySpeaking: Boolean,
    onSpeakingLocaleSelected: (String) -> Unit,
    onRequestMicPermission: () -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onUseCapturedTranscript: () -> Unit,
    onClearCapturedTranscript: () -> Unit,
    onRetrySpeaking: () -> Unit,
    onOpenSettings: () -> Unit,
    onContinue: () -> Unit
) {
    val tone = skillTone(activity.skill)
    GlassPanel(accent = tone.accent.copy(alpha = 0.24f)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), shadowElevation = 12.dp) {
                Box(
                    modifier = Modifier.size(112.dp).background(
                        Brush.radialGradient(
                            colors = listOf(tone.accent.copy(alpha = 0.95f), MaterialTheme.colorScheme.primaryContainer)
                        )
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Mic, contentDescription = "Speaking capture", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(38.dp))
                }
            }
            Text(speakingStatus, style = MaterialTheme.typography.titleMedium, color = tone.accent)
            Text(speakingHint, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TokenCloud(tokens = focusTokens, accent = tone.accent)
        StatusPill(text = speakingAvailability.uiLabel(), accent = MaterialTheme.colorScheme.secondary)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            speakingCapability.supportedLocales.forEach { localeOption ->
                FilterChip(
                    selected = localeOption.tag == selectedSpeakingLocaleTag,
                    onClick = { onSpeakingLocaleSelected(localeOption.tag) },
                    label = { Text(localeOption.label) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = !isSpeakingBusy,
                onClick = {
                    when (speakingAvailability) {
                        SpeakingAvailability.PERMISSION_REQUIRED -> onRequestMicPermission()
                        SpeakingAvailability.READY -> onStartListening()
                        SpeakingAvailability.UNSUPPORTED -> Unit
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    when {
                        isSpeakingProcessing -> "Processing"
                        isSpeakingListening -> "Listening"
                        else -> "Start listening"
                    }
                )
            }
            Button(enabled = isSpeakingBusy, onClick = onStopListening, modifier = Modifier.weight(1f)) {
                Text("Stop")
            }
        }
        if (showOpenSettings) {
            Button(enabled = !isSpeakingBusy, onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text("Open microphone settings")
            }
        }
        if (capturedTranscript != null) {
            MissionPromptCard(title = "Captured transcript", body = capturedTranscript)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = !isSpeakingBusy, onClick = onUseCapturedTranscript, modifier = Modifier.weight(1f)) {
                    Text("Use transcript")
                }
                Button(enabled = !isSpeakingBusy, onClick = onClearCapturedTranscript, modifier = Modifier.weight(1f)) {
                    Text("Clear")
                }
            }
        }
        if (canRetrySpeaking) {
            Button(onClick = onRetrySpeaking, modifier = Modifier.fillMaxWidth()) {
                Text("Retry capture")
            }
        }
        StatusPill(
            text = "Feedback dimensions: ${speakingCapability.feedbackDimensions.joinToString { prettyListItem(it) }}",
            accent = MaterialTheme.colorScheme.tertiary
        )
        GlowButton(text = "Continue to response", onClick = onContinue, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ResponseStage(
    activity: PracticeActivityItem,
    fieldLabel: String,
    answerText: String,
    onAnswerChanged: (String) -> Unit,
    submitEnabled: Boolean,
    capturedTranscript: String?,
    onBackToExercise: () -> Unit,
    onSubmit: () -> Unit
) {
    val tone = skillTone(activity.skill)
    GlassPanel(accent = tone.accent.copy(alpha = 0.26f)) {
        Text("Shape your final response", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "This is the final drafting step. Keep it focused, then submit for feedback.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        MissionPromptCard(
            title = if (activity.skill == SkillType.SPEAKING && !capturedTranscript.isNullOrBlank()) "Captured transcript" else "Prompt",
            body = if (activity.skill == SkillType.SPEAKING && !capturedTranscript.isNullOrBlank()) capturedTranscript else activity.prompt
        )
        OutlinedTextField(
            value = answerText,
            onValueChange = onAnswerChanged,
            modifier = Modifier.fillMaxWidth(),
            minLines = 8,
            label = { Text(fieldLabel) }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onBackToExercise,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text("Back to task")
            }
            GlowButton(
                text = "Submit for feedback",
                onClick = onSubmit,
                enabled = submitEnabled,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun FeedbackStage(
    activity: PracticeActivityItem,
    lastAttempt: ActivityAttemptRecord?,
    onTryAgain: () -> Unit,
    onBackToPath: () -> Unit
) {
    val tone = skillTone(activity.skill)
    GlassPanel(accent = tone.accent.copy(alpha = 0.26f)) {
        if (lastAttempt == null || lastAttempt.activityId != activity.id) {
            Text("Analyzing your attempt...", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "The result panel will update as soon as the submission is saved.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Mission review", style = MaterialTheme.typography.labelLarge, color = tone.accent)
                    Text("Score ${lastAttempt.score}", style = MaterialTheme.typography.displaySmall)
                }
                ScoreBadge(lastAttempt.score)
            }
            if (lastAttempt.weakTags.isNotEmpty()) {
                TokenCloud(tokens = lastAttempt.weakTags, accent = tone.accent)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Feedback", style = MaterialTheme.typography.titleMedium)
                lastAttempt.feedback.forEach { line ->
                    Text("• $line", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            MissionPromptCard(title = "Model answer", body = activity.modelAnswer)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onTryAgain,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text("Refine response")
            }
            GlowButton(text = "Back to path", onClick = onBackToPath, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ScoreBadge(score: Int) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)) {
        Box(
            modifier = Modifier.size(74.dp).border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.26f),
                shape = CircleShape
            ),
            contentAlignment = Alignment.Center
        ) {
            Text("$score%", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}

@Composable
private fun CompactAttemptSummary(attempt: ActivityAttemptRecord, modelAnswer: String) {
    GlassPanel(accent = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)) {
        Text("Latest saved attempt", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
        Text(
            text = "Score ${attempt.score} • Weak tags: ${attempt.weakTags.joinToString().ifBlank { "None" }}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text("Model answer", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(modelAnswer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MissionPromptCard(title: String, body: String) {
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
            Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}

@Composable
private fun TokenCloud(tokens: List<String>, accent: Color) {
    if (tokens.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Focus on", style = MaterialTheme.typography.labelLarge, color = accent)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tokens.forEach { token ->
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(token) },
                    colors = AssistChipDefaults.assistChipColors(
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.74f),
                        disabledLabelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}

@Composable
private fun PlaybackButton(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(enabled = enabled, onClick = onClick, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = label)
            Text(label)
        }
    }
}

private fun buildFocusTokens(activity: PracticeActivityItem): List<String> {
    val seedTokens = buildList {
        addAll(activity.evaluationTargets)
        addAll(activity.tags)
        activity.minimumWordCount?.let { add("$it+ words") }
        activity.minimumResponseItems?.let { add("$it+ points") }
        if (activity.exerciseType == ExerciseType.LISTEN_AND_SUMMARIZE) {
            add("main position")
            add("contrast")
        }
    }
    return seedTokens.map(String::trim).filter(String::isNotBlank).distinct().take(8)
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

private fun formatPlaybackTime(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(minutes, seconds)
}
