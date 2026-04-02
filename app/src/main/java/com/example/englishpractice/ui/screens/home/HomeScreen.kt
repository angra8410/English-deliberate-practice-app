package com.example.englishpractice.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.englishpractice.ui.app.AppUiState

@Composable
fun HomeScreen(
    state: AppUiState,
    onStartPractice: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Today", style = MaterialTheme.typography.headlineMedium)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "B2 to C1 deliberate practice",
                    style = MaterialTheme.typography.titleLarge
                )
                Text("Current level: ${state.currentLevel}  Target: ${state.targetLevel}")
                Text("Pilot content: ${state.pilotLevels.joinToString()}")
                Text("Daily goal: ${state.dailyGoalMinutes} minutes across four skills")
                LinearProgressIndicator(
                    progress = { state.overallCompletion / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Program completion: ${state.overallCompletion}%")
                Button(onClick = onStartPractice) {
                    Text("Start practice")
                }
            }
        }

        Text("Daily loop", style = MaterialTheme.typography.titleMedium)
        state.dailyPlan.forEach { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(item.skill.label, style = MaterialTheme.typography.labelLarge)
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    Text(item.focus)
                    Text(
                        text = "${item.exerciseType}  |  ${item.estimatedMinutes} min",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Source: ${item.sourceLabel}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Text("Weak patterns to review", style = MaterialTheme.typography.titleMedium)
        state.weakPatterns.forEach { pattern ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${pattern.skill.label}: ${pattern.tag}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(pattern.note)
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Speaking support", style = MaterialTheme.typography.titleMedium)
                Text("Status: ${state.speakingCapability.availability}")
                Text(
                    "Flow: ${state.speakingCapability.sessionFlow.joinToString(separator = " -> ")}"
                )
                Text("Feedback: ${state.speakingCapability.feedbackDimensions.joinToString()}")
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Listening support", style = MaterialTheme.typography.titleMedium)
                Text("Engine: ${state.listeningCapability.playbackEngine}")
                Text(
                    "Flow: ${state.listeningCapability.workflowSteps.joinToString(separator = " -> ")}"
                )
            }
        }
    }
}
