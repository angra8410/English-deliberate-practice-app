package com.example.englishpractice.ui.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.englishpractice.data.repository.AssetContentRepository
import com.example.englishpractice.data.repository.AppPreferencesRepository
import com.example.englishpractice.data.repository.BookCatalogRepository
import com.example.englishpractice.data.repository.CompositeContentRepository
import com.example.englishpractice.data.repository.ContentRepository
import com.example.englishpractice.data.repository.PersistedSubmission
import com.example.englishpractice.data.repository.PracticeUnitAsset
import com.example.englishpractice.data.repository.PracticeRepository
import com.example.englishpractice.domain.model.CefrLevel
import com.example.englishpractice.domain.model.ExerciseType
import com.example.englishpractice.domain.model.SkillType
import com.example.englishpractice.feature.listening.ListeningEvaluator
import com.example.englishpractice.feature.listening.ListeningPlayer
import com.example.englishpractice.feature.practice.PracticeFeedback
import com.example.englishpractice.feature.progress.ProgressCalculator
import com.example.englishpractice.feature.progress.SkillProgressInput
import com.example.englishpractice.feature.reading.ReadingEvaluator
import com.example.englishpractice.feature.review.ReviewScheduler
import com.example.englishpractice.feature.speaking.SpeakingManager
import com.example.englishpractice.feature.writing.WritingFeedbackRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AppViewModel(
    application: Application,
    private val contentRepository: ContentRepository
) : AndroidViewModel(application) {
    private data class LevelContent(
        val activityCatalog: List<PracticeActivityItem>,
        val dailyPlan: List<DailyPracticeItem>
    )

    constructor(application: Application) : this(
        application = application,
        contentRepository = CompositeContentRepository(
            listOf(
                AssetContentRepository(application),
                BookCatalogRepository(application)
            )
        )
    )

    private val speakingManager = SpeakingManager(application)
    private val listeningPlayer = ListeningPlayer(application)
    private val repository = PracticeRepository.create(application)
    private val preferencesRepository = AppPreferencesRepository(application)
    private val pilotLevels = buildPilotLevels()
    private val defaultPilotLevel = pilotLevels.firstOrNull { level ->
        level == AppPreferencesRepository.DEFAULT_PILOT_LEVEL
    } ?: pilotLevels.firstOrNull() ?: AppPreferencesRepository.DEFAULT_PILOT_LEVEL
    private val defaultLevelContent = buildLevelContent(defaultPilotLevel)
    private val baseProgressInputs = buildProgressInputs()
    private val defaultSpeakingLocaleTag = AppPreferencesRepository.DEFAULT_SPEAKING_LOCALE_TAG
    private val selectedPilotLevel = MutableStateFlow(defaultPilotLevel)
    private val selectedSpeakingLocaleTag = MutableStateFlow(defaultSpeakingLocaleTag)

    private val _uiState = MutableStateFlow(
        buildUiState(
            currentLevel = defaultPilotLevel,
            activityCatalog = defaultLevelContent.activityCatalog,
            dailyPlan = defaultLevelContent.dailyPlan,
            progressInputs = baseProgressInputs,
            weakPatterns = defaultWeakPatterns(),
            reviewQueue = defaultReviewQueue(),
            recentAttempts = emptyList(),
            speakingLocaleTag = defaultSpeakingLocaleTag
        )
    )
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        observePreferences()
        refreshPersistedState(defaultPilotLevel)
    }

    fun getActivity(skill: SkillType): PracticeActivityItem? {
        return _uiState.value.activityCatalog.firstOrNull { activity -> activity.skill == skill }
    }

    fun updatePilotLevel(level: CefrLevel) {
        val normalizedLevel = normalizePilotLevel(level)
        if (selectedPilotLevel.value == normalizedLevel) return
        selectedPilotLevel.value = normalizedLevel
        refreshPersistedState(normalizedLevel)
        viewModelScope.launch {
            preferencesRepository.setPilotLevel(normalizedLevel)
        }
    }

    fun updateSpeakingLocale(localeTag: String) {
        selectedSpeakingLocaleTag.value = localeTag
        _uiState.value = _uiState.value.copy(selectedSpeakingLocaleTag = localeTag)
        viewModelScope.launch {
            preferencesRepository.setSpeakingLocaleTag(localeTag)
        }
    }

    fun submitActivity(
        skill: SkillType,
        answer: String,
        transcriptText: String? = null
    ) {
        val activity = getActivity(skill) ?: return
        val normalizedAnswer = answer.trim()
        if (normalizedAnswer.isBlank()) return

        val feedback = evaluateActivity(
            activity = activity,
            answer = normalizedAnswer,
            transcriptText = transcriptText
        )

        viewModelScope.launch {
            repository.saveSubmission(
                PersistedSubmission(
                    activity = activity,
                    skill = skill,
                    answer = normalizedAnswer,
                    transcriptText = transcriptText,
                    score = feedback.score,
                    feedback = feedback.feedback,
                    weakTags = feedback.weakTags
                )
            )
            refreshPersistedState(selectedPilotLevel.value)
        }
    }

    private fun buildUiState(
        currentLevel: CefrLevel,
        activityCatalog: List<PracticeActivityItem>,
        dailyPlan: List<DailyPracticeItem>,
        progressInputs: List<SkillProgressInput>,
        weakPatterns: List<WeakPattern>,
        reviewQueue: List<ReviewQueueItem>,
        recentAttempts: List<ActivityAttemptRecord>,
        speakingLocaleTag: String
    ): AppUiState {
        val skillProgress = progressInputs.map(ProgressCalculator::buildSnapshot)

        return AppUiState(
            currentLevel = currentLevel,
            targetLevel = pilotLevels.lastOrNull() ?: CefrLevel.C1,
            streakDays = 12,
            dailyGoalMinutes = 60,
            pilotLevels = pilotLevels,
            overallCompletion = ProgressCalculator.overallCompletion(skillProgress),
            dailyPlan = dailyPlan,
            skillProgress = skillProgress,
            weakPatterns = weakPatterns,
            reviewSummary = ReviewSummary(
                dueToday = reviewQueue.size,
                recurringPatterns = weakPatterns.size.coerceAtMost(5),
                nextCheckpointDays = ReviewScheduler.nextIntervalDays(
                    previousIntervalDays = 3,
                    wasSuccessful = true
                )
            ),
            reviewQueue = reviewQueue,
            activityCatalog = activityCatalog,
            recentAttempts = recentAttempts,
            selectedSpeakingLocaleTag = speakingLocaleTag,
            speakingCapability = speakingManager.capability(),
            listeningCapability = listeningPlayer.capability()
        )
    }

    private fun buildLevelContent(level: CefrLevel): LevelContent {
        val activityCatalog = buildActivityCatalog(level)
        val unitCatalog = buildUnitCatalog(level)
        return LevelContent(
            activityCatalog = activityCatalog,
            dailyPlan = buildDailyPlan(unitCatalog, activityCatalog)
        )
    }

    private fun normalizePilotLevel(level: CefrLevel): CefrLevel {
        return level.takeIf { candidate -> candidate in pilotLevels } ?: defaultPilotLevel
    }

    private fun buildProgressInputs(): List<SkillProgressInput> {
        return listOf(
            SkillProgressInput(
                skill = SkillType.READING,
                completedActivities = 8,
                targetActivities = 10,
                averageScore = 81,
                weakTags = listOf("tone inference", "supporting ideas")
            ),
            SkillProgressInput(
                skill = SkillType.WRITING,
                completedActivities = 6,
                targetActivities = 10,
                averageScore = 74,
                weakTags = listOf("collocations", "paragraph structure", "connectors")
            ),
            SkillProgressInput(
                skill = SkillType.LISTENING,
                completedActivities = 7,
                targetActivities = 10,
                averageScore = 77,
                weakTags = listOf("contrast markers", "detail recall")
            ),
            SkillProgressInput(
                skill = SkillType.SPEAKING,
                completedActivities = 5,
                targetActivities = 10,
                averageScore = 69,
                weakTags = listOf("response length", "connector range", "task relevance")
            )
        )
    }

    private fun buildActivityCatalog(level: CefrLevel): List<PracticeActivityItem> {
        val assetActivities = contentRepository.loadActivitiesForLevel(level)
        return if (assetActivities.isNotEmpty()) {
            assetActivities
        } else {
            fallbackActivityCatalog()
        }
    }

    private fun buildUnitCatalog(level: CefrLevel): List<PracticeUnitAsset> {
        return contentRepository.loadUnitsForLevel(level)
    }

    private fun buildPilotLevels(): List<CefrLevel> {
        val levelsWithActivities = contentRepository.loadLevels().filter { level ->
            contentRepository.loadActivitiesForLevel(level).isNotEmpty()
        }.sortedBy { level -> level.ordinal }
        return if (levelsWithActivities.isNotEmpty()) {
            levelsWithActivities
        } else {
            listOf(CefrLevel.B2, CefrLevel.C1)
        }
    }

    private fun buildDailyPlan(
        units: List<PracticeUnitAsset>,
        activities: List<PracticeActivityItem>
    ): List<DailyPracticeItem> {
        val activityByUnitId = activities.mapNotNull { activity ->
            activity.unitId?.let { unitId -> unitId to activity }
        }.toMap()

        val plannedUnits = units.sortedBy { unit ->
            when (unit.skill) {
                SkillType.READING -> 0
                SkillType.WRITING -> 1
                SkillType.LISTENING -> 2
                SkillType.SPEAKING -> 3
            }
        }

        return if (plannedUnits.isNotEmpty()) {
            plannedUnits.map { unit ->
                val activity = activityByUnitId[unit.id]
                DailyPracticeItem(
                    skill = unit.skill,
                    title = unit.title,
                    focus = unit.description,
                    exerciseType = activity?.exerciseType ?: defaultExerciseType(unit.skill),
                    estimatedMinutes = 15,
                    sourceLabel = unit.sourceLabel
                )
            }
        } else {
            fallbackDailyPlan()
        }
    }

    private fun fallbackDailyPlan(): List<DailyPracticeItem> {
        return listOf(
            DailyPracticeItem(
                skill = SkillType.READING,
                title = "Read and summarize an editorial",
                focus = "main idea, support, and more precise vocabulary",
                exerciseType = ExerciseType.READ_AND_SUMMARIZE,
                estimatedMinutes = 15,
                sourceLabel = "Fallback seed"
            ),
            DailyPracticeItem(
                skill = SkillType.WRITING,
                title = "Write a short opinion response",
                focus = "clarity, connectors, and stronger collocations",
                exerciseType = ExerciseType.OPEN_TEXT,
                estimatedMinutes = 15,
                sourceLabel = "Fallback seed"
            ),
            DailyPracticeItem(
                skill = SkillType.LISTENING,
                title = "Listen to a short debate and capture key details",
                focus = "detail recall and contrast markers",
                exerciseType = ExerciseType.LISTEN_AND_SUMMARIZE,
                estimatedMinutes = 15,
                sourceLabel = "Fallback seed"
            ),
            DailyPracticeItem(
                skill = SkillType.SPEAKING,
                title = "Answer a prompt aloud and improve the retry",
                focus = "fluency, relevance, and longer structured answers",
                exerciseType = ExerciseType.SPEAK_RESPONSE,
                estimatedMinutes = 15,
                sourceLabel = "Fallback seed"
            )
        )
    }

    private fun defaultExerciseType(skill: SkillType): ExerciseType {
        return when (skill) {
            SkillType.READING -> ExerciseType.READ_AND_SUMMARIZE
            SkillType.WRITING -> ExerciseType.OPEN_TEXT
            SkillType.LISTENING -> ExerciseType.LISTEN_AND_SUMMARIZE
            SkillType.SPEAKING -> ExerciseType.SPEAK_RESPONSE
        }
    }

    private fun fallbackActivityCatalog(): List<PracticeActivityItem> {
        return listOf(
            PracticeActivityItem(
                id = "reading-b2-editorial",
                unitId = "b2_reading_summaries",
                skill = SkillType.READING,
                title = "Editorial summary and tone",
                instructions = "Read the passage and write a short summary that states the main idea, one supporting point, and the writer's tone.",
                prompt = "City centers should reduce private car traffic, not because cars are inherently harmful, but because current street design prioritizes speed over public life. Supporters of the change argue that quieter streets improve health, increase local commerce, and make commuting more predictable. Critics worry that delivery times and commuter flexibility will suffer during the transition.",
                exerciseType = ExerciseType.READ_AND_SUMMARIZE,
                starterText = "The article argues that...",
                modelAnswer = "The passage argues that city centers should reduce private car traffic because current streets favor speed over community life. It supports this by linking calmer streets to health and local business benefits, while acknowledging concerns about deliveries and commuter flexibility. The tone is balanced but clearly supportive of reform.",
                evaluationTargets = listOf("city", "traffic", "health", "business", "tone"),
                supportNote = "Aim for 3 to 4 sentences and mention both the argument and the tone."
            ),
            PracticeActivityItem(
                id = "writing-b2-opinion",
                unitId = "b2_writing_opinions",
                skill = SkillType.WRITING,
                title = "Opinion paragraph upgrade",
                instructions = "Write one clear paragraph in response to the prompt. Use at least one connector and one reasoned example.",
                prompt = "Should universities require all students to take a communication course, even if it is outside their major?",
                exerciseType = ExerciseType.OPEN_TEXT,
                starterText = "Universities should...",
                modelAnswer = "Universities should require a communication course because most professions now depend on clear writing and speaking, not only technical knowledge. For example, engineers and designers often need to explain complex ideas to clients or multidisciplinary teams. Therefore, a communication course would strengthen employability and improve collaboration across fields.",
                evaluationTargets = listOf("connector", "example", "clear opinion"),
                supportNote = "Keep the paragraph focused: position, reason, and example."
            ),
            PracticeActivityItem(
                id = "listening-b2-summary",
                unitId = "b2_listening_hybrid_work",
                skill = SkillType.LISTENING,
                title = "Listening detail capture",
                instructions = "Pretend you listened to a short debate. Write a summary of the speaker's final position and one contrasting detail they mentioned.",
                prompt = "Audio prompt placeholder: a speaker first recognizes the convenience of remote work, then argues that junior employees still benefit from in-person mentoring several times per week.",
                exerciseType = ExerciseType.LISTEN_AND_SUMMARIZE,
                starterText = "The speaker ultimately argues that...",
                audioAssetPath = "audio/listening_b2_remote_work.wav",
                listeningPromptText = "A speaker first recognizes the convenience of remote work, then argues that junior employees still benefit from in-person mentoring several times per week.",
                modelAnswer = "The speaker ends up supporting a hybrid model, arguing that junior employees need regular in-person mentoring even if remote work is convenient. The contrast is that convenience matters, but training and informal feedback matter more early in a career.",
                evaluationTargets = listOf("hybrid", "mentoring", "junior", "contrast"),
                supportNote = "Playback now prefers the bundled sample audio asset and falls back to prompt playback when no file is available."
            ),
            PracticeActivityItem(
                id = "speaking-b2-argument",
                unitId = "b2_speaking_remote_work",
                skill = SkillType.SPEAKING,
                title = "Spoken argument and retry",
                instructions = "Answer the prompt aloud. For the MVP, use the transcript field or load the guided transcript sample, then submit for feedback.",
                prompt = "Do you agree that remote work is harder for junior employees than for experienced employees? Explain your position.",
                exerciseType = ExerciseType.SPEAK_RESPONSE,
                starterText = "I partly agree because junior employees usually need faster feedback and more structure. However, remote work can still work if teams create regular mentoring routines and clear communication habits.",
                modelAnswer = "I agree that remote work is usually harder for junior employees because they need quick feedback, observation, and informal learning opportunities that are easier to access in person. However, this does not mean remote work is impossible. If teams offer structured mentoring, regular check-ins, and clear expectations, junior staff can still develop effectively.",
                evaluationTargets = listOf("agree", "junior employees", "feedback", "however"),
                supportNote = "Speaking v1 uses transcript-first practice before real speech capture."
            )
        )
    }

    private fun defaultWeakPatterns(): List<WeakPattern> {
        return listOf(
            WeakPattern(
                skill = SkillType.SPEAKING,
                tag = "response length",
                note = "Speaking answers stop too early before giving evidence."
            ),
            WeakPattern(
                skill = SkillType.WRITING,
                tag = "collocations",
                note = "Writing uses basic verb + noun combinations too often."
            ),
            WeakPattern(
                skill = SkillType.LISTENING,
                tag = "contrast markers",
                note = "Listening misses turns introduced by however, although, and while."
            ),
            WeakPattern(
                skill = SkillType.READING,
                tag = "tone inference",
                note = "Reading summaries capture facts but miss stance and tone."
            )
        )
    }

    private fun defaultReviewQueue(): List<ReviewQueueItem> {
        return listOf(
            ReviewQueueItem(
                skill = SkillType.SPEAKING,
                prompt = "Argue for or against remote work for junior employees.",
                dueLabel = "Due now",
                reason = "Retry after short response and weak connector range."
            ),
            ReviewQueueItem(
                skill = SkillType.WRITING,
                prompt = "Rewrite a message so it sounds more natural and precise.",
                dueLabel = "Today",
                reason = "Recurring collocation and paragraph-structure issues."
            ),
            ReviewQueueItem(
                skill = SkillType.LISTENING,
                prompt = "Re-listen and identify the speaker's final position.",
                dueLabel = "Tomorrow",
                reason = "Detail recall dropped when the audio shifted tone."
            ),
            ReviewQueueItem(
                skill = SkillType.READING,
                prompt = "Infer tone from a short article intro and closing paragraph.",
                dueLabel = "In 3 days",
                reason = "Review for tone inference and supporting-idea selection."
            )
        )
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesRepository.pilotLevelFlow.collectLatest { level ->
                val normalizedLevel = normalizePilotLevel(level)
                selectedPilotLevel.value = normalizedLevel
                refreshPersistedState(normalizedLevel)
            }
        }
        viewModelScope.launch {
            preferencesRepository.speakingLocaleTagFlow.collectLatest { localeTag ->
                selectedSpeakingLocaleTag.value = localeTag
                _uiState.value = _uiState.value.copy(selectedSpeakingLocaleTag = localeTag)
            }
        }
    }

    private fun refreshPersistedState(level: CefrLevel) {
        viewModelScope.launch {
            val normalizedLevel = normalizePilotLevel(level)
            val levelContent = buildLevelContent(normalizedLevel)
            val snapshot = repository.loadSnapshot(levelContent.activityCatalog)
            val mergedProgressInputs = mergeProgressInputs(snapshot.skillProgressInputs)
            val effectiveWeakPatterns = if (snapshot.weakPatterns.isNotEmpty()) {
                snapshot.weakPatterns
            } else {
                defaultWeakPatterns()
            }
            val effectiveReviewQueue = if (snapshot.reviewQueue.isNotEmpty()) {
                snapshot.reviewQueue
            } else {
                defaultReviewQueue()
            }

            _uiState.value = buildUiState(
                currentLevel = normalizedLevel,
                activityCatalog = levelContent.activityCatalog,
                dailyPlan = levelContent.dailyPlan,
                progressInputs = mergedProgressInputs,
                weakPatterns = effectiveWeakPatterns,
                reviewQueue = effectiveReviewQueue,
                recentAttempts = snapshot.recentAttempts,
                speakingLocaleTag = selectedSpeakingLocaleTag.value
            )
        }
    }

    private fun evaluateActivity(
        activity: PracticeActivityItem,
        answer: String,
        transcriptText: String?
    ): PracticeFeedback {
        return when (activity.skill) {
            SkillType.READING -> ReadingEvaluator.evaluateSummary(answer, activity.evaluationTargets)
            SkillType.WRITING -> WritingFeedbackRules.evaluateAnswer(
                answer = answer,
                expectedKeywords = activity.evaluationTargets,
                scoringProfile = activity.scoringProfile,
                minimumWordCount = activity.minimumWordCount,
                minimumResponseItems = activity.minimumResponseItems
            )
            SkillType.LISTENING -> ListeningEvaluator.evaluateSummary(answer, activity.evaluationTargets)
            SkillType.SPEAKING -> evaluateSpeakingAnswer(
                answer = answer,
                transcriptText = transcriptText ?: answer,
                expectedKeywords = activity.evaluationTargets
            )
        }
    }

    private fun evaluateSpeakingAnswer(
        answer: String,
        transcriptText: String,
        expectedKeywords: List<String>
    ): PracticeFeedback {
        val normalizedTranscript = transcriptText.lowercase()
        val matchedKeywords = expectedKeywords.count { keyword ->
            normalizedTranscript.contains(keyword.lowercase())
        }
        val feedback = mutableListOf<String>()
        val weakTags = mutableListOf<String>()

        if (transcriptText.split(" ").size < 35) {
            feedback += "Extend the response with a stronger explanation and one concrete example."
            weakTags += "response length"
        }

        if (matchedKeywords < 2) {
            feedback += "Stay closer to the task by naming junior employees, feedback, or your exact position."
            weakTags += "task relevance"
        } else {
            feedback += "The response stays on topic and addresses the prompt."
        }

        if (!normalizedTranscript.contains("however") && !normalizedTranscript.contains("although")) {
            feedback += "Add a connector such as however or although to improve fluency and range."
            weakTags += "connector range"
        }

        if (answer == transcriptText) {
            feedback += "Good for v1: the transcript is stored and ready for comparison with the model answer."
        }

        val score = (46 + matchedKeywords * 15 - weakTags.size * 5).coerceIn(0, 100)
        return PracticeFeedback(score, feedback.distinct(), weakTags.distinct())
    }

    private fun mergeProgressInputs(
        persistedInputs: List<SkillProgressInput>
    ): List<SkillProgressInput> {
        val persistedBySkill = persistedInputs.associateBy { input -> input.skill }
        return baseProgressInputs.map { baseInput ->
            val persistedInput = persistedBySkill[baseInput.skill]
            if (
                persistedInput != null &&
                (
                    persistedInput.completedActivities > 0 ||
                        persistedInput.averageScore > 0 ||
                        persistedInput.weakTags.isNotEmpty()
                    )
            ) {
                persistedInput
            } else {
                baseInput
            }
        }
    }
}
