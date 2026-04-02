package com.example.englishpractice.data.repository

import com.example.englishpractice.domain.model.ExerciseType
import com.example.englishpractice.domain.model.SkillType
import com.example.englishpractice.ui.app.PromptScoringProfile

data class BookCatalog(
    val version: Int,
    val generatedAt: String,
    val books: List<BookSeed>
)

data class BookSeed(
    val id: String,
    val title: String,
    val author: String,
    val cefr: List<String>,
    val sourceType: String,
    val tags: List<String>,
    val chapters: List<BookChapter>
)

data class BookChapter(
    val id: String,
    val title: String,
    val order: Int,
    val cefr: List<String>,
    val tags: List<String>,
    val summary: String,
    val points: List<String>,
    val examples: List<BookExample>,
    val pitfalls: List<String>,
    val practicePrompts: List<BookPracticePrompt>,
    val related: List<String>,
    val metadata: Map<String, String>
)

data class BookExample(
    val english: String,
    val note: String? = null
)

data class BookPracticePrompt(
    val id: String,
    val type: ExerciseType,
    val targetSkill: SourceTargetSkill,
    val prompt: String,
    val instructions: String? = null,
    val starterText: String? = null,
    val audioAsset: String? = null,
    val modelAnswer: String? = null,
    val expectedKeywords: List<String> = emptyList(),
    val scoringProfile: PromptScoringProfile? = null,
    val minimumWordCount: Int? = null,
    val minimumResponseItems: Int? = null,
    val minimumKeywordMatches: Int? = null,
    val requiresToneReference: Boolean? = null,
    val requiresContrastMarker: Boolean? = null
)

enum class SourceTargetSkill {
    READING,
    WRITING,
    LISTENING,
    SPEAKING,
    VOCABULARY
}

object BookPromptSkillMapper {
    fun toAppSkill(
        sourceTargetSkill: SourceTargetSkill,
        promptType: ExerciseType
    ): SkillType {
        return when (sourceTargetSkill) {
            SourceTargetSkill.READING -> SkillType.READING
            SourceTargetSkill.WRITING -> SkillType.WRITING
            SourceTargetSkill.LISTENING -> SkillType.LISTENING
            SourceTargetSkill.SPEAKING -> SkillType.SPEAKING
            SourceTargetSkill.VOCABULARY -> vocabularyPromptSkill(promptType)
        }
    }

    private fun vocabularyPromptSkill(promptType: ExerciseType): SkillType {
        return when (promptType) {
            ExerciseType.OPEN_TEXT,
            ExerciseType.ERROR_CORRECTION,
            ExerciseType.SENTENCE_TRANSFORMATION -> SkillType.WRITING

            ExerciseType.MULTIPLE_CHOICE,
            ExerciseType.FILL_IN_BLANK,
            ExerciseType.READ_AND_SUMMARIZE -> SkillType.READING

            ExerciseType.SPEAK_RESPONSE -> SkillType.SPEAKING
            ExerciseType.LISTEN_AND_SUMMARIZE -> SkillType.LISTENING
        }
    }
}
