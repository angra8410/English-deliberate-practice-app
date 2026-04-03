package com.example.englishpractice.data.repository

import com.example.englishpractice.domain.model.CefrLevel
import com.example.englishpractice.domain.model.ExerciseType
import com.example.englishpractice.domain.model.SkillType
import com.example.englishpractice.ui.app.PracticeActivityItem
import com.example.englishpractice.ui.app.PromptScoringProfile

object BookCatalogMapper {
    fun toLevels(catalog: BookCatalog): List<CefrLevel> {
        return catalog.books
            .flatMap { book ->
                book.chapters.mapNotNull { chapter ->
                    resolveLevel(chapter.cefr.ifEmpty { book.cefr })
                }
            }
            .distinct()
    }

    fun toUnits(catalog: BookCatalog): List<PracticeUnitAsset> {
        return catalog.books.flatMap { book ->
            book.chapters.mapNotNull { chapter ->
                val level = resolveLevel(chapter.cefr.ifEmpty { book.cefr }) ?: return@mapNotNull null
                val dominantSkill = chapter.practicePrompts
                    .firstOrNull()
                    ?.let { prompt -> BookPromptSkillMapper.toAppSkill(prompt.targetSkill, prompt.type) }
                    ?: inferSkillFromTags(chapter.tags)

                PracticeUnitAsset(
                    id = chapter.id,
                    title = chapter.title,
                    level = level,
                    skill = dominantSkill,
                    description = buildChapterDescription(chapter),
                    sourceLabel = "Book catalog"
                )
            }
        }
    }

    fun toActivities(catalog: BookCatalog): List<PracticeActivityItem> {
        return catalog.books.flatMap { book ->
            book.chapters.flatMap { chapter ->
                val level = resolveLevel(chapter.cefr.ifEmpty { book.cefr }) ?: return@flatMap emptyList()
                chapter.practicePrompts.mapIndexed { index, prompt ->
                    val skill = BookPromptSkillMapper.toAppSkill(prompt.targetSkill, prompt.type)
                    PracticeActivityItem(
                        id = prompt.id,
                        unitId = chapter.id,
                        skill = skill,
                        title = buildActivityTitle(chapter.title, index, chapter.practicePrompts.size),
                        sourceLabel = "Book catalog",
                        collectionTitle = book.title,
                        instructions = prompt.instructions ?: buildInstructions(prompt.type),
                        prompt = prompt.prompt,
                        exerciseType = prompt.type,
                        starterText = prompt.starterText ?: defaultStarterText(skill),
                        audioAssetPath = prompt.audioAsset,
                        listeningPromptText = prompt.listeningPromptText,
                        modelAnswer = prompt.modelAnswer ?: defaultModelAnswer(skill),
                        evaluationTargets = buildEvaluationTargets(chapter, prompt, skill),
                        supportNote = buildSupportNote(book.title, level, chapter),
                        tags = buildActivityTags(book, chapter, prompt),
                        difficulty = inferDifficulty(level),
                        scoringProfile = prompt.scoringProfile ?: inferScoringProfile(prompt),
                        minimumWordCount = prompt.minimumWordCount ?: inferMinimumWordCount(prompt),
                        minimumResponseItems = prompt.minimumResponseItems ?: inferMinimumResponseItems(prompt),
                        minimumKeywordMatches = prompt.minimumKeywordMatches
                            ?: inferMinimumKeywordMatches(prompt),
                        requiresToneReference = prompt.requiresToneReference
                            ?: inferToneRequirement(prompt),
                        requiresContrastMarker = prompt.requiresContrastMarker
                            ?: inferContrastRequirement(prompt)
                    )
                }
            }
        }
    }

    private fun resolveLevel(candidates: List<String>): CefrLevel? {
        return when {
            "C1" in candidates -> CefrLevel.C1
            "C2" in candidates -> CefrLevel.C1
            "B2" in candidates -> CefrLevel.B2
            "B1" in candidates -> CefrLevel.B1
            "A2" in candidates -> CefrLevel.A2
            "A1" in candidates -> CefrLevel.A1
            else -> null
        }
    }

    private fun inferSkillFromTags(tags: List<String>): SkillType {
        return when {
            tags.any { it.contains("listen", ignoreCase = true) } -> SkillType.LISTENING
            tags.any { it.contains("speak", ignoreCase = true) } -> SkillType.SPEAKING
            tags.any { it.contains("read", ignoreCase = true) } -> SkillType.READING
            else -> SkillType.WRITING
        }
    }

    private fun buildChapterDescription(chapter: BookChapter): String {
        return buildList {
            add(chapter.summary)
            chapter.points.firstOrNull()?.let(::add)
            chapter.pitfalls.firstOrNull()?.let { add("Pitfall: $it") }
        }.joinToString(" ")
    }

    private fun buildActivityTitle(title: String, index: Int, totalPrompts: Int): String {
        return if (totalPrompts > 1) "$title (${index + 1})" else title
    }

    private fun buildInstructions(exerciseType: ExerciseType): String {
        return when (exerciseType) {
            ExerciseType.OPEN_TEXT -> "Write a clear, focused response using the target language from this chapter."
            ExerciseType.FILL_IN_BLANK -> "Complete each item with the most natural word or phrase."
            ExerciseType.MULTIPLE_CHOICE -> "Choose the best answer and pay attention to meaning and usage."
            ExerciseType.SPEAK_RESPONSE -> "Answer aloud and use the chapter language as naturally as possible."
            ExerciseType.LISTEN_AND_SUMMARIZE -> "Listen carefully and summarize the main point with one supporting detail."
            ExerciseType.READ_AND_SUMMARIZE -> "Read the prompt and summarize the key idea accurately."
            ExerciseType.ERROR_CORRECTION -> "Rewrite the text with the errors corrected."
            ExerciseType.SENTENCE_TRANSFORMATION -> "Rewrite the sentence while preserving the original meaning."
        }
    }

