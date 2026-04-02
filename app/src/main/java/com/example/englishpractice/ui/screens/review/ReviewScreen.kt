package com.example.englishpractice.ui.screens.review

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.englishpractice.ui.app.AppUiState
import com.example.englishpractice.ui.components.ContentProvenanceBlock

@Composable
fun ReviewScreen(
    state: AppUiState,
    onOpenReviewActivity: (String) -> Unit = {}
) {
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
        state.reviewQueue.forEachIndexed { index, item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenReviewActivity(item.activityId) },
                colors = CardDefaults.cardColors(
                    containerColor = reviewCardColor(item.dueLabel)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = reviewOrderLabel(index),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${item.skill.label}  |  ${item.dueLabel}",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    ContentProvenanceBlock(
                        sourceLabel = item.sourceLabel,
                        collectionTitle = item.collectionTitle,
                        unitTitle = item.unitTitle,
                        currentTitle = item.title
                    )
                    Text(item.prompt)
                    item.lastScore?.let { score ->
                        Text("Last score: $score", style = MaterialTheme.typography.bodySmall)
                    }
                    if (item.weakTags.isNotEmpty()) {
                        Text(
                            "Weak tags: ${item.weakTags.joinToString()}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(item.reason, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun reviewCardColor(dueLabel: String) = when (dueLabel) {
    "Due now" -> MaterialTheme.colorScheme.errorContainer
    "Today" -> MaterialTheme.colorScheme.tertiaryContainer
    else -> MaterialTheme.colorScheme.surfaceVariant
}

private fun reviewOrderLabel(index: Int): String = "Retry ${index + 1}"
