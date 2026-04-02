package com.example.englishpractice.data.repository

import com.example.englishpractice.domain.model.CefrLevel
import com.example.englishpractice.ui.app.PracticeActivityItem

class CompositeContentRepository(
    private val repositories: List<ContentRepository>
) : ContentRepository {
    override fun loadLevels(): List<CefrLevel> {
        return repositories.flatMap(ContentRepository::loadLevels).distinct()
    }

    override fun loadActivitiesForLevel(level: CefrLevel): List<PracticeActivityItem> {
        return repositories
            .flatMap { repository -> repository.loadActivitiesForLevel(level) }
            .distinctBy { it.id }
    }

    override fun loadUnitsForLevel(level: CefrLevel): List<PracticeUnitAsset> {
        return repositories
            .flatMap { repository -> repository.loadUnitsForLevel(level) }
            .distinctBy { it.id }
    }
}
