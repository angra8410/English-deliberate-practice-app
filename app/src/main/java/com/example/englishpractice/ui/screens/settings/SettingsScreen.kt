package com.example.englishpractice.ui.screens.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.englishpractice.domain.model.CefrLevel
import com.example.englishpractice.ui.app.AppUiState

@Composable
fun SettingsScreen(
    state: AppUiState,
    onPilotLevelSelected: (CefrLevel) -> Unit,
    onSpeakingLocaleSelected: (String) -> Unit
) {
    val speechEnabled = remember { mutableStateOf(true) }
    val ttsEnabled = remember { mutableStateOf(true) }
    val selectedLocale = state.speakingCapability.supportedLocales.firstOrNull { locale ->
        locale.tag == state.selectedSpeakingLocaleTag
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Tune the app around your current level, speaking locale, and capture preferences without breaking the deliberate-practice flow.",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Pilot levels ${state.pilotLevels.joinToString()}  |  Current ${state.currentLevel}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SettingsPanel(title = "Content level", description = "Choose the active pilot level for the app catalog.") {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.pilotLevels.forEach { level ->
                    FilterChip(
                        selected = level == state.currentLevel,
                        onClick = { onPilotLevelSelected(level) },
                        label = { Text(level.name) }
                    )
                }
            }
        }

        SettingsPanel(title = "Speaking input", description = "Speech recognition and transcript-first speaking practice.") {
            Text("Status: ${state.speakingCapability.availability}")
            Text("Speech recognizer: ${state.speakingCapability.usesSpeechRecognizer}")
            Text("Speaking locale: ${selectedLocale?.label ?: state.selectedSpeakingLocaleTag}")
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.speakingCapability.supportedLocales.forEach { locale ->
                    FilterChip(
                        selected = locale.tag == state.selectedSpeakingLocaleTag,
                        onClick = { onSpeakingLocaleSelected(locale.tag) },
                        label = { Text(locale.label) }
                    )
                }
            }
            Text("Enable microphone-based speaking practice")
            Switch(
                checked = speechEnabled.value,
                onCheckedChange = { speechEnabled.value = it }
            )
        }

        SettingsPanel(title = "Prompt playback", description = "Text-to-speech and bundled listening playback.") {
            Text("Listening engine: ${state.listeningCapability.playbackEngine}")
            Text("Enable prompt playback")
            Switch(
                checked = ttsEnabled.value,
                onCheckedChange = { ttsEnabled.value = it }
            )
        }

        SettingsPanel(title = "Current scope", description = "A quick reminder of what the MVP supports right now.") {
            Text("Daily structure: Reading, Writing, Listening, Speaking")
            Text("Speaking v1: transcript-first feedback with model answer comparison")
            Text("Content sources: built-in assets plus seeded book catalog")
        }
    }
}

@Composable
private fun SettingsPanel(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
    }
}
