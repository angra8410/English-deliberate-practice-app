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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.englishpractice.ui.app.AppUiState

@Composable
fun SettingsScreen(
    state: AppUiState,
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Speaking input", style = MaterialTheme.typography.titleMedium)
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
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Prompt playback", style = MaterialTheme.typography.titleMedium)
                Text("Listening engine: ${state.listeningCapability.playbackEngine}")
                Text("Enable text-to-speech and prompt playback")
                Switch(
                    checked = ttsEnabled.value,
                    onCheckedChange = { ttsEnabled.value = it }
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("MVP scope", style = MaterialTheme.typography.titleMedium)
                Text("Levels: ${state.pilotLevels.joinToString()}")
                Text("Daily structure: Reading, Writing, Listening, Speaking")
                Text("Speaking v1: transcript-first feedback with model answer comparison")
            }
        }
    }
}
