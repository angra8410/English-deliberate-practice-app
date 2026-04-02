package com.example.englishpractice.data.repository

import com.example.englishpractice.domain.model.CefrLevel
import com.example.englishpractice.domain.model.ExerciseType
import com.example.englishpractice.domain.model.SkillType
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
                                    prompt = "List five phrases commonly used in job advertisements."
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
        assertEquals(1, activities.size)
        assertEquals("applying-for-a-job-prompt-2", activities.single().id)
        assertEquals("applying-for-a-job", activities.single().unitId)
        assertEquals(SkillType.WRITING, activities.single().skill)
        assertEquals("Applying for a job", activities.single().title)
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
}
