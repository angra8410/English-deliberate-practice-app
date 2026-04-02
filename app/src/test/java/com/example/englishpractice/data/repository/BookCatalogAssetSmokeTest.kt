package com.example.englishpractice.data.repository

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class BookCatalogAssetSmokeTest {
    @Test
    fun `content repository asset exists and parses`() {
        val assetFile = File("src/main/assets/content/content_repository.json")

        assertTrue(assetFile.exists(), "Expected content repository asset to exist at ${assetFile.path}")

        val catalog = BookCatalogParser.parseCatalog(assetFile.readText())
        val totalChapters = catalog.books.sumOf { book -> book.chapters.size }
        val totalPrompts = catalog.books.sumOf { book ->
            book.chapters.sumOf { chapter -> chapter.practicePrompts.size }
        }
        val readingPromptCount = catalog.books.sumOf { book ->
            book.chapters.sumOf { chapter ->
                chapter.practicePrompts.count { prompt -> prompt.type.name == "READ_AND_SUMMARIZE" }
            }
        }
        val listeningPromptCount = catalog.books.sumOf { book ->
            book.chapters.sumOf { chapter ->
                chapter.practicePrompts.count { prompt -> prompt.type.name == "LISTEN_AND_SUMMARIZE" }
            }
        }
        val speakingPromptCount = catalog.books.sumOf { book ->
            book.chapters.sumOf { chapter ->
                chapter.practicePrompts.count { prompt -> prompt.type.name == "SPEAK_RESPONSE" }
            }
        }

        assertTrue(catalog.version >= 2)
        assertTrue(catalog.books.isNotEmpty(), "Expected at least one book in the content repository asset")
        assertTrue(totalChapters >= 18, "Expected at least 18 curated chapters in the content repository asset")
        assertTrue(totalPrompts >= 21, "Expected at least 21 seeded prompts in the content repository asset")
        assertTrue(
            catalog.books.any { book ->
                book.chapters.any { chapter -> chapter.practicePrompts.isNotEmpty() }
            },
            "Expected at least one chapter with practice prompts in the content repository asset"
        )
        assertTrue(readingPromptCount >= 2, "Expected at least two seeded reading summary prompts in the content repository asset")
        assertTrue(listeningPromptCount >= 4, "Expected at least four seeded listening summary prompts in the content repository asset")
        assertTrue(speakingPromptCount >= 4, "Expected at least four seeded speaking prompts in the content repository asset")
    }
}
