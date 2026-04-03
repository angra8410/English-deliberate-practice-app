package com.example.englishpractice.ui.screens.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.englishpractice.domain.model.SkillType
import com.example.englishpractice.feature.progress.SkillProgressSnapshot
import com.example.englishpractice.ui.app.AppUiState
import com.example.englishpractice.ui.app.DailyPracticeItem
import com.example.englishpractice.ui.components.ContentProvenanceBlock
import com.example.englishpractice.ui.components.GlassPanel
import com.example.englishpractice.ui.components.GlowButton
import com.example.englishpractice.ui.components.ImmersiveScreen
import com.example.englishpractice.ui.components.MiniSectionTitle
import com.example.englishpractice.ui.components.StatusPill
import com.example.englishpractice.ui.components.engineLabel
import com.example.englishpractice.ui.components.skillTone
import com.example.englishpractice.ui.components.uiLabel

@Composable
fun PracticeScreen(
    state: AppUiState,
    onSkillSelected: (SkillType) -> Unit = {},
    onBrowseContent: () -> Unit = {}
) {
    val spotlight = state.skillProgress.firstOrNull()

    ImmersiveScreen {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatusPill(text = "Learn")
            StatusPill(text = "Level ${state.currentLevel}", accent = MaterialTheme.colorScheme.tertiary)
            StatusPill(text = "${state.overallCompletion}% path complete", accent = MaterialTheme.colorScheme.primary)
        }

        spotlight?.let { progress ->
            LearnSpotlight(
                progress = progress,
                matchingPlan = state.dailyPlan.firstOrNull { item -> item.skill == progress.skill },
                onOpen = { onSkillSelected(progress.skill) },
                onBrowseContent = onBrowseContent
            )
        }

        MiniSectionTitle(
            eyebrow = "Tracks",
            title = "Choose a lane and go deep"
        )
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            state.skillProgress.forEach { progress ->
                ProgressLaneRow(
                    progress = progress,
                    matchingPlan = state.dailyPlan.firstOrNull { item -> item.skill == progress.skill },
                    onOpen = { onSkillSelected(progress.skill) }
                )
            }
        }
    }
}

@Composable
private fun LearnSpotlight(
    progress: SkillProgressSnapshot,
    matchingPlan: DailyPracticeItem?,
    onOpen: () -> Unit,
    onBrowseContent: () -> Unit
) {
    val tone = skillTone(progress.skill)
    GlassPanel(accent = tone.accent.copy(alpha = 0.3f)) {
        Text(
            text = "Current lane",
            style = MaterialTheme.typography.labelLarge,
            color = tone.accent
        )
        Text(
            text = progress.skill.label,
            style = MaterialTheme.typography.displaySmall
        )
        Text(
            text = progress.skill.deliberatePracticeFocus,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(tone.gradient)
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f))
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Completion ${progress.completionPercent}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Average score ${progress.averageScore}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        LinearProgressIndicator(
            progress = { progress.completionPercent / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(9.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = tone.accent,
            trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
        )
        matchingPlan?.let { plan ->
            ContentProvenanceBlock(
                sourceLabel = plan.sourceLabel,
                collectionTitle = plan.collectionTitle,
                currentTitle = plan.title
            )
            Text(
                text = "${plan.title}  |  ${plan.exerciseType.uiLabel()}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlowButton(
                text = "Open lane",
                onClick = onOpen,
                modifier = Modifier.weight(1f)
            )
            GlowButton(
                text = "Browse all",
                onClick = onBrowseContent,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ProgressLaneRow(
    progress: SkillProgressSnapshot,
    matchingPlan: DailyPracticeItem?,
    onOpen: () -> Unit
) {
    val tone = skillTone(progress.skill)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
        shadowElevation = 10.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(tone.gradient),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = progress.skill.label.take(1),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = progress.skill.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = tone.accent
                )
                matchingPlan?.let { plan ->
                    Text(
                        text = "${plan.title}  |  ${plan.exerciseType.uiLabel()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Completion ${progress.completionPercent}%  |  Average ${progress.averageScore}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Open",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
