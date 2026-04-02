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

        assertTrue(catalog.version >= 2)
        assertTrue(catalog.books.isNotEmpty(), "Expected at least one book in the content repository asset")
        assertTrue(
            catalog.books.any { book ->
                book.chapters.any { chapter -> chapter.practicePrompts.isNotEmpty() }
            },
            "Expected at least one chapter with practice prompts in the content repository asset"
        )
    }
}