    private fun defaultStarterText(skill: SkillType): String {
        return when (skill) {
            SkillType.READING -> "The main idea is that..."
            SkillType.WRITING -> ""
            SkillType.LISTENING -> "The speaker explains that..."
            SkillType.SPEAKING -> ""
        }
    }

    private fun defaultModelAnswer(skill: SkillType): String {
        return when (skill) {
            SkillType.READING -> "Summarize the key idea, include one supporting detail, and keep the wording precise."
            SkillType.WRITING -> "Use the target language accurately and support your ideas with a clear example."
            SkillType.LISTENING -> "Capture the main point and one important supporting detail."
            SkillType.SPEAKING -> "Answer directly, develop the point, and use the target language naturally."
        }
    }

    private fun buildEvaluationTargets(
        chapter: BookChapter,
        prompt: BookPracticePrompt,
        skill: SkillType
    ): List<String> {
        if (prompt.expectedKeywords.isNotEmpty()) {
            return prompt.expectedKeywords.distinct().take(6)
        }
        return when (skill) {
            SkillType.READING,
            SkillType.WRITING,
            SkillType.LISTENING,
            SkillType.SPEAKING -> chapter.tags.take(4) + chapter.points.take(2).flatMap(::extractKeywords)
        }.distinct().take(6)
    }

    private fun inferScoringProfile(prompt: BookPracticePrompt): PromptScoringProfile {
        val normalizedPrompt = prompt.prompt.lowercase()
        return when {
            prompt.type == ExerciseType.ERROR_CORRECTION ||
                prompt.type == ExerciseType.SENTENCE_TRANSFORMATION ||
                normalizedPrompt.startsWith("rewrite") -> PromptScoringProfile.REWRITE

            normalizedPrompt.contains("sentence pair") -> PromptScoringProfile.SENTENCE_DRILL
            normalizedPrompt.startsWith("list ") -> PromptScoringProfile.LIST
            else -> PromptScoringProfile.DEFAULT
        }
    }

    private fun inferMinimumWordCount(prompt: BookPracticePrompt): Int? {
        return when (prompt.scoringProfile ?: inferScoringProfile(prompt)) {
            PromptScoringProfile.LIST -> 10
            PromptScoringProfile.SENTENCE_DRILL -> 18
            PromptScoringProfile.REWRITE -> 20
            PromptScoringProfile.DEFAULT -> null
        }
    }

    private fun inferMinimumResponseItems(prompt: BookPracticePrompt): Int? {
        val numericHint = Regex("\\b(\\d+)\\b")
            .find(prompt.prompt)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?: wordNumberHint(prompt.prompt)

        return when (prompt.scoringProfile ?: inferScoringProfile(prompt)) {
            PromptScoringProfile.LIST -> numericHint
            PromptScoringProfile.SENTENCE_DRILL -> numericHint
            PromptScoringProfile.REWRITE -> numericHint
            PromptScoringProfile.DEFAULT -> null
        }
    }

    private fun inferMinimumKeywordMatches(prompt: BookPracticePrompt): Int? {
        return when (prompt.type) {
            ExerciseType.READ_AND_SUMMARIZE,
            ExerciseType.LISTEN_AND_SUMMARIZE -> 2

            else -> null
        }
    }

    private fun inferToneRequirement(prompt: BookPracticePrompt): Boolean? {
        return when (prompt.type) {
            ExerciseType.READ_AND_SUMMARIZE -> true
            else -> null
        }
    }

    private fun inferContrastRequirement(prompt: BookPracticePrompt): Boolean? {
        return when (prompt.type) {
            ExerciseType.LISTEN_AND_SUMMARIZE -> true
            else -> null
        }
    }

    private fun wordNumberHint(promptText: String): Int? {
        val numberWords = mapOf(
            "one" to 1,
            "two" to 2,
            "three" to 3,
            "four" to 4,
            "five" to 5,
            "six" to 6,
            "seven" to 7,
            "eight" to 8,
            "nine" to 9,
            "ten" to 10
        )
        val normalizedPrompt = promptText.lowercase()
        return numberWords.entries.firstOrNull { (word, _) ->
            Regex("\\b$word\\b").containsMatchIn(normalizedPrompt)
        }?.value
    }

    private fun extractKeywords(text: String): List<String> {
        return text.split(Regex("[^A-Za-z]+"))
            .map { it.lowercase() }
            .filter { it.length >= 4 }
            .take(2)
    }

    private fun buildActivityTags(
        book: BookSeed,
        chapter: BookChapter,
        prompt: BookPracticePrompt
    ): List<String> {
        return buildList {
            addAll(book.tags)
            addAll(chapter.tags)
            add(prompt.type.name.lowercase())
        }.distinct().take(6)
    }

    private fun inferDifficulty(level: CefrLevel): Int {
        return when (level) {
            CefrLevel.A1,
            CefrLevel.A2 -> 1

            CefrLevel.B1 -> 2
            CefrLevel.B2 -> 3
            CefrLevel.C1 -> 4
        }
    }

    private fun buildSupportNote(bookTitle: String, level: CefrLevel, chapter: BookChapter): String {
        val example = chapter.examples.firstOrNull()?.english
        return buildList {
            add("Adapted from $bookTitle for $level practice.")
            example?.let { add("Example: $it") }
            chapter.pitfalls.firstOrNull()?.let { add("Watch out: $it") }
        }.joinToString(" ")
    }
}
