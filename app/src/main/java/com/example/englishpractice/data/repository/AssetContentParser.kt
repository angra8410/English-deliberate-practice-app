package com.example.englishpractice.data.repository

import com.example.englishpractice.domain.model.CefrLevel
import com.example.englishpractice.domain.model.ExerciseType
import com.example.englishpractice.domain.model.SkillType
import com.example.englishpractice.ui.app.PracticeActivityItem
import com.example.englishpractice.ui.app.PromptScoringProfile
import org.json.JSONArray
import org.json.JSONObject

object AssetContentParser {
    fun parseLevels(rawJson: String): List<CefrLevel> {
        val levelArray = JSONArray(rawJson)
        return buildList(levelArray.length()) {
            repeat(levelArray.length()) { index ->
                val item = levelArray.optJSONObject(index) ?: return@repeat
                item.optNullableString("id")
                    ?.let(CefrLevel::valueOf)
                    ?.let(::add)
            }
        }
    }

    fun parseActivities(rawJson: String): List<PracticeActivityItem> {
        val activityArray = JSONArray(rawJson)
        return buildList(activityArray.length()) {
            repeat(activityArray.length()) { index ->
                val item = activityArray.optJSONObject(index) ?: return@repeat
                add(item.toPracticeActivity())
            }
        }
    }

    fun parseUnits(rawJson: String): List<PracticeUnitAsset> {
        val unitArray = JSONArray(rawJson)
        return buildList(unitArray.length()) {
            repeat(unitArray.length()) { index ->
                val item = unitArray.optJSONObject(index) ?: return@repeat
                add(item.toUnitAsset())
            }
        }
    }

    private fun JSONObject.toPracticeActivity(): PracticeActivityItem {
        val skill = SkillType.valueOf(getString("skill"))
        return PracticeActivityItem(
            id = getString("id"),
            unitId = optNullableString("unitId"),
            skill = skill,
            title = getString("title"),
            instructions = getString("instructions"),
            prompt = getString("prompt"),
            exerciseType = ExerciseType.valueOf(getString("exerciseType")),
            starterText = optString("starterText"),
            audioAssetPath = optNullableString("audioAsset"),
            listeningPromptText = optNullableString("listeningPromptText"),
            modelAnswer = optNullableString("modelAnswer")
                ?: optNullableString("sampleAnswer")
                ?: defaultModelAnswer(skill),
            evaluationTargets = optStringList("evaluationTargets"),
            supportNote = optNullableString("supportNote") ?: defaultSupportNote(skill),
            scoringProfile = optNullableString("scoringProfile")
                ?.let { value -> PromptScoringProfile.valueOf(value.uppercase()) }
                ?: PromptScoringProfile.DEFAULT,
            minimumWordCount = optInt("minimumWordCount").takeIf { value -> value > 0 },
            minimumResponseItems = optInt("minimumResponseItems").takeIf { value -> value > 0 },
            minimumKeywordMatches = optInt("minimumKeywordMatches").takeIf { value -> value > 0 },
            requiresToneReference = optBoolean("requiresToneReference")
                .takeUnless { !has("requiresToneReference") },
            requiresContrastMarker = optBoolean("requiresContrastMarker")
                .takeUnless { !has("requiresContrastMarker") }
        )
    }

    private fun JSONObject.toUnitAsset(): PracticeUnitAsset {
        return PracticeUnitAsset(
            id = getString("id"),
            title = getString("title"),
            level = CefrLevel.valueOf(getString("level")),
            skill = SkillType.valueOf(getString("skill")),
            description = getString("description"),
            sourceLabel = "Built-in assets"
        )
    }

    private fun JSONObject.optStringList(key: String): List<String> {
        val jsonArray = optJSONArray(key) ?: return emptyList()
        return buildList(jsonArray.length()) {
            repeat(jsonArray.length()) { index ->
                jsonArray.optString(index)
                    .takeIf { value -> value.isNotBlank() }
                    ?.let(::add)
            }
        }
    }

    private fun JSONObject.optNullableString(key: String): String? {
        return optString(key).takeIf { value -> value.isNotBlank() }
    }

    private fun defaultModelAnswer(skill: SkillType): String {
        return when (skill) {
            SkillType.READING -> "State the main idea, one supporting detail, and the writer's tone."
            SkillType.WRITING -> "Give a clear position, support it, and keep the response cohesive."
            SkillType.LISTENING -> "Capture the final position and one important contrasting detail."
            SkillType.SPEAKING -> "Answer directly, extend with a reason, and use at least one connector."
        }
    }

    private fun defaultSupportNote(skill: SkillType): String {
        return when (skill) {
            SkillType.READING -> "Focus on the main point and one strong supporting detail."
            SkillType.WRITING -> "Keep the answer organized and natural."
            SkillType.LISTENING -> "Listen once for the main idea, then again for contrast or nuance."
            SkillType.SPEAKING -> "Aim for a complete spoken response with a clear position."
        }
    }
}
