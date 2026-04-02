package com.example.englishpractice.data.repository

import android.content.Context
import com.example.englishpractice.domain.model.CefrLevel
import com.example.englishpractice.domain.model.SkillType
import com.example.englishpractice.ui.app.PracticeActivityItem

data class PracticeUnitAsset(
    val id: String,
    val title: String,
    val level: CefrLevel,
    val skill: SkillType,
    val description: String
)

class AssetContentRepository(private val context: Context) : ContentRepository {
    override fun loadLevels(): List<CefrLevel> {
        return runCatching {
            val rawJson = context.assets.open("content/levels.json").bufferedReader().use { it.readText() }
            AssetContentParser.parseLevels(rawJson)
        }.getOrElse { emptyList() }
    }

    override fun loadActivitiesForLevel(level: CefrLevel): List<PracticeActivityItem> {
        val assetFileName = when (level) {
            CefrLevel.B2 -> "content/activities_b2.json"
            CefrLevel.C1 -> "content/activities_c1.json"
            else -> null
        } ?: return emptyList()

        return runCatching {
            val rawJson = context.assets.open(assetFileName).bufferedReader().use { it.readText() }
            AssetContentParser.parseActivities(rawJson)
        }.getOrElse { emptyList() }
    }

    override fun loadUnitsForLevel(level: CefrLevel): List<PracticeUnitAsset> {
        val assetFileName = when (level) {
            CefrLevel.B2 -> "content/units_b2.json"
            CefrLevel.C1 -> "content/units_c1.json"
            else -> null
        } ?: return emptyList()

        return runCatching {
            val rawJson = context.assets.open(assetFileName).bufferedReader().use { it.readText() }
            AssetContentParser.parseUnits(rawJson)
        }.getOrElse { emptyList() }
    }
}
