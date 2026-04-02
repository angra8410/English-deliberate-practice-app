package com.example.englishpractice.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.englishpractice.domain.model.CefrLevel
import com.example.englishpractice.ui.app.AppUiState
import com.example.englishpractice.ui.app.DailyPracticeItem
import com.example.englishpractice.ui.app.ReviewQueueItem
import com.example.englishpractice.ui.app.WeakPattern
import com.example.englishpractice.ui.components.ContentProvenanceBlock
import com.example.englishpractice.ui.components.skillTone

@Composable
fun HomeScreen(
    state: AppUiState,
    onPilotLevelSelected: (CefrLevel) -> Unit,
    onStartPractice: () -> Unit,
    onBrowseContent: () -> Unit,
    onResumeReview: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        HeroPanel(
            state = state,
            onPilotLevelSelected = onPilotLevelSelected,
            onStartPractice = onStartPractice,
            onBrowseContent = onBrowseContent
        )

        state.reviewQueue.firstOrNull()?.let { nextReview ->
            ReviewHighlightCard(
                item = nextReview,
                onResumeReview = onResumeReview
            )
        }

        SectionHeading(
            eyebrow = "Today",
            title = "Deliberate loop",
            description = "One focused pass through each skill, with clearer pacing and stronger visual cues."
        )
        state.dailyPlan.forEach { item ->
            DailyLoopCard(item = item)
        }

        SectionHeading(
            eyebrow = "Watch list",
            title = "Weak patterns",
            description = "These are the habits worth tightening before they harden into default responses."
        )
        state.weakPatterns.forEach { pattern ->
            WeakPatternCard(pattern = pattern)
        }

        SectionHeading(
            eyebrow = "Support",
            title = "Capture and playback",
            description = "Your speaking and listening tools are active and ready for deliberate reps."
        )
        SupportCard(
            title = "Speaking capture",
            accent = MaterialTheme.colorScheme.secondary,
            lines = listOf(
                "Status: ${state.speakingCapability.availability}",
                "Flow: ${state.speakingCapability.sessionFlow.joinToString(separator = " -> ")}",
                "Feedback: ${state.speakingCapability.feedbackDimensions.joinToString()}"
            )
        )
        SupportCard(
            title = "Listening playback",
            accent = MaterialTheme.colorScheme.tertiary,
            lines = listOf(
                "Engine: ${state.listeningCapability.playbackEngine}",
                "Flow: ${state.listeningCapability.workflowSteps.joinToString(separator = " -> ")}"
            )
        )
    }
}

@Composable
private fun HeroPanel(
    state: AppUiState,
    onPilotLevelSelected: (CefrLevel) -> Unit,
    onStartPractice: () -> Unit,
    onBrowseContent: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.tertiaryContainer,
                        MaterialTheme.colorScheme.secondaryContainer
                    )
                )
            )
            .padding(22.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "Daily deliberate practice",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Build a sharper C1 routine with one compact, high-quality loop.",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Current level ${state.currentLevel}  |  Target ${state.targetLevel}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(
                    modifier = Modifier.weight(1f),
                    label = "Streak",
                    value = "${state.streakDays}d"
                )
                MetricTile(
                    modifier = Modifier.weight(1f),
                    label = "Goal",
                    value = "${state.dailyGoalMinutes}m"
                )
                MetricTile(
                    modifier = Modifier.weight(1f),
                    label = "Complete",
                    value = "${state.overallCompletion}%"
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(
                    progress = { state.overallCompletion / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)
                )
                Text(
                    text = "Program completion",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                )
            }

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

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onStartPractice,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Start today’s loop")
                }
                Button(
                    onClick = onBrowseContent,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text("Browse catalog")
                }
            }
        }
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.62f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ReviewHighlightCard(
    item: ReviewQueueItem,
    onResumeReview: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = reviewCardColor(item.dueLabel)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Best next retry",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${item.skill.label}  |  ${item.dueLabel}",
                style = MaterialTheme.typography.titleSmall
            )
            Text(item.title, style = MaterialTheme.typography.titleLarge)
            ContentProvenanceBlock(
                sourceLabel = item.sourceLabel,
                collectionTitle = item.collectionTitle,
                unitTitle = item.unitTitle,
                currentTitle = item.title
            )
            Text(item.reason, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "Weak tags: ${item.weakTags.joinToString()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            item.lastScore?.let { score ->
                Text(
                    text = "Last score $score",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = { onResumeReview(item.activityId) }) {
                Text("Resume retry")
            }
        }
    }
}

@Composable
private fun DailyLoopCard(item: DailyPracticeItem) {
    val tone = skillTone(item.skill)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(10.dp)
                    .height(112.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(tone.gradient)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = item.skill.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = tone.accent
                )
                Text(item.title, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = item.focus,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${item.exerciseType}  |  ${item.estimatedMinutes} min",
                    style = MaterialTheme.typography.labelMedium
                )
                ContentProvenanceBlock(
                    sourceLabel = item.sourceLabel,
                    collectionTitle = item.collectionTitle
                )
            }
        }
    }
}

@Composable
private fun WeakPatternCard(pattern: WeakPattern) {
    val tone = skillTone(pattern.skill)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = tone.soft.copy(alpha = 0.65f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = pattern.skill.label,
                style = MaterialTheme.typography.labelLarge,
                color = tone.accent
            )
            Text(
                text = pattern.tag,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = pattern.note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SupportCard(
    title: String,
    accent: Color,
    lines: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = accent
            )
            lines.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
