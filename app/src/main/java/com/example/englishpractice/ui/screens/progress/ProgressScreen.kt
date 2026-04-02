package com.example.englishpractice.ui.screens.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.englishpractice.ui.app.AppUiState

@Composable
fun ProgressScreen(state: AppUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Progress", style = MaterialTheme.typography.headlineMedium)
        Text("Current level: ${state.currentLevel} -> ${state.targetLevel}")
        Text("Streak: ${state.streakDays} days")

        state.skillProgress.forEach { progress ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(progress.skill.label, style = MaterialTheme.typography.titleMedium)
                    LinearProgressIndicator(
                        progress = { progress.completionPercent / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Completion: ${progress.completionPercent}%")
                    Text("Average score: ${progress.averageScore}%")
                    Text("Weak tags: ${progress.weakTags.joinToString()}")
                }
            }
        }
    }
}
