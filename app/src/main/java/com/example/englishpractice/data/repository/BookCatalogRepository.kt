package com.example.englishpractice.data.repository

import android.content.Context
import com.example.englishpractice.domain.model.CefrLevel
import com.example.englishpractice.ui.app.PracticeActivityItem

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
        }.getOrNull()
    }

    companion object {
        const val BOOK_CATALOG_ASSET_PATH = "content/content_repository.json"
    }
}
