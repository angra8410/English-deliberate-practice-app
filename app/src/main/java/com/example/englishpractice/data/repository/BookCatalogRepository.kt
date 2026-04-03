package com.example.englishpractice.data.repository

import android.content.Context
import android.util.Log
import com.example.englishpractice.domain.model.CefrLevel
import com.example.englishpractice.ui.app.PracticeActivityItem

data class BookCatalogLoadDiagnostics(
    val loadedBooks: Int = 0,
    val loadedChapters: Int = 0,
    val loadedPrompts: Int = 0,
    val lastError: String? = null
)

class BookCatalogRepository(private val context: Context) : ContentRepository {
    override fun loadLevels(): List<CefrLevel> {
        return loadCatalog()?.let(BookCatalogMapper::toLevels).orEmpty()
    }

    override fun loadActivitiesForLevel(level: CefrLevel): List<PracticeActivityItem> {
        val catalog = loadCatalog() ?: return emptyList()
        val unitIdsForLevel = BookCatalogMapper.toUnits(catalog)
            .asSequence()
            .filter { it.level == level }
            .map { it.id }
            .toSet()

        return BookCatalogMapper.toActivities(catalog)
            .filter { activity -> activity.unitId in unitIdsForLevel }
    }

    override fun loadUnitsForLevel(level: CefrLevel): List<PracticeUnitAsset> {
        return loadCatalog()
            ?.let(BookCatalogMapper::toUnits)
            .orEmpty()
            .filter { it.level == level }
    }

    private fun loadCatalog(): BookCatalog? {
        return runCatching {
            val rawJson = context.assets.open(BOOK_CATALOG_ASSET_PATH).bufferedReader().use { it.readText() }
            BookCatalogParser.parseCatalog(rawJson)
        }.onSuccess { catalog ->
            diagnostics = BookCatalogLoadDiagnostics(
                loadedBooks = catalog.books.size,
                loadedChapters = catalog.books.sumOf { book -> book.chapters.size },
                loadedPrompts = catalog.books.sumOf { book ->
                    book.chapters.sumOf { chapter -> chapter.practicePrompts.size }
                },
                lastError = null
            )
        }.onFailure { throwable ->
            diagnostics = BookCatalogLoadDiagnostics(
                lastError = "${throwable::class.simpleName}: ${throwable.message ?: "Unknown error"}"
            )
            Log.e(TAG, "Failed to load $BOOK_CATALOG_ASSET_PATH", throwable)
        }.getOrNull()
    }

    companion object {
        const val BOOK_CATALOG_ASSET_PATH = "content/content_repository.json"
        private const val TAG = "BookCatalogRepository"
        @Volatile
        private var diagnostics: BookCatalogLoadDiagnostics = BookCatalogLoadDiagnostics()

        fun diagnostics(): BookCatalogLoadDiagnostics = diagnostics
    }
}
