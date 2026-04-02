package com.example.englishpractice.data.repository

import com.example.englishpractice.domain.model.CefrLevel
import com.example.englishpractice.ui.app.PracticeActivityItem

interface ContentRepository {
    fun loadLevels(): List<CefrLevel>

    fun loadActivitiesForLevel(level: CefrLevel): List<PracticeActivityItem>

    fun loadUnitsForLevel(level: CefrLevel): List<PracticeUnitAsset>
}
