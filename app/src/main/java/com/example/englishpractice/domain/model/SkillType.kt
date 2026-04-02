package com.example.englishpractice.domain.model

enum class SkillType {
    READING,
    WRITING,
    LISTENING,
    SPEAKING;

    val label: String
        get() = name.lowercase().replaceFirstChar { char -> char.titlecase() }

    val deliberatePracticeFocus: String
        get() = when (this) {
            READING -> "main ideas, inference, and precise wording"
            WRITING -> "clarity, collocations, and structured responses"
            LISTENING -> "key details, tone, and summary accuracy"
            SPEAKING -> "fluency, range, task relevance, and response length"
        }
}
