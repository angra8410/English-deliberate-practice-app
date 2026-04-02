package com.example.englishpractice.ui.components

import com.example.englishpractice.domain.model.ExerciseType
import com.example.englishpractice.feature.speaking.SpeakingAvailability

fun ExerciseType.uiLabel(): String {
    return when (this) {
        ExerciseType.MULTIPLE_CHOICE -> "Multiple choice"
        ExerciseType.FILL_IN_BLANK -> "Fill in the blanks"
        ExerciseType.OPEN_TEXT -> "Open response"
        ExerciseType.SPEAK_RESPONSE -> "Speak your response"
        ExerciseType.LISTEN_AND_SUMMARIZE -> "Listen and summarize"
        ExerciseType.READ_AND_SUMMARIZE -> "Read and summarize"
        ExerciseType.ERROR_CORRECTION -> "Error correction"
        ExerciseType.SENTENCE_TRANSFORMATION -> "Sentence transformation"
    }
}

fun sourceLabelUi(sourceLabel: String): String {
    return when (sourceLabel) {
        "Built-in assets" -> "Built-in lessons"
        "Book catalog" -> "Book-based lessons"
        "Fallback seed" -> "Starter lessons"
        "Unknown source" -> "Unknown source"
        else -> sourceLabel
    }
}

fun SpeakingAvailability.uiLabel(): String {
    return when (this) {
        SpeakingAvailability.READY -> "Ready"
        SpeakingAvailability.PERMISSION_REQUIRED -> "Microphone permission required"
        SpeakingAvailability.UNSUPPORTED -> "Not available on this device"
    }
}

fun Boolean.uiEnabledLabel(enabledText: String = "Enabled", disabledText: String = "Disabled"): String {
    return if (this) enabledText else disabledText
}

fun engineLabel(rawEngine: String): String {
    return when (rawEngine) {
        "Media3 ExoPlayer with prompt synthesis fallback" -> "Bundled audio with spoken prompt fallback"
        else -> rawEngine
    }
}

fun prettyListItem(raw: String): String {
    return raw.split(' ', '-', '_')
        .filter(String::isNotBlank)
        .joinToString(" ") { part ->
            part.replaceFirstChar { char -> char.uppercase() }
        }
}
