package com.example.englishpractice.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
    val weakPatternsBySkill = state.weakPatterns.associateBy { it.skill }
    val reviewBySkill = state.reviewQueue.associateBy { it.skill }
    var expandedSkillName by rememberSaveable { mutableStateOf<String?>(null) }

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
            title = "Skill hubs",
            description = "Each card holds the anchor task, the weak pattern to watch, and the next retry for that skill."
        )
        state.dailyPlan.forEach { item ->
            SkillHubCard(
                item = item,
                weakPattern = weakPatternsBySkill[item.skill],
                reviewItem = reviewBySkill[item.skill],
                expanded = expandedSkillName == item.skill.name,
                onToggleExpanded = {
                    expandedSkillName = if (expandedSkillName == item.skill.name) {
                        null
                    } else {
                        item.skill.name
                    }
                },
                onResumeReview = onResumeReview
            )
        }

        SectionHeading(
            eyebrow = "Support",
            title = "Capture and playback",
            description = "The toolchain is still visible, just condensed so it stops dominating the page."
        )
        SupportStrip(state = state)
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
                text = levelSummary(state),
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
                    Text("Start today's loop")
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
private fun SkillHubCard(
    item: DailyPracticeItem,
    weakPattern: WeakPattern?,
    reviewItem: ReviewQueueItem?,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onResumeReview: (String) -> Unit
) {
    val tone = skillTone(item.skill)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, tone.accent.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = tone.soft
                    ) {
                        Text(
                            text = item.skill.label,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = tone.accent
                        )
                    }
                    reviewItem?.let {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = reviewCardColor(it.dueLabel).copy(alpha = 0.92f)
                        ) {
                            Text(
                                text = it.dueLabel,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                OutlinedButton(onClick = onToggleExpanded) {
                    Text(if (expanded) "Collapse" else "Expand")
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Today's anchor",
                    style = MaterialTheme.typography.labelLarge,
                    color = tone.accent
                )
                Text(item.title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = item.focus,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${item.exerciseType}  |  ${item.estimatedMinutes} min",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ContentProvenanceBlock(
                        sourceLabel = item.sourceLabel,
                        collectionTitle = item.collectionTitle,
                        currentTitle = item.title
                    )

                    weakPattern?.let { pattern ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = tone.soft.copy(alpha = 0.72f)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Focus to tighten",
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

                    if (reviewItem != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, tone.accent.copy(alpha = 0.16f))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Next retry",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = tone.accent
                                )
                                Text(
                                    text = reviewItem.reason,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (reviewItem.weakTags.isNotEmpty()) {
                                    Text(
                                        text = "Weak tags: ${reviewItem.weakTags.joinToString()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(onClick = { onResumeReview(reviewItem.activityId) }) {
                                    Text("Resume ${item.skill.label.lowercase()} retry")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SupportStrip(state: AppUiState) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
private fun SupportCard(
    title: String,
    accent: Color,
    lines: List<String>
) {
    Card(
        modifier = Modifier.width(260.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f))
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

private fun levelSummary(state: AppUiState): String {
    return if (state.currentLevel == state.targetLevel) {
        "Current level ${state.currentLevel}"
    } else {
        "Current level ${state.currentLevel}  |  Target ${state.targetLevel}"
    }
}

@Composable
private fun reviewCardColor(dueLabel: String) = when (dueLabel) {
    "Due now" -> MaterialTheme.colorScheme.errorContainer
    "Today" -> MaterialTheme.colorScheme.secondaryContainer
    else -> MaterialTheme.colorScheme.tertiaryContainer
}
