package com.example.englishpractice.data.repository

import com.example.englishpractice.domain.model.CefrLevel
import com.example.englishpractice.domain.model.SkillType
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BuiltInC1AssetSmokeTest {
    @Test
    fun `c1 built in assets cover all four skills`() {
        val activitiesFile = File("src/main/assets/content/activities_c1.json")
        val unitsFile = File("src/main/assets/content/units_c1.json")

        assertTrue(activitiesFile.exists(), "Expected C1 activities asset to exist at ${activitiesFile.path}")
        assertTrue(unitsFile.exists(), "Expected C1 units asset to exist at ${unitsFile.path}")

        val activities = AssetContentParser.parseActivities(activitiesFile.readText())
        val units = AssetContentParser.parseUnits(unitsFile.readText())

        assertEquals(
            setOf(SkillType.READING, SkillType.WRITING, SkillType.LISTENING, SkillType.SPEAKING),
            activities.map { activity -> activity.skill }.toSet()
        )
        assertEquals(
            setOf(SkillType.READING, SkillType.WRITING, SkillType.LISTENING, SkillType.SPEAKING),
            units.map { unit -> unit.skill }.toSet()
        )
        assertTrue(
            units.all { unit -> unit.level == CefrLevel.C1 },
            "Expected all built-in C1 units to stay on the C1 level"
        )
    }
}
