package com.example.englishpractice.ui.screens.review

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.englishpractice.ui.app.AppUiState
import com.example.englishpractice.ui.app.ReviewQueueItem
import com.example.englishpractice.ui.components.ContentProvenanceBlock
import com.example.englishpractice.ui.components.skillTone

@Composable
fun ReviewScreen(
    state: AppUiState,
    onOpenReviewActivity: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        ReviewHero(state = state)

        SectionHeading(
            eyebrow = "Queue",
            title = "Retry order",
            description = "These items are already ranked by urgency and recent weakness, so you can resume the right one quickly."
        )

        state.reviewQueue.forEachIndexed { index, item ->
            ReviewQueueCard(
                index = index,
                item = item,
                onOpenReviewActivity = onOpenReviewActivity
            )
        }
    }
}

@Composable
private fun ReviewHero(state: AppUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Review loop",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Turn weak attempts into deliberate retries instead of letting them disappear into history.",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Due today ${state.reviewSummary.dueToday}  |  Recurring patterns ${state.reviewSummary.recurringPatterns}  |  Next checkpoint ${state.reviewSummary.nextCheckpointDays} days",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReviewQueueCard(
    index: Int,
    item: ReviewQueueItem,
    onOpenReviewActivity: (String) -> Unit
) {
    val tone = skillTone(item.skill)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenReviewActivity(item.activityId) },
        colors = CardDefaults.cardColors(containerColor = reviewCardColor(item.dueLabel))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Retry ${index + 1}  |  ${item.skill.label}  |  ${item.dueLabel}",
                style = MaterialTheme.typography.labelLarge,
                color = tone.accent
            )
            Text(item.title, style = MaterialTheme.typography.titleLarge)
            ContentProvenanceBlock(
                sourceLabel = item.sourceLabel,
                collectionTitle = item.collectionTitle,
                unitTitle = item.unitTitle,
                currentTitle = item.title
            )
            Text(
                text = item.prompt,
                style = MaterialTheme.typography.bodyMedium
            )
            item.lastScore?.let { score ->
                Text(
                    text = "Last score $score",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            if (item.weakTags.isNotEmpty()) {
                Text(
                    text = "Weak tags: ${item.weakTags.joinToString()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = item.reason,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            Button(onClick = { onOpenReviewActivity(item.activityId) }) {
                Text("Open retry")
            }
        }
    }
}

@Composable
private fun SectionHeading(
    eyebrow: String,
    title: String,
    description: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = eyebrow,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun reviewCardColor(dueLabel: String) = when (dueLabel) {
    "Due now" -> MaterialTheme.colorScheme.errorContainer
    "Today" -> MaterialTheme.colorScheme.secondaryContainer
    else -> MaterialTheme.colorScheme.tertiaryContainer
}
