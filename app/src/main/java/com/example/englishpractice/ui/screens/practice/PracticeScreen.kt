package com.example.englishpractice.ui.screens.practice

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.englishpractice.domain.model.SkillType
import com.example.englishpractice.ui.app.AppUiState
import com.example.englishpractice.ui.components.ContentProvenanceBlock

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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Practice", style = MaterialTheme.typography.headlineMedium)
        Text("Choose one of the four deliberate-practice tracks.")
        Button(onClick = onBrowseContent) {
            Text("Browse all content")
        }

        state.skillProgress.forEach { progress ->
            val matchingPlan = state.dailyPlan.firstOrNull { item -> item.skill == progress.skill }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSkillSelected(progress.skill) }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(progress.skill.label, style = MaterialTheme.typography.titleLarge)
                    Text(progress.skill.deliberatePracticeFocus)
                    Text("Completion: ${progress.completionPercent}%")
                    Text("Average score: ${progress.averageScore}%")
                    if (matchingPlan != null) {
                        Text("Today's task: ${matchingPlan.title}")
                        Text("Exercise type: ${matchingPlan.exerciseType}")
                        ContentProvenanceBlock(
                            sourceLabel = matchingPlan.sourceLabel,
                            collectionTitle = matchingPlan.collectionTitle
                        )
                    }

                    when (progress.skill) {
                        SkillType.SPEAKING -> {
                            Text(
                                "Speech input: ${state.speakingCapability.availability} with transcript-first feedback."
                            )
                        }

                        SkillType.LISTENING -> {
                            Text(
                                "Playback: ${state.listeningCapability.playbackEngine} for prompt audio and listening drills."
                            )
                        }

                        else -> {
                            Text("Weak tags: ${progress.weakTags.joinToString()}")
                        }
                    }
                }
            }
        }
    }
}
