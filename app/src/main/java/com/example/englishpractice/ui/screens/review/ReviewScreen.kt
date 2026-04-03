package com.example.englishpractice.ui.screens.review

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.englishpractice.ui.app.AppUiState
import com.example.englishpractice.ui.app.ReviewQueueItem
import com.example.englishpractice.ui.components.ContentProvenanceBlock
import com.example.englishpractice.ui.components.GlassPanel
import com.example.englishpractice.ui.components.GlowButton
import com.example.englishpractice.ui.components.ImmersiveScreen
import com.example.englishpractice.ui.components.StatusPill
import com.example.englishpractice.ui.components.skillTone

@Composable
fun ReviewScreen(
    state: AppUiState,
    onOpenReviewActivity: (String) -> Unit = {}
) {
    ImmersiveScreen {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatusPill(text = "${state.reviewSummary.dueToday} due today")
            StatusPill(
                text = "${state.reviewSummary.recurringPatterns} patterns",
                accent = MaterialTheme.colorScheme.tertiary
            )
            StatusPill(
                text = "Next check ${state.reviewSummary.nextCheckpointDays}d",
                accent = MaterialTheme.colorScheme.secondary
            )
        }

        GlassPanel {
            Text(
                text = "Review",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = "Clean retries, clearer priorities",
                style = MaterialTheme.typography.displaySmall
            )
            Text(
                text = "The queue now highlights the exact item worth reopening instead of forcing you through a noisy list.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        ReviewQueuePanel(
            items = state.reviewQueue,
            onOpenReviewActivity = onOpenReviewActivity
        )
    }
}

@Composable
private fun ReviewQueuePanel(
    items: List<ReviewQueueItem>,
    onOpenReviewActivity: (String) -> Unit
) {
    GlassPanel {
        Text(
            text = "Retry queue",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        items.forEachIndexed { index, item ->
            if (index > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
            }
            ReviewRow(
                item = item,
                index = index,
                onOpenReviewActivity = onOpenReviewActivity
            )
        }
    }
}

@Composable
private fun ReviewRow(
    item: ReviewQueueItem,
    index: Int,
    onOpenReviewActivity: (String) -> Unit
) {
    val tone = skillTone(item.skill)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenReviewActivity(item.activityId) }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(tone.gradient),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.skill.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = tone.accent
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = reviewChipColor(item.dueLabel)
                ) {
                    Text(
                        text = item.dueLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = item.reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (item.weakTags.isNotEmpty()) {
                Text(
                    text = "Weak tags: ${item.weakTags.joinToString()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ContentProvenanceBlock(
                sourceLabel = item.sourceLabel,
                collectionTitle = item.collectionTitle,
                unitTitle = item.unitTitle,
                currentTitle = item.title
            )
        }
        GlowButton(
            text = "Open",
            onClick = { onOpenReviewActivity(item.activityId) }
        )
    }
}

@Composable
private fun reviewChipColor(dueLabel: String) = when (dueLabel) {
    "Due now" -> MaterialTheme.colorScheme.errorContainer
    "Today" -> MaterialTheme.colorScheme.secondaryContainer
    else -> MaterialTheme.colorScheme.tertiaryContainer
}
