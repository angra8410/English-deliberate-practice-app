package com.example.englishpractice.ui.screens.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.englishpractice.ui.app.AppUiState

@Composable
fun ReviewScreen(state: AppUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Review", style = MaterialTheme.typography.headlineMedium)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Due today: ${state.reviewSummary.dueToday}")
                Text("Recurring patterns: ${state.reviewSummary.recurringPatterns}")
                Text("Next checkpoint: ${state.reviewSummary.nextCheckpointDays} days")
            }
        }

        Text("Retry queue", style = MaterialTheme.typography.titleMedium)
        state.reviewQueue.forEach { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${item.skill.label}  |  ${item.dueLabel}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(item.prompt)
                    Text(item.reason, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
