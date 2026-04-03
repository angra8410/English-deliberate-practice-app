package com.example.englishpractice.ui.screens.activity

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.example.englishpractice.domain.model.SkillType
import com.example.englishpractice.feature.listening.ListeningCapability
import com.example.englishpractice.feature.listening.ListeningPlaybackMode
import com.example.englishpractice.feature.listening.ListeningPlaybackState
import com.example.englishpractice.feature.listening.ListeningPlayer
import com.example.englishpractice.feature.speaking.SpeakingAvailability
import com.example.englishpractice.feature.speaking.SpeakingCapability
import com.example.englishpractice.feature.speaking.SpeakingCaptureState
import com.example.englishpractice.feature.speaking.SpeakingManager
import com.example.englishpractice.ui.app.ActivityAttemptRecord
import com.example.englishpractice.ui.app.PracticeActivityItem
import com.example.englishpractice.ui.components.ContentProvenanceBlock
import com.example.englishpractice.ui.components.engineLabel
import com.example.englishpractice.ui.components.GlassPanel
import com.example.englishpractice.ui.components.GlowButton
import com.example.englishpractice.ui.components.ImmersiveScreen
import com.example.englishpractice.ui.components.prettyListItem
import com.example.englishpractice.ui.components.StatusPill
import com.example.englishpractice.ui.components.skillTone
import com.example.englishpractice.ui.components.uiEnabledLabel
import com.example.englishpractice.ui.components.uiLabel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun ActivityPlayerScreen(
    activity: PracticeActivityItem?,
    availableListeningActivities: List<PracticeActivityItem>,
    lastAttempt: ActivityAttemptRecord?,
    selectedSpeakingLocaleTag: String,
    speakingCapability: SpeakingCapability,
    listeningCapability: ListeningCapability,
    onSpeakingLocaleSelected: (String) -> Unit,
    onListeningActivitySelected: (String) -> Unit,
    onSubmit: (answer: String, transcriptText: String?) -> Unit
) {
    if (activity == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("The selected activity could not be loaded.")
        }
        return
    }

    var answerText by rememberSaveable(activity.id) { mutableStateOf(activity.starterText) }
    var listeningMode by rememberSaveable(activity.id) { mutableStateOf<ListeningPlaybackMode?>(null) }
    var listeningStatus by rememberSaveable(activity.id) { mutableStateOf("Idle") }
    var listeningHint by rememberSaveable(activity.id) {
        mutableStateOf("Press Play to hear the listening prompt before writing your summary.")
    }
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
    var speakingAvailability by rememberSaveable(activity.id) {
        mutableStateOf(speakingCapability.availability)
    }
    var speakingHint by rememberSaveable(activity.id) {
        mutableStateOf("Tap Start listening, speak for a few seconds, then review the transcript.")
    }
    var showListeningLibrary by rememberSaveable(activity.id) { mutableStateOf(false) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val listeningPlayer = remember(context) { ListeningPlayer(context) }
    val speakingManager = remember(context) { SpeakingManager(context) }
    val isListeningBusy = isListeningPreparing || isListeningPlaying
    val isSpeakingBusy = isSpeakingListening || isSpeakingProcessing
    val tone = skillTone(activity.skill)

    fun applyTranscript(transcript: String) {
        val safeTranscript = transcript.trim()
        if (safeTranscript.isBlank()) return

        val canOverwriteDraft = answerText.isBlank() ||
            answerText == activity.starterText ||
            answerText == lastAppliedTranscript

        capturedTranscript = safeTranscript
        if (canOverwriteDraft) {
            answerText = safeTranscript
        }
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
                listeningHint = "Press Play to hear the listening prompt before writing your summary."
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
                listeningHint = when (state.mode) {
                    ListeningPlaybackMode.BUNDLED_AUDIO -> {
                        "Bundled audio is loading. Playback controls will activate when it is ready."
                    }

                    ListeningPlaybackMode.PROMPT_SYNTHESIS -> {
                        "Prompt playback is preparing. The current English voice will be used for synthesis."
                    }
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
                listeningHint = when (state.mode) {
                    ListeningPlaybackMode.BUNDLED_AUDIO -> {
                        "Play the listening audio, then capture the speaker's conclusion and contrast."
                    }

                    ListeningPlaybackMode.PROMPT_SYNTHESIS -> {
                        "Play the spoken prompt, then summarize the final position and contrasting detail."
                    }
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
                listeningHint = "Listen through the full prompt before writing the summary."
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
                listeningHint = when (state.mode) {
                    ListeningPlaybackMode.BUNDLED_AUDIO -> {
                        "Resume from Play or restart from Replay before submitting your answer."
                    }

                    ListeningPlaybackMode.PROMPT_SYNTHESIS -> {
                        "Prompt synthesis will start again from the beginning when you press Play or Replay."
                    }
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
                listeningHint = "Write the speaker's main position and one clear contrast while it is still fresh."
            }

            is ListeningPlaybackState.Error -> {
                listeningStatus = state.message
                isListeningPreparing = false
                isListeningPlaying = false
                hasListeningPlaybackError = true
                listeningHint = "Retry playback or continue with the written prompt if audio is unavailable."
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
                speakingHint = "The recognizer is idle. Start a new capture when you are ready."
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
                speakingHint = "Keep speaking until you finish one complete answer."
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
                speakingHint = "Partial speech was captured. Keep talking or stop when you finish."
            }

            is SpeakingCaptureState.TranscriptReady -> {
                speakingStatus = "Transcript captured"
                isSpeakingListening = false
                isSpeakingProcessing = false
                applyTranscript(state.transcript)
                canRetrySpeaking = false
                speakingHint = "Review the transcript, adjust it if needed, then submit for feedback."
            }

            is SpeakingCaptureState.Error -> {
                speakingStatus = state.message
                isSpeakingListening = false
                isSpeakingProcessing = false
                canRetrySpeaking = !state.message.contains("Retrying once automatically")
                if (!state.partialTranscript.isNullOrBlank()) {
                    applyTranscript(state.partialTranscript)
                }
                speakingHint = when {
                    canRetrySpeaking -> "You can retry immediately or keep editing the saved transcript."
                    else -> "The recognizer is recovering automatically."
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

    DisposableEffect(speakingManager, activity.skill) {
        if (activity.skill == SkillType.SPEAKING) {
            refreshSpeakingReadiness()
        }
        onDispose {
            speakingManager.release()
        }
    }

    DisposableEffect(
        listeningPlayer,
        activity.skill,
        activity.audioAssetPath,
        activity.listeningPromptText,
        selectedSpeakingLocaleTag
    ) {
        if (activity.skill == SkillType.LISTENING) {
            refreshListeningPlayback()
        }
        onDispose {
            listeningPlayer.release()
        }
    }

    DisposableEffect(lifecycleOwner, speakingManager, activity.skill) {
        if (activity.skill != SkillType.SPEAKING) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    refreshSpeakingReadiness()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showOpenSettings = false
            refreshSpeakingReadiness()
            speakingManager.startListening(selectedSpeakingLocaleTag, ::handleSpeakingState)
        } else {
            speakingAvailability = speakingManager.capability().availability
            val hostActivity = context.findActivity()
            val permanentlyDenied = hasRequestedMicPermission &&
                hostActivity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(
                    hostActivity,
                    Manifest.permission.RECORD_AUDIO
                )

            showOpenSettings = permanentlyDenied
            speakingStatus = if (permanentlyDenied) {
                "Microphone permission is blocked"
            } else {
                "Microphone permission denied"
            }
            canRetrySpeaking = false
            speakingHint = if (permanentlyDenied) {
                "Open app settings, enable microphone access, then return to start speaking."
            } else {
                "Allow microphone permission to capture speech, or keep practicing with typed answers."
            }
        }
    }

    val fieldLabel = when (activity.skill) {
        SkillType.SPEAKING -> "Transcript or speaking notes"
        SkillType.LISTENING -> "Listening summary"
        else -> "Your response"
    }

    ImmersiveScreen {
        GlassPanel(accent = tone.accent.copy(alpha = 0.32f)) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusPill(text = activity.skill.label, accent = tone.accent)
                StatusPill(text = activity.exerciseType.uiLabel(), accent = MaterialTheme.colorScheme.tertiary)
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(activity.title, style = MaterialTheme.typography.headlineMedium)
                Text(
                    activity.instructions,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ContentProvenanceBlock(
                    sourceLabel = activity.sourceLabel,
                    collectionTitle = activity.collectionTitle,
                    unitTitle = activity.unitTitle,
                    currentTitle = activity.title
                )
            }
        }

        GlassPanel(accent = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)) {
            Text(
                text = "Prompt",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(activity.prompt)
            Text(activity.supportNote, style = MaterialTheme.typography.bodySmall)
        }

        if (activity.skill == SkillType.LISTENING) {
            GlassPanel(accent = tone.accent.copy(alpha = 0.26f)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Listening playback",
                        style = MaterialTheme.typography.titleMedium,
                        color = tone.accent
                    )
                    Text("Engine: ${engineLabel(listeningCapability.playbackEngine)}")
                    Text("Source: $listeningSourceLabel")
                    Text("Status: $listeningStatus")
                    Text(listeningHint, style = MaterialTheme.typography.bodySmall)
                    LinearProgressIndicator(
                        progress = {
                            if (listeningDurationMs > 0L) {
                                (listeningPositionMs.toFloat() / listeningDurationMs.toFloat())
                                    .coerceIn(0f, 1f)
                            } else {
                                0f
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "${formatPlaybackTime(listeningPositionMs)} / ${formatPlaybackTime(listeningDurationMs)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = !isListeningPreparing,
                            onClick = { listeningPlayer.play() }
                        ) {
                            Text(
                                when {
                                    isListeningPlaying -> "Playing..."
                                    listeningMode == ListeningPlaybackMode.PROMPT_SYNTHESIS &&
                                        listeningPositionMs > 0L -> "Play again"
                                    else -> "Play"
                                }
                            )
                        }
                        Button(
                            enabled = hasStartedListeningPlayback && !isListeningPreparing,
                            onClick = { listeningPlayer.pause() }
                        ) {
                            Text("Pause")
                        }
                        Button(
                            enabled = hasStartedListeningPlayback && !isListeningPreparing,
                            onClick = { listeningPlayer.replay() }
                        ) {
                            Text("Replay")
                        }
                    }
                    Text(
                        if (hasListeningPlaybackError) {
                            "Audio is unavailable right now. You can continue with the written prompt and still submit your summary."
                        } else if (hasCompletedListeningPlayback) {
                            "Playback finished. You can replay it or submit your summary."
                        } else {
                            "Listen at least once before submitting the listening response."
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            GlassPanel(accent = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)) {
                val listeningItems = availableListeningActivities.sortedWith(
                    compareBy<PracticeActivityItem>(
                        { it.collectionTitle ?: it.sourceLabel },
                        { it.unitTitle ?: it.title },
                        { it.title }
                    )
                )
                val availableAssetCount = listeningItems.count { !it.audioAssetPath.isNullOrBlank() }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Listening library for this level",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        "$availableAssetCount bundled files across ${listeningItems.size} listening activities.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { showListeningLibrary = !showListeningLibrary }
                    ) {
                        Text(if (showListeningLibrary) "Hide listening files" else "Show listening files")
                    }

                    if (showListeningLibrary) {
                        listeningItems.forEach { listeningItem ->
                            val isCurrent = listeningItem.id == activity.id
                            val audioLabel = listeningItem.audioAssetPath?.substringAfterLast('/')
                                ?: "No bundled file"
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCurrent) {
                                        tone.soft
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        listeningItem.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (isCurrent) tone.accent else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        audioLabel,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        "Source: ${listeningItem.sourceLabel}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    listeningItem.unitTitle?.let { unitTitle ->
                                        Text(
                                            unitTitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        AssistChip(
                                            onClick = {},
                                            enabled = false,
                                            label = { Text(if (isCurrent) "Current activity" else "Available") },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = if (isCurrent) tone.soft else MaterialTheme.colorScheme.surfaceVariant,
                                                labelColor = if (isCurrent) tone.accent else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                        Button(
                                            enabled = !isCurrent,
                                            onClick = { onListeningActivitySelected(listeningItem.id) }
                                        ) {
                                            Text(if (isCurrent) "Open now" else "Open this file")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (activity.skill == SkillType.SPEAKING) {
            GlassPanel(accent = tone.accent.copy(alpha = 0.26f)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Speaking capture",
                        style = MaterialTheme.typography.titleMedium,
                        color = tone.accent
                    )
                    Text("Availability: ${speakingAvailability.uiLabel()}")
                    Text("Status: $speakingStatus")
                    Text(speakingHint, style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "Locale: ${
                            speakingCapability.supportedLocales.firstOrNull { option ->
                                option.tag == selectedSpeakingLocaleTag
                            }?.label ?: selectedSpeakingLocaleTag
                        }"
                    )
                    Text("Feedback dimensions: ${speakingCapability.feedbackDimensions.joinToString { prettyListItem(it) }}")
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        speakingCapability.supportedLocales.forEach { localeOption ->
                            FilterChip(
                                selected = localeOption.tag == selectedSpeakingLocaleTag,
                                onClick = { onSpeakingLocaleSelected(localeOption.tag) },
                                label = { Text(localeOption.label) }
                            )
                        }
                    }
                    Button(
                        enabled = !isSpeakingBusy,
                        onClick = {
                            when (speakingAvailability) {
                                SpeakingAvailability.PERMISSION_REQUIRED -> {
                                    hasRequestedMicPermission = true
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }

                                SpeakingAvailability.READY -> {
                                    showOpenSettings = false
                                    speakingManager.startListening(
                                        selectedSpeakingLocaleTag,
                                        ::handleSpeakingState
                                    )
                                }

                                SpeakingAvailability.UNSUPPORTED -> {
                                    speakingStatus = "Speech recognition is unavailable"
                                    speakingHint = "This device cannot run speech recognition, so use typed speaking practice."
                                }
                            }
                        }
                    ) {
                        Text(
                            when {
                                isSpeakingProcessing -> "Processing..."
                                isSpeakingListening -> "Listening..."
                                else -> "Start listening"
                            }
                        )
                    }
                    Button(
                        enabled = isSpeakingBusy,
                        onClick = {
                            speakingManager.stopListening()
                            speakingStatus = "Stopped"
                            isSpeakingListening = false
                            isSpeakingProcessing = false
                            speakingHint = "Capture stopped. You can review the transcript or start another attempt."
                        }
                    ) {
                        Text("Stop listening")
                    }
                    Button(onClick = { answerText = activity.starterText }) {
                        Text("Load guided transcript")
                    }
                    if (showOpenSettings) {
                        Button(
                            enabled = !isSpeakingBusy,
                            onClick = {
                                val appSettingsIntent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null)
                                )
                                context.startActivity(appSettingsIntent)
                            }
                        ) {
                            Text("Open microphone settings")
                        }
                    }
                    if (!capturedTranscript.isNullOrBlank() && answerText != capturedTranscript) {
                        Button(
                            enabled = !isSpeakingBusy,
                            onClick = {
                                answerText = capturedTranscript!!
                                lastAppliedTranscript = capturedTranscript
                            }
                        ) {
                            Text("Use captured transcript")
                        }
                    }
                    if (!capturedTranscript.isNullOrBlank()) {
                        Button(
                            enabled = !isSpeakingBusy,
                            onClick = {
                                if (answerText == capturedTranscript) {
                                    answerText = ""
                                }
                                capturedTranscript = null
                                lastAppliedTranscript = null
                                canRetrySpeaking = false
                                speakingStatus = "Transcript cleared"
                                speakingHint = "Start a fresh capture or type your answer manually."
                            }
                        ) {
                            Text("Clear captured transcript")
                        }
                    }
                    if (canRetrySpeaking) {
                        Button(
                            onClick = {
                                capturedTranscript = null
                                lastAppliedTranscript = null
                                speakingManager.startListening(
                                    selectedSpeakingLocaleTag,
                                    ::handleSpeakingState
                                )
                            }
                        ) {
                            Text("Retry again")
                        }
                    }
                    if (!capturedTranscript.isNullOrBlank()) {
                        Text("Captured transcript", style = MaterialTheme.typography.titleSmall)
                        Text(capturedTranscript!!)
                        Text(
                            "Words: ${capturedTranscript!!.split(Regex("\\s+")).filter { token -> token.isNotBlank() }.size}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        GlassPanel(accent = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Response workspace",
                    style = MaterialTheme.typography.titleMedium,
                    color = tone.accent
                )
                OutlinedTextField(
                    value = answerText,
                    onValueChange = { answerText = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 8,
                    label = { Text(fieldLabel) }
                )

                GlowButton(
                    text = "Submit and get feedback",
                    enabled = when (activity.skill) {
                        SkillType.SPEAKING -> !isSpeakingBusy && answerText.isNotBlank()
                        SkillType.LISTENING -> ListeningSubmissionPolicy.canSubmit(
                            isPreparing = isListeningPreparing,
                            hasStartedPlayback = hasStartedListeningPlayback,
                            hasPlaybackError = hasListeningPlaybackError,
                            answerText = answerText
                        )
                        else -> answerText.isNotBlank()
                    },
                    onClick = {
                        val transcript = if (activity.skill == SkillType.SPEAKING) {
                            capturedTranscript ?: answerText
                        } else {
                            null
                        }
                        onSubmit(answerText, transcript)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (lastAttempt != null && lastAttempt.activityId == activity.id) {
            GlassPanel(accent = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.24f)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Latest attempt",
                        style = MaterialTheme.typography.titleMedium,
                        color = tone.accent
                    )
                    Text("Score: ${lastAttempt.score}")
                    Text("Weak tags: ${lastAttempt.weakTags.joinToString().ifBlank { "None" }}")
                    lastAttempt.feedback.forEach { line ->
                        Text("- $line")
                    }
                    Text("Model answer", style = MaterialTheme.typography.titleSmall)
                    Text(activity.modelAnswer)
                }
            }
        }
    }
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
