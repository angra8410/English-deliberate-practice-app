package com.example.englishpractice.ui.screens.activity

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.englishpractice.domain.model.SkillType
import com.example.englishpractice.feature.listening.ListeningCapability
import com.example.englishpractice.feature.speaking.SpeakingCapability
import com.example.englishpractice.ui.app.ActivityAttemptRecord
import com.example.englishpractice.ui.app.PracticeActivityItem

@Composable
fun ActivityPlayerScreen(
    activity: PracticeActivityItem?,
    lastAttempt: ActivityAttemptRecord?,
    speakingCapability: SpeakingCapability,
    listeningCapability: ListeningCapability,
    onSubmit: (answer: String, transcriptText: String?) -> Unit
) {
    if (activity == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("The selected activity could not be loaded.")
        }
        return
    }

    var answerText by rememberSaveable(activity.id) { mutableStateOf(activity.starterText) }
    var listeningPlaybackReady by rememberSaveable(activity.id) { mutableStateOf(false) }

    val fieldLabel = when (activity.skill) {
        SkillType.SPEAKING -> "Transcript or speaking notes"
        SkillType.LISTENING -> "Listening summary"
        else -> "Your response"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(activity.title, style = MaterialTheme.typography.headlineMedium)
        Text(activity.instructions)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(activity.skill.label, style = MaterialTheme.typography.labelLarge)
                Text(activity.prompt)
                Text(activity.supportNote, style = MaterialTheme.typography.bodySmall)
            }
        }

        if (activity.skill == SkillType.LISTENING) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Listening playback", style = MaterialTheme.typography.titleMedium)
                    Text("Engine: ${listeningCapability.playbackEngine}")
                    Button(onClick = { listeningPlaybackReady = true }) {
                        Text(if (listeningPlaybackReady) "Prompt marked as played" else "Mark prompt as played")
                    }
                }
            }
        }

        if (activity.skill == SkillType.SPEAKING) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Speaking capture", style = MaterialTheme.typography.titleMedium)
                    Text("Availability: ${speakingCapability.availability}")
                    Text("Feedback dimensions: ${speakingCapability.feedbackDimensions.joinToString()}")
                    Button(onClick = { answerText = activity.starterText }) {
                        Text("Load guided transcript")
                    }
                }
            }
        }

        OutlinedTextField(
            value = answerText,
            onValueChange = { answerText = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 8,
            label = { Text(fieldLabel) }
        )

        Button(
            onClick = {
                val transcript = if (activity.skill == SkillType.SPEAKING) answerText else null
                onSubmit(answerText, transcript)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Submit and get feedback")
        }

        if (lastAttempt != null && lastAttempt.activityId == activity.id) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Latest attempt", style = MaterialTheme.typography.titleMedium)
                    Text("Score: ${lastAttempt.score}")
                    Text("Weak tags: ${lastAttempt.weakTags.joinToString().ifBlank { "None" }}")
                    lastAttempt.feedback.forEach { line ->
                        Text("- $line")
                    }
                    Text("Model answer", style = MaterialTheme.typography.titleSmall)
                    Text(activity.modelAnswer)
                }
            }
        }
    }
}
