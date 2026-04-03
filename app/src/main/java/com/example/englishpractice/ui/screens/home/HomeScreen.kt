package com.example.englishpractice.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.englishpractice.domain.model.CefrLevel
import com.example.englishpractice.ui.app.AppUiState
import com.example.englishpractice.ui.app.DailyPracticeItem
import com.example.englishpractice.ui.app.ReviewQueueItem
import com.example.englishpractice.ui.components.ContentProvenanceBlock
import com.example.englishpractice.ui.components.GlassPanel
import com.example.englishpractice.ui.components.GlowButton
import com.example.englishpractice.ui.components.ImmersiveScreen
import com.example.englishpractice.ui.components.MiniSectionTitle
import com.example.englishpractice.ui.components.StatusPill
import com.example.englishpractice.ui.components.skillTone
import com.example.englishpractice.ui.components.uiLabel

@Composable
fun HomeScreen(
    state: AppUiState,
    onPilotLevelSelected: (CefrLevel) -> Unit,
    onStartPractice: () -> Unit,
    onBrowseContent: () -> Unit,
    onResumeReview: (String) -> Unit
) {
    val spotlight = state.dailyPlan.firstOrNull()

    ImmersiveScreen {
        TopStatusRow(state = state)
        LevelSelector(
            state = state,
            onPilotLevelSelected = onPilotLevelSelected
        )
        spotlight?.let { item ->
            LessonSpotlight(
                item = item,
                overallCompletion = state.overallCompletion,
                onStartPractice = onStartPractice,
                onBrowseContent = onBrowseContent
            )
        }
        state.reviewQueue.firstOrNull()?.let { item ->
            RetrySpotlight(
                item = item,
                onResumeReview = onResumeReview
            )
        }
        MiniSectionTitle(
            eyebrow = "Learning path",
            title = "Your four-skill roadmap"
        )
        HomeJourney(
            dailyPlan = state.dailyPlan,
            reviewQueue = state.reviewQueue,
            onResumeReview = onResumeReview
        )
    }
}

@Composable
private fun TopStatusRow(state: AppUiState) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatusPill(text = "Level ${state.currentLevel}")
        StatusPill(
            text = "${state.streakDays} day streak",
            accent = MaterialTheme.colorScheme.tertiary
        )
        StatusPill(
            text = "${state.overallCompletion}% complete",
            accent = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun LevelSelector(
    state: AppUiState,
    onPilotLevelSelected: (CefrLevel) -> Unit
) {
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
}

@Composable
private fun LessonSpotlight(
    item: DailyPracticeItem,
    overallCompletion: Int,
    onStartPractice: () -> Unit,
    onBrowseContent: () -> Unit
) {
    val tone = skillTone(item.skill)
    GlassPanel(accent = tone.accent.copy(alpha = 0.3f)) {
        Text(
            text = "Today's main lesson",
            style = MaterialTheme.typography.labelLarge,
            color = tone.accent
        )
        Text(
            text = item.title,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = item.focus,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LessonOrb(item = item)
        ContentProvenanceBlock(
            sourceLabel = item.sourceLabel,
            collectionTitle = item.collectionTitle,
            currentTitle = item.title
        )
        Text(
            text = "${item.exerciseType.uiLabel()}  |  ${item.estimatedMinutes} min",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        LinearProgressIndicator(
            progress = { overallCompletion / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(9.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.14f)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlowButton(
                text = "Continue lesson",
                onClick = onStartPractice,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = onBrowseContent,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onBackground
                )
            ) {
                Text("Open roadmap")
            }
        }
    }
}

@Composable
private fun LessonOrb(item: DailyPracticeItem) {
    val tone = skillTone(item.skill)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(tone.gradient)
    ) {
        Box(
            modifier = Modifier
                .size(170.dp)
                .align(Alignment.CenterStart)
                .offset(x = (-18).dp, y = 14.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.14f)
            ) {
                Text(
                    text = item.skill.label.uppercase(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun RetrySpotlight(
    item: ReviewQueueItem,
    onResumeReview: (String) -> Unit
) {
    val tone = skillTone(item.skill)
    GlassPanel(accent = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.26f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Review ready",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            StatusPill(
                text = item.dueLabel,
                accent = tone.accent
            )
        }
        Text(
            text = item.reason,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (item.weakTags.isNotEmpty()) {
            Text(
                text = "Watch: ${item.weakTags.joinToString()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        GlowButton(
            text = "Resume retry",
            onClick = { onResumeReview(item.activityId) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun HomeJourney(
    dailyPlan: List<DailyPracticeItem>,
    reviewQueue: List<ReviewQueueItem>,
    onResumeReview: (String) -> Unit
) {
    val reviewBySkill = reviewQueue.associateBy { it.skill }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        dailyPlan.forEachIndexed { index, item ->
            JourneyStep(
                index = index,
                item = item,
                reviewItem = reviewBySkill[item.skill],
                onResumeReview = onResumeReview
            )
        }
    }
}

@Composable
private fun JourneyStep(
    index: Int,
    item: DailyPracticeItem,
    reviewItem: ReviewQueueItem?,
    onResumeReview: (String) -> Unit
) {
    val tone = skillTone(item.skill)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(tone.gradient)
                    .border(2.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f), CircleShape),
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
                        .width(2.dp)
                        .height(54.dp)
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f))
                )
            }
        }
        GlassPanel(
            modifier = Modifier.weight(1f),
            accent = tone.accent.copy(alpha = 0.22f)
        ) {
            Text(
                text = item.skill.label,
                style = MaterialTheme.typography.labelLarge,
                color = tone.accent
            )
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = item.exerciseType.uiLabel(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            reviewItem?.let { review ->
                Surface(
                    modifier = Modifier.clickable { onResumeReview(review.activityId) },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)
                ) {
                    Text(
                        text = "Retry ready: ${review.dueLabel}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
