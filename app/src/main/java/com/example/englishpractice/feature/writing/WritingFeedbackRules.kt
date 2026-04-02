package com.example.englishpractice.feature.writing

import com.example.englishpractice.feature.practice.PracticeFeedback
import com.example.englishpractice.ui.app.PromptScoringProfile
import kotlin.math.max

object WritingFeedbackRules {
    private val strongerConnectors = listOf("however", "therefore", "moreover", "although", "while")
    private val basicWords = listOf("good", "bad", "nice", "thing", "very")

    fun evaluateAnswer(
        answer: String,
        expectedKeywords: List<String> = emptyList(),
        scoringProfile: PromptScoringProfile = PromptScoringProfile.DEFAULT,
        minimumWordCount: Int? = null,
        minimumResponseItems: Int? = null
    ): PracticeFeedback {
        val normalizedAnswer = answer.lowercase()
        val words = answer.split(Regex("\\s+")).filter { it.isNotBlank() }
        val feedback = mutableListOf<String>()
        val weakTags = mutableListOf<String>()
        val effectiveMinimumWordCount = minimumWordCount ?: defaultMinimumWordCount(scoringProfile)
        val coverageKeywords = coverageKeywords(expectedKeywords)
        val matchedKeywords = coverageKeywords.count { keyword ->
            normalizedAnswer.contains(keyword.lowercase())
        }

        when (scoringProfile) {
            PromptScoringProfile.LIST -> evaluateListResponse(
                answer = answer,
                normalizedAnswer = normalizedAnswer,
                words = words,
                expectedKeywords = coverageKeywords,
                matchedKeywords = matchedKeywords,
                minimumWordCount = effectiveMinimumWordCount,
                minimumResponseItems = minimumResponseItems,
                feedback = feedback,
                weakTags = weakTags
            )

            PromptScoringProfile.SENTENCE_DRILL -> evaluateSentenceDrill(
                normalizedAnswer = normalizedAnswer,
                words = words,
                minimumWordCount = effectiveMinimumWordCount,
                minimumResponseItems = minimumResponseItems,
                feedback = feedback,
                weakTags = weakTags
            )

            PromptScoringProfile.REWRITE -> evaluateRewriteTask(
                normalizedAnswer = normalizedAnswer,
                words = words,
                minimumWordCount = effectiveMinimumWordCount,
                minimumResponseItems = minimumResponseItems,
                feedback = feedback,
                weakTags = weakTags
            )

            PromptScoringProfile.DEFAULT -> evaluateOpenResponse(
                normalizedAnswer = normalizedAnswer,
                words = words,
                coverageKeywords = coverageKeywords,
                rubricKeywords = expectedKeywords,
                matchedKeywords = matchedKeywords,
                minimumWordCount = effectiveMinimumWordCount,
                feedback = feedback,
                weakTags = weakTags
            )
        }

        val score = (
            50 +
                matchedKeywords * 8 +
                minOf(words.size, effectiveMinimumWordCount) / 4 -
                weakTags.size * 6
            ).coerceIn(0, 100)

        return PracticeFeedback(
            score = score,
            feedback = feedback.distinct(),
            weakTags = weakTags.distinct()
        )
    }

    private fun evaluateOpenResponse(
        normalizedAnswer: String,
        words: List<String>,
        coverageKeywords: List<String>,
        rubricKeywords: List<String>,
        matchedKeywords: Int,
        minimumWordCount: Int,
        feedback: MutableList<String>,
        weakTags: MutableList<String>
    ) {
        if (words.size < minimumWordCount) {
            feedback += "Expand the response with a clearer reason and one supporting example."
            weakTags += "response length"
        }

        if (shouldRequireKeywordCoverage(coverageKeywords, matchedKeywords)) {
            feedback += "Use more of the target chapter language so the answer reflects the prompt more closely."
            weakTags += "target language"
        }

        if (rubricKeywords.any { keyword -> keyword.contains("connector", ignoreCase = true) }) {
            if (strongerConnectors.none { connector -> normalizedAnswer.contains(connector) }) {
                feedback += "Use a stronger connector such as however, therefore, or although."
                weakTags += "connectors"
            } else {
                feedback += "You are starting to structure the response with linking language."
            }
        }

        if (basicWords.count { word -> normalizedAnswer.contains(word) } >= 3) {
            feedback += "Replace repeated basic vocabulary with more precise wording."
            weakTags += "word choice"
        }

        if (rubricKeywords.any { keyword -> keyword.contains("opinion", ignoreCase = true) }) {
            if (!normalizedAnswer.contains("should") && !normalizedAnswer.contains("need")) {
                feedback += "Make your opinion more explicit so the position is easy to follow."
                weakTags += "task response"
            }
        }

        if (weakTags.isEmpty()) {
            feedback += "The response is well developed and uses the target language appropriately."
        }
    }

