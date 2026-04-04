package com.example.englishpractice.ui.screens.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.englishpractice.domain.model.SkillType
import com.example.englishpractice.feature.progress.SkillProgressSnapshot
import com.example.englishpractice.ui.app.AppUiState
import com.example.englishpractice.ui.app.DailyPracticeItem
import com.example.englishpractice.ui.components.GlowButton
import com.example.englishpractice.ui.components.ImmersiveScreen
import com.example.englishpractice.ui.components.StatusPill
import com.example.englishpractice.ui.components.skillTone
import com.example.englishpractice.ui.components.uiLabel

@Composable
fun PracticeScreen(
    state: AppUiState,
    onSkillSelected: (SkillType) -> Unit = {},
    onBrowseContent: () -> Unit = {}
) {
    val spotlight = state.skillProgress.maxByOrNull { progress -> progress.completionPercent }
        ?: state.skillProgress.firstOrNull()
    val sessionScoredSkills = state.activityCatalog
        .filter { activity -> activity.id in state.sessionSubmittedActivityIds }
        .map { activity -> activity.skill }
        .toSet()
    val pathItems = state.skillProgress.map { progress ->
        LearnPathItem(
            progress = progress,
            matchingPlan = state.dailyPlan.firstOrNull { item -> item.skill == progress.skill },
            showSessionScore = progress.skill in sessionScoredSkills
        )
    }

    ImmersiveScreen {
        LearnHero(
            state = state,
            spotlight = spotlight,
            onOpenSpotlight = { spotlight?.let { onSkillSelected(it.skill) } },
            onBrowseContent = onBrowseContent
        )
        LearnPath(
            items = pathItems,
            onSkillSelected = onSkillSelected
        )
    }
}

private data class LearnPathItem(
    val progress: SkillProgressSnapshot,
    val matchingPlan: DailyPracticeItem?,
    val showSessionScore: Boolean
)

@Composable
private fun LearnHero(
    state: AppUiState,
    spotlight: SkillProgressSnapshot?,
    onOpenSpotlight: () -> Unit,
    onBrowseContent: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(34.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.16f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(34.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(34.dp)
                )
                .padding(22.dp)
        ) {
            LearningAura()
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusPill(text = "B2 path")
                    StatusPill(
                        text = "${state.dailyPlan.size} live missions",
                        accent = MaterialTheme.colorScheme.tertiary
                    )
                    StatusPill(
                        text = "${state.overallCompletion}% complete",
                        accent = MaterialTheme.colorScheme.primary
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Learn through a moving lesson path",
                        style = MaterialTheme.typography.displaySmall
                    )
                    Text(
                        text = "One focused mission at a time. Open the next lane, clear it, then move down the path instead of browsing a static book grid.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                spotlight?.let { progress ->
                    val tone = skillTone(progress.skill)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Recommended next lane",
                            style = MaterialTheme.typography.labelLarge,
                            color = tone.accent
                        )
                        Text(
                            text = progress.skill.label,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = progress.skill.deliberatePracticeFocus,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlowButton(
                        text = "Open next mission",
                        onClick = onOpenSpotlight,
                        modifier = Modifier.weight(1f)
                    )
                    GlowButton(
                        text = "Browse all lessons",
                        onClick = onBrowseContent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.LearningAura() {
    Box(
        modifier = Modifier
            .size(210.dp)
            .align(Alignment.TopEnd)
            .offset(x = 76.dp, y = (-58).dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                        androidx.compose.ui.graphics.Color.Transparent
                    )
                )
            )
    )
    Box(
        modifier = Modifier
            .size(170.dp)
            .align(Alignment.BottomStart)
            .offset(x = (-42).dp, y = 56.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                        androidx.compose.ui.graphics.Color.Transparent
                    )
                )
            )
    )
}

@Composable
private fun LearnPath(
    items: List<LearnPathItem>,
    onSkillSelected: (SkillType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(
            text = "Mission path",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        items.forEachIndexed { index, item ->
            val offset = when (index % 3) {
                1 -> 30.dp
                2 -> (-22).dp
                else -> 0.dp
            }
            MissionNode(
                index = index,
                item = item,
                modifier = Modifier.offset(x = offset),
                onClick = { onSkillSelected(item.progress.skill) }
            )
        }
    }
}

@Composable
private fun MissionNode(
    index: Int,
    item: LearnPathItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val tone = skillTone(item.progress.skill)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(tone.gradient)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            if (index < 3) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .width(6.dp)
                        .height(74.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    tone.accent.copy(alpha = 0.8f),
                                    tone.accent.copy(alpha = 0.18f)
                                )
                            )
                        )
                )
            }
        }
        Surface(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
            tonalElevation = 0.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                tone.soft.copy(alpha = 0.92f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = tone.accent.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = item.progress.skill.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = tone.accent
                        )
                        Text(
                            text = item.matchingPlan?.title ?: item.progress.skill.deliberatePracticeFocus,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (item.showSessionScore && item.progress.averageScore > 0) {
                        Surface(
                            modifier = Modifier.padding(start = 12.dp),
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
                        ) {
                            Text(
                                text = "Score ${item.progress.averageScore}%",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
                Text(
                    text = item.matchingPlan?.focus ?: item.progress.skill.deliberatePracticeFocus,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                item.matchingPlan?.let { plan ->
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusPill(text = plan.exerciseType.uiLabel(), accent = tone.accent)
                        StatusPill(
                            text = "${plan.estimatedMinutes} min",
                            accent = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                LinearProgressIndicator(
                    progress = { item.progress.completionPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = tone.accent,
                    trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.14f)
                )
                Text(
                    text = if (item.progress.completionPercent == 0) {
                        "Progress 0% • Tap to start this lane"
                    } else {
                        "Progress ${item.progress.completionPercent}% • Continue this lane"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}
