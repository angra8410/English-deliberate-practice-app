package com.example.englishpractice.ui.screens.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.englishpractice.feature.progress.SkillProgressSnapshot
import com.example.englishpractice.ui.app.AppUiState
import com.example.englishpractice.ui.components.skillTone

@Composable
fun ProgressScreen(state: AppUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Progress map",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Track how each skill is maturing instead of reducing progress to one generic percentage.",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Current level ${state.currentLevel}  |  Target ${state.targetLevel}  |  Streak ${state.streakDays} days",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        state.skillProgress.forEach { progress ->
            ProgressSkillCard(progress = progress)
        }
    }
}

@Composable
private fun ProgressSkillCard(progress: SkillProgressSnapshot) {
    val tone = skillTone(progress.skill)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(tone.gradient)
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
            LinearProgressIndicator(
                progress = { progress.completionPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp)),
                color = tone.accent,
                trackColor = tone.soft
            )
            Text(
                text = "Completion ${progress.completionPercent}%  |  Average ${progress.averageScore}%",
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = "Weak tags: ${progress.weakTags.joinToString()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
