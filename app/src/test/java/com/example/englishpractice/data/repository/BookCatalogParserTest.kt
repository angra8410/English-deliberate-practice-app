package com.example.englishpractice.data.repository

import com.example.englishpractice.domain.model.ExerciseType
import com.example.englishpractice.ui.app.PromptScoringProfile
import kotlin.test.Test
import kotlin.test.assertEquals

class BookCatalogParserTest {
    @Test
    fun `parseCatalog maps prompt ids source skills and optional evaluation metadata`() {
        val rawJson = """
            {
              "version": 2,
              "generatedAt": "2026-04-02T13:08:48.105482+00:00",
              "books": [
                {
                  "id": "english-vocabulary-in-use-advanced",
                  "title": "English Vocabulary in Use Advanced",
                  "author": "Michael McCarthy; Felicity O'Dell",
                  "cefr": ["C1", "C2"],
                  "sourceType": "curated_notes",
                  "tags": ["vocabulary"],
                  "chapters": [
                    {
                      "id": "applying-for-a-job",
                      "title": "Applying for a job",
                      "order": 3,
                      "cefr": ["C1"],
                      "tags": ["vocabulary", "jobs"],
                      "summary": "Vocabulary for job ads.",
                      "points": ["Applications should stand out clearly."],
                      "examples": [
                        { "english": "Please find attached my CV.", "note": "formal application language" }
                      ],
                      "pitfalls": ["Avoid overly casual language."],
                      "practicePrompts": [
                        {
                          "id": "applying-for-a-job-prompt-2",
                          "type": "open_text",
                          "targetSkill": "VOCABULARY",
                          "prompt": "List five phrases commonly used in job advertisements.",
                          "instructions": "List five phrases from authentic job ads.",
                          "modelAnswer": "competitive salary; excellent career prospects",
                          "expectedKeywords": ["competitive salary", "career prospects"],
                          "scoringProfile": "list",
                          "minimumWordCount": 10,
                          "minimumResponseItems": 5
                        }
                      ],
                      "related": ["job-interviews"],
                      "metadata": { "sourceFile": "vocabulary\\\\unit-3.json" }
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val catalog = BookCatalogParser.parseCatalog(rawJson)
        val prompt = catalog.books.single().chapters.single().practicePrompts.single()

        assertEquals(2, catalog.version)
        assertEquals("applying-for-a-job-prompt-2", prompt.id)
        assertEquals(ExerciseType.OPEN_TEXT, prompt.type)
        assertEquals(SourceTargetSkill.VOCABULARY, prompt.targetSkill)
        assertEquals("List five phrases from authentic job ads.", prompt.instructions)
        assertEquals(PromptScoringProfile.LIST, prompt.scoringProfile)
        assertEquals(listOf("competitive salary", "career prospects"), prompt.expectedKeywords)
        assertEquals(10, prompt.minimumWordCount)
        assertEquals(5, prompt.minimumResponseItems)
    }
}
