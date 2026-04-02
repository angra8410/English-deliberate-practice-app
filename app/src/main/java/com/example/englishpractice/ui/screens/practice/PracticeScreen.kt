package com.example.englishpractice.ui.screens.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.englishpractice.domain.model.SkillType
import com.example.englishpractice.feature.progress.SkillProgressSnapshot
import com.example.englishpractice.ui.app.AppUiState
import com.example.englishpractice.ui.app.DailyPracticeItem
import com.example.englishpractice.ui.components.ContentProvenanceBlock
import com.example.englishpractice.ui.components.engineLabel
import com.example.englishpractice.ui.components.skillTone
import com.example.englishpractice.ui.components.uiLabel

@Composable
fun PracticeScreen(
    state: AppUiState,
    onSkillSelected: (SkillType) -> Unit = {},
    onBrowseContent: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        PracticeHero(
            currentLevel = state.currentLevel.name,
            completion = state.overallCompletion,
            onBrowseContent = onBrowseContent
        )

        SectionHeading(
            eyebrow = "Tracks",
            title = "Choose your next skill",
            description = "Each track keeps the same logic, but the visual rhythm now makes the differences easier to scan."
        )

        state.skillProgress.forEach { progress ->
            SkillTrackCard(
                progress = progress,
                matchingPlan = state.dailyPlan.firstOrNull { item -> item.skill == progress.skill },
                speakingStatus = state.speakingCapability.availability.uiLabel(),
                listeningEngine = engineLabel(state.listeningCapability.playbackEngine),
                onOpen = { onSkillSelected(progress.skill) }
            )
        }
    }
}

@Composable
private fun PracticeHero(
    currentLevel: String,
    completion: Int,
    onBrowseContent: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Practice studio",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Pick one track and go deep instead of skimming all four at once.",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Current level $currentLevel  |  Overall completion $completion%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onBrowseContent) {
                Text("Browse the full catalog")
            }
        }
    }
}

@Composable
private fun SkillTrackCard(
    progress: SkillProgressSnapshot,
    matchingPlan: DailyPracticeItem?,
    speakingStatus: String,
    listeningEngine: String,
    onOpen: () -> Unit
) {
    val tone = skillTone(progress.skill)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .background(tone.gradient)
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = progress.skill.label,
                        style = MaterialTheme.typography.titleLarge,
                        color = tone.accent
                    )
                    Text(
                        text = progress.skill.deliberatePracticeFocus,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "Completion ${progress.completionPercent}%  |  Average ${progress.averageScore}%",
                style = MaterialTheme.typography.labelLarge
            )

            matchingPlan?.let { plan ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Today's anchor", style = MaterialTheme.typography.labelLarge, color = tone.accent)
                    Text(plan.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${plan.exerciseType.uiLabel()}  |  ${plan.estimatedMinutes} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ContentProvenanceBlock(
                        sourceLabel = plan.sourceLabel,
                        collectionTitle = plan.collectionTitle
                    )
                }
            }

            Text(
                text = when (progress.skill) {
                    SkillType.SPEAKING -> "Capture mode: $speakingStatus with transcript-first feedback."
                    SkillType.LISTENING -> "Playback mode: $listeningEngine for prompt audio and summaries."
                    else -> "Weak tags: ${progress.weakTags.joinToString()}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(onClick = onOpen) {
                Text("Open ${progress.skill.label}")
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
