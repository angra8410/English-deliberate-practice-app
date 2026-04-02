package com.example.englishpractice.data.repository

import com.example.englishpractice.domain.model.ExerciseType
import com.example.englishpractice.ui.app.PromptScoringProfile
import org.json.JSONArray
import org.json.JSONObject

object BookCatalogParser {
    fun parseCatalog(rawJson: String): BookCatalog {
        val root = JSONObject(rawJson)
        return BookCatalog(
            version = root.getInt("version"),
            generatedAt = root.getString("generatedAt"),
            books = root.getJSONArray("books").toBookSeeds()
        )
    }

    private fun JSONArray.toBookSeeds(): List<BookSeed> {
        return buildList(length()) {
            repeat(length()) { index ->
                val item = optJSONObject(index) ?: return@repeat
                add(item.toBookSeed())
            }
        }
    }

    private fun JSONObject.toBookSeed(): BookSeed {
        return BookSeed(
            id = getString("id"),
            title = getString("title"),
            author = getString("author"),
            cefr = optStringList("cefr"),
            sourceType = getString("sourceType"),
            tags = optStringList("tags"),
            chapters = getJSONArray("chapters").toBookChapters()
        )
    }

    private fun JSONArray.toBookChapters(): List<BookChapter> {
        return buildList(length()) {
            repeat(length()) { index ->
                val item = optJSONObject(index) ?: return@repeat
                add(item.toBookChapter())
            }
        }
    }

    private fun JSONObject.toBookChapter(): BookChapter {
        return BookChapter(
            id = getString("id"),
            title = getString("title"),
            order = getInt("order"),
            cefr = optStringList("cefr"),
            tags = optStringList("tags"),
            summary = getString("summary"),
            points = optStringList("points"),
            examples = optJSONArray("examples").toBookExamples(),
            pitfalls = optStringList("pitfalls"),
            practicePrompts = optJSONArray("practicePrompts").toBookPracticePrompts(),
            related = optStringList("related"),
            metadata = optJSONObject("metadata").toStringMap()
        )
    }

    private fun JSONArray?.toBookExamples(): List<BookExample> {
        if (this == null) return emptyList()
        return buildList(length()) {
            repeat(length()) { index ->
                val item = optJSONObject(index) ?: return@repeat
                add(
                    BookExample(
                        english = item.getString("english"),
                        note = item.optString("note").takeIf { it.isNotBlank() }
                    )
                )
            }
        }
    }

    private fun JSONArray?.toBookPracticePrompts(): List<BookPracticePrompt> {
        if (this == null) return emptyList()
        return buildList(length()) {
            repeat(length()) { index ->
                val item = optJSONObject(index) ?: return@repeat
                add(
                    BookPracticePrompt(
                        id = item.getString("id"),
                        type = ExerciseType.valueOf(item.getString("type").uppercase()),
                        targetSkill = SourceTargetSkill.valueOf(item.getString("targetSkill").uppercase()),
                        prompt = item.getString("prompt"),
                        instructions = item.optString("instructions").takeIf { it.isNotBlank() },
                        starterText = item.optString("starterText").takeIf { it.isNotBlank() },
                        audioAsset = item.optString("audioAsset").takeIf { it.isNotBlank() },
                        modelAnswer = item.optString("modelAnswer").takeIf { it.isNotBlank() },
                        expectedKeywords = item.optStringList("expectedKeywords"),
                        scoringProfile = item.optString("scoringProfile")
                            .takeIf { it.isNotBlank() }
                            ?.let { value -> PromptScoringProfile.valueOf(value.uppercase()) },
                        minimumWordCount = item.optInt("minimumWordCount").takeIf { value -> value > 0 },
                        minimumResponseItems = item.optInt("minimumResponseItems")
                            .takeIf { value -> value > 0 },
                        minimumKeywordMatches = item.optInt("minimumKeywordMatches")
                            .takeIf { value -> value > 0 },
                        requiresToneReference = item.optBoolean("requiresToneReference")
                            .takeUnless { !item.has("requiresToneReference") },
                        requiresContrastMarker = item.optBoolean("requiresContrastMarker")
                            .takeUnless { !item.has("requiresContrastMarker") }
                    )
                )
            }
        }
    }

    private fun JSONObject?.toStringMap(): Map<String, String> {
        if (this == null) return emptyMap()
        return keys().asSequence().associateWith { key -> optString(key) }
    }

    private fun JSONObject.optStringList(key: String): List<String> {
        val jsonArray = optJSONArray(key) ?: return emptyList()
        return buildList(jsonArray.length()) {
            repeat(jsonArray.length()) { index ->
                jsonArray.optString(index)
                    .takeIf { it.isNotBlank() }
                    ?.let(::add)
            }
        }
    }
}
