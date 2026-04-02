package com.example.englishpractice.data.repository

import com.example.englishpractice.domain.model.CefrLevel
import com.example.englishpractice.domain.model.ExerciseType
import com.example.englishpractice.domain.model.SkillType
import com.example.englishpractice.ui.app.PromptScoringProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BookCatalogMapperTest {
    @Test
    fun `toActivities maps vocabulary open text prompts to writing activities`() {
        val catalog = BookCatalog(
            version = 2,
            generatedAt = "2026-04-02T13:08:48.105482+00:00",
            books = listOf(
                BookSeed(
                    id = "english-vocabulary-in-use-advanced",
                    title = "English Vocabulary in Use Advanced",
                    author = "Michael McCarthy; Felicity O'Dell",
                    cefr = listOf("C1", "C2"),
                    sourceType = "curated_notes",
                    tags = listOf("vocabulary"),
                    chapters = listOf(
                        BookChapter(
                            id = "applying-for-a-job",
                            title = "Applying for a job",
                            order = 3,
                            cefr = listOf("C1"),
                            tags = listOf("vocabulary", "jobs", "applications", "career"),
                            summary = "Vocabulary for job ads, cover letters, applications and career-related language.",
                            points = listOf("Applications should stand out clearly and professionally."),
                            examples = listOf(BookExample("Please find attached my CV.", "formal application language")),
                            pitfalls = listOf("Avoid overly casual language in job applications."),
                            practicePrompts = listOf(
                                BookPracticePrompt(
                                    id = "applying-for-a-job-prompt-2",
                                    type = ExerciseType.OPEN_TEXT,
                                    targetSkill = SourceTargetSkill.VOCABULARY,
                                    prompt = "List five phrases commonly used in job advertisements.",
                                    expectedKeywords = listOf("competitive salary", "career prospects"),
                                    scoringProfile = PromptScoringProfile.LIST,
                                    minimumResponseItems = 5
                                )
                            ),
                            related = listOf("job-interviews"),
                            metadata = mapOf("sourceFile" to "vocabulary/unit-3.json")
                        )
                    )
                )
            )
        )

        val units = BookCatalogMapper.toUnits(catalog)
        val activities = BookCatalogMapper.toActivities(catalog)

        assertEquals(listOf(CefrLevel.C1), BookCatalogMapper.toLevels(catalog))
        assertEquals(1, units.size)
        assertEquals(SkillType.WRITING, units.single().skill)
        assertEquals("Book catalog", units.single().sourceLabel)
        assertEquals(1, activities.size)
        assertEquals("applying-for-a-job-prompt-2", activities.single().id)
        assertEquals("applying-for-a-job", activities.single().unitId)
        assertEquals(SkillType.WRITING, activities.single().skill)
        assertEquals("Applying for a job", activities.single().title)
        assertEquals(PromptScoringProfile.LIST, activities.single().scoringProfile)
        assertEquals(5, activities.single().minimumResponseItems)
        assertEquals(
            listOf("competitive salary", "career prospects"),
            activities.single().evaluationTargets
        )
        assertTrue(activities.single().supportNote.contains("English Vocabulary in Use Advanced"))
    }

    @Test
    fun `toLevels falls back from c2 to c1 for advanced source content`() {
        val catalog = BookCatalog(
            version = 2,
            generatedAt = "2026-04-02T13:08:48.105482+00:00",
            books = listOf(
                BookSeed(
                    id = "advanced-grammar-in-use",
                    title = "Advanced Grammar in Use",
                    author = "Martin Hewings",
                    cefr = listOf("C2"),
                    sourceType = "curated_notes",
                    tags = listOf("grammar"),
                    chapters = listOf(
                        BookChapter(
                            id = "present-simple-vs-continuous",
                            title = "Present simple vs present continuous",
                            order = 1,
                            cefr = emptyList(),
                            tags = listOf("grammar", "tense"),
                            summary = "Use present simple for routines and general truths.",
                            points = listOf("Present simple describes habits."),
                            examples = emptyList(),
                            pitfalls = emptyList(),
                            practicePrompts = listOf(
                                BookPracticePrompt(
                                    id = "present-simple-vs-continuous-prompt-1",
                                    type = ExerciseType.OPEN_TEXT,
                                    targetSkill = SourceTargetSkill.WRITING,
                                    prompt = "Write three sentence pairs."
                                )
                            ),
                            related = emptyList(),
                            metadata = emptyMap()
                        )
                    )
                )
            )
        )

        assertEquals(listOf(CefrLevel.C1), BookCatalogMapper.toLevels(catalog))
    }

    @Test
    fun `toActivities infers sentence drill profile from prompt wording`() {
        val catalog = BookCatalog(
            version = 2,
            generatedAt = "2026-04-02T13:08:48.105482+00:00",
            books = listOf(
                BookSeed(
                    id = "advanced-grammar-in-use",
                    title = "Advanced Grammar in Use",
                    author = "Martin Hewings",
                    cefr = listOf("C1"),
                    sourceType = "curated_notes",
                    tags = listOf("grammar"),
                    chapters = listOf(
                        BookChapter(
                            id = "present-simple-vs-continuous",
                            title = "Present simple vs present continuous",
                            order = 1,
                            cefr = listOf("C1"),
                            tags = listOf("grammar", "tense"),
                            summary = "Use present simple for routines.",
                            points = listOf("Present simple describes habits."),
                            examples = emptyList(),
                            pitfalls = emptyList(),
                            practicePrompts = listOf(
                                BookPracticePrompt(
                                    id = "present-simple-vs-continuous-prompt-1",
                                    type = ExerciseType.OPEN_TEXT,
                                    targetSkill = SourceTargetSkill.WRITING,
                                    prompt = "Write three sentence pairs contrasting present simple and present continuous."
                                )
                            ),
                            related = emptyList(),
                            metadata = emptyMap()
                        )
                    )
                )
            )
        )

        val activity = BookCatalogMapper.toActivities(catalog).single()

        assertEquals(PromptScoringProfile.SENTENCE_DRILL, activity.scoringProfile)
        assertEquals(3, activity.minimumResponseItems)
        assertEquals(18, activity.minimumWordCount)
    }

    @Test
    fun `toActivities infers reading and listening summary requirements`() {
        val catalog = BookCatalog(
            version = 2,
            generatedAt = "2026-04-02T13:08:48.105482+00:00",
            books = listOf(
                BookSeed(
                    id = "mixed-skills",
                    title = "Mixed Skills Source",
                    author = "Example Author",
                    cefr = listOf("B2"),
                    sourceType = "curated_notes",
                    tags = listOf("mixed"),
                    chapters = listOf(
                        BookChapter(
                            id = "reading-summary",
                            title = "Reading summary",
                            order = 1,
                            cefr = listOf("B2"),
                            tags = listOf("reading", "argument", "tone"),
                            summary = "Summarize the article and identify the writer's stance.",
                            points = listOf("The article argues for clearer regulation."),
                            examples = emptyList(),
                            pitfalls = emptyList(),
                            practicePrompts = listOf(
                                BookPracticePrompt(
                                    id = "reading-summary-prompt-1",
                                    type = ExerciseType.READ_AND_SUMMARIZE,
                                    targetSkill = SourceTargetSkill.READING,
                                    prompt = "Read the article and summarize the argument."
                                )
                            ),
                            related = emptyList(),
                            metadata = emptyMap()
                        ),
                        BookChapter(
                            id = "listening-summary",
                            title = "Listening summary",
                            order = 2,
                            cefr = listOf("B2"),
                            tags = listOf("listening", "debate", "contrast"),
                            summary = "Summarize the speaker's final position and the nuance.",
                            points = listOf("The speaker accepts one benefit but still prefers stricter rules."),
                            examples = emptyList(),
                            pitfalls = emptyList(),
                            practicePrompts = listOf(
                                BookPracticePrompt(
                                    id = "listening-summary-prompt-1",
                                    type = ExerciseType.LISTEN_AND_SUMMARIZE,
                                    targetSkill = SourceTargetSkill.LISTENING,
                                    prompt = "Listen to the debate and summarize the conclusion."
                                )
                            ),
                            related = emptyList(),
                            metadata = emptyMap()
                        )
                    )
                )
            )
        )

        val activities = BookCatalogMapper.toActivities(catalog)
        val readingActivity = activities.first { it.id == "reading-summary-prompt-1" }
        val listeningActivity = activities.first { it.id == "listening-summary-prompt-1" }

        assertEquals(SkillType.READING, readingActivity.skill)
        assertEquals(2, readingActivity.minimumKeywordMatches)
        assertEquals(true, readingActivity.requiresToneReference)
        assertEquals(SkillType.LISTENING, listeningActivity.skill)
        assertEquals(2, listeningActivity.minimumKeywordMatches)
        assertEquals(true, listeningActivity.requiresContrastMarker)
    }
}
