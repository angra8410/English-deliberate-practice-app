package com.example.englishpractice.data.repository

import com.example.englishpractice.domain.model.CefrLevel
import com.example.englishpractice.domain.model.SkillType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AssetContentParserTest {
    @Test
    fun `parseActivities maps listening fields from assets`() {
        val rawJson = """
            [
              {
                "id": "listening-b2-summary",
                "unitId": "b2_listening_hybrid_work",
                "title": "Listening detail capture",
                "level": "B2",
                "skill": "LISTENING",
                "exerciseType": "LISTEN_AND_SUMMARIZE",
                "prompt": "Listen and summarize.",
                "instructions": "Write a short summary.",
                "starterText": "The speaker argues that...",
                "audioAsset": "audio/listening_b2_remote_work.wav",
                "listeningPromptText": "A speaker recognizes convenience, then argues for mentoring.",
                "tags": ["listening", "contrast"],
                "difficulty": 3,
                "supportNote": "Bundled audio first.",
                "evaluationTargets": ["hybrid", "mentoring"],
                "sampleAnswer": "The speaker supports hybrid work."
              }
            ]
        """.trimIndent()

        val activities = AssetContentParser.parseActivities(rawJson)

        assertEquals(1, activities.size)
        val activity = activities.single()
        assertEquals("listening-b2-summary", activity.id)
        assertEquals("b2_listening_hybrid_work", activity.unitId)
        assertEquals(SkillType.LISTENING, activity.skill)
        assertEquals("audio/listening_b2_remote_work.wav", activity.audioAssetPath)
        assertEquals(
            "A speaker recognizes convenience, then argues for mentoring.",
            activity.listeningPromptText
        )
        assertEquals("Built-in B2 track", activity.collectionTitle)
        assertEquals(listOf("listening", "contrast"), activity.tags)
        assertEquals(3, activity.difficulty)
        assertEquals("The speaker supports hybrid work.", activity.modelAnswer)
        assertEquals(listOf("hybrid", "mentoring"), activity.evaluationTargets)
    }

    @Test
    fun `parseActivities falls back to defaults when optional content is missing`() {
        val rawJson = """
            [
              {
                "id": "writing-b2-opinion",
                "title": "Opinion paragraph upgrade",
                "level": "B2",
                "skill": "WRITING",
                "exerciseType": "OPEN_TEXT",
                "prompt": "Should universities require communication courses?",
                "instructions": "Write one paragraph."
              }
            ]
        """.trimIndent()

        val activity = AssetContentParser.parseActivities(rawJson).single()

        assertNull(activity.unitId)
        assertNull(activity.audioAssetPath)
        assertNull(activity.listeningPromptText)
        assertEquals("Built-in B2 track", activity.collectionTitle)
        assertEquals(
            "Give a clear position, support it, and keep the response cohesive.",
            activity.modelAnswer
        )
        assertEquals("Keep the answer organized and natural.", activity.supportNote)
        assertEquals(emptyList(), activity.evaluationTargets)
    }

    @Test
    fun `parseUnits and levels map structured content`() {
        val levelsJson = """
            [
              { "id": "B2" },
              { "id": "C1" }
            ]
        """.trimIndent()
        val unitsJson = """
            [
              {
                "id": "b2_listening_hybrid_work",
                "title": "Listening: Hybrid Work",
                "level": "B2",
                "skill": "LISTENING",
                "description": "Listen for the speaker's final position."
              }
            ]
        """.trimIndent()

        val levels = AssetContentParser.parseLevels(levelsJson)
        val units = AssetContentParser.parseUnits(unitsJson)

        assertEquals(listOf(CefrLevel.B2, CefrLevel.C1), levels)
        assertEquals(1, units.size)
        assertEquals("b2_listening_hybrid_work", units.single().id)
        assertEquals(CefrLevel.B2, units.single().level)
        assertEquals(SkillType.LISTENING, units.single().skill)
    }
}