    private fun evaluateListResponse(
        answer: String,
        normalizedAnswer: String,
        words: List<String>,
        expectedKeywords: List<String>,
        matchedKeywords: Int,
        minimumWordCount: Int,
        minimumResponseItems: Int?,
        feedback: MutableList<String>,
        weakTags: MutableList<String>
    ) {
        val responseItems = extractResponseItems(answer)
        val requiredItems = minimumResponseItems ?: 5

        if (responseItems.size < requiredItems) {
            feedback += "List more target phrases so the response reaches the required coverage."
            weakTags += "item count"
        }

        if (words.size < minimumWordCount) {
            feedback += "Add a little more detail so the list feels complete rather than fragmentary."
            weakTags += "response length"
        }

        if (shouldRequireKeywordCoverage(expectedKeywords, matchedKeywords)) {
            feedback += "Include more of the key expressions from the chapter rather than generic wording."
            weakTags += "target language"
        } else {
            feedback += "The response captures several useful target expressions."
        }

        if (basicWords.count { word -> normalizedAnswer.contains(word) } >= 3) {
            feedback += "Use more precise expressions instead of repeating very basic words."
            weakTags += "word choice"
        }
    }

    private fun evaluateSentenceDrill(
        normalizedAnswer: String,
        words: List<String>,
        minimumWordCount: Int,
        minimumResponseItems: Int?,
        feedback: MutableList<String>,
        weakTags: MutableList<String>
    ) {
        val sentenceCount = normalizedAnswer.split(Regex("[.!?]+"))
            .map(String::trim)
            .count { sentence -> sentence.isNotEmpty() }
        val requiredSentenceCount = (minimumResponseItems ?: 3) * 2

        if (sentenceCount < requiredSentenceCount) {
            feedback += "Add the missing sentence pairs so both forms are practiced enough times."
            weakTags += "sentence pairs"
        }

        if (words.size < minimumWordCount) {
            feedback += "Expand the drill so each pair is expressed as a full sentence."
            weakTags += "response length"
        }

        if (weakTags.isEmpty()) {
            feedback += "The response shows the target forms clearly across the drill."
        }
    }

    private fun evaluateRewriteTask(
        normalizedAnswer: String,
        words: List<String>,
        minimumWordCount: Int,
        minimumResponseItems: Int?,
        feedback: MutableList<String>,
        weakTags: MutableList<String>
    ) {
        val sentenceCount = normalizedAnswer.split(Regex("[.!?\\n]+"))
            .map(String::trim)
            .count { sentence -> sentence.isNotEmpty() }
        val requiredSentences = minimumResponseItems ?: 1

        if (sentenceCount < requiredSentences) {
            feedback += "Rewrite all of the required items so the task is fully completed."
            weakTags += "task completion"
        }

        if (words.size < minimumWordCount) {
            feedback += "Add the missing rewrites so the corrected version is complete."
            weakTags += "response length"
        }

        if (weakTags.isEmpty()) {
            feedback += "The rewritten version is focused on the target language."
        }
    }

    private fun shouldRequireKeywordCoverage(
        expectedKeywords: List<String>,
        matchedKeywords: Int
    ): Boolean {
        if (expectedKeywords.isEmpty()) return false
        return matchedKeywords < max(1, minOf(2, expectedKeywords.size))
    }

    private fun coverageKeywords(expectedKeywords: List<String>): List<String> {
        return expectedKeywords.filterNot { keyword ->
            keyword.equals("connector", ignoreCase = true) ||
                keyword.equals("example", ignoreCase = true) ||
                keyword.equals("clear opinion", ignoreCase = true)
        }
    }

    private fun defaultMinimumWordCount(scoringProfile: PromptScoringProfile): Int {
        return when (scoringProfile) {
            PromptScoringProfile.DEFAULT -> 45
            PromptScoringProfile.LIST -> 10
            PromptScoringProfile.SENTENCE_DRILL -> 18
            PromptScoringProfile.REWRITE -> 20
        }
    }

    private fun extractResponseItems(answer: String): List<String> {
        val newlineOrSemicolonItems = answer.split(Regex("[\\n;]+"))
            .map(String::trim)
            .filter { item -> item.isNotEmpty() }

        if (newlineOrSemicolonItems.size >= 2) return newlineOrSemicolonItems

        val commaItems = answer.split(",")
            .map(String::trim)
            .filter { item -> item.isNotEmpty() }

        return if (commaItems.size >= 2) commaItems else newlineOrSemicolonItems
    }
}
