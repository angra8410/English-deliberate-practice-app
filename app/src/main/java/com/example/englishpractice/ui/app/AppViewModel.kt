package com.example.englishpractice.ui.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.englishpractice.data.repository.AssetContentRepository
import com.example.englishpractice.data.repository.AppPreferencesRepository
import com.example.englishpractice.data.repository.BookCatalogRepository
import com.example.englishpractice.data.repository.BookCatalogLoadDiagnostics
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
import com.example.englishpractice.feature.speaking.SpeakingEvaluator
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
        val dailyPlan: List<DailyPracticeItem>,
        val contentBrowserItems: List<ContentBrowserItem>
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
            contentBrowserItems = defaultLevelContent.contentBrowserItems,
            dailyPlan = defaultLevelContent.dailyPlan,
            progressInputs = baseProgressInputs,
            weakPatterns = defaultWeakPatterns(),
            reviewQueue = defaultReviewQueue(defaultLevelContent.activityCatalog),
            recentAttempts = emptyList(),
            speakingLocaleTag = defaultSpeakingLocaleTag
        )
    )
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        observePreferences()
        refreshPersistedState(defaultPilotLevel)
    }

    fun getActivity(activityId: String): PracticeActivityItem? {
        return _uiState.value.activityCatalog.firstOrNull { activity -> activity.id == activityId }
    }

    fun getFirstActivityForSkill(skill: SkillType): PracticeActivityItem? {
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
        activityId: String,
        answer: String,
        transcriptText: String? = null
    ) {
        val activity = getActivity(activityId) ?: return
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
                    skill = activity.skill,
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
        contentBrowserItems: List<ContentBrowserItem>,
        dailyPlan: List<DailyPracticeItem>,
        progressInputs: List<SkillProgressInput>,
        weakPatterns: List<WeakPattern>,
        reviewQueue: List<ReviewQueueItem>,
        recentAttempts: List<ActivityAttemptRecord>,
        speakingLocaleTag: String
    ): AppUiState {
        val skillProgress = progressInputs.map(ProgressCalculator::buildSnapshot)
        val contentSourceSummaries = activityCatalog
            .groupBy(PracticeActivityItem::sourceLabel)
            .map { (sourceLabel, items) ->
                ContentSourceSummary(
                    sourceLabel = sourceLabel,
                    activityCount = items.size,
                    listeningCount = items.count { activity -> activity.skill == SkillType.LISTENING }
                )
            }
            .sortedBy(ContentSourceSummary::sourceLabel)
        val bookCatalogDiagnostics = BookCatalogRepository.diagnostics()

        return AppUiState(
            currentLevel = currentLevel,
            targetLevel = pilotLevels.lastOrNull() ?: CefrLevel.C1,
            streakDays = 0,
            dailyGoalMinutes = 60,
            pilotLevels = pilotLevels,
            overallCompletion = ProgressCalculator.overallCompletion(skillProgress),
            dailyPlan = dailyPlan,
            skillProgress = skillProgress,
            weakPatterns = weakPatterns,
            reviewSummary = ReviewSummary(
                dueToday = reviewQueue.size,
                recurringPatterns = weakPatterns.size.coerceAtMost(5),
                nextCheckpointDays = if (reviewQueue.isEmpty()) 0 else ReviewScheduler.nextIntervalDays(
                    previousIntervalDays = 3,
                    wasSuccessful = true
                )
            ),
            reviewQueue = reviewQueue,
            activityCatalog = activityCatalog,
            contentBrowserItems = contentBrowserItems,
            contentSourceSummaries = contentSourceSummaries,
            bookCatalogStatusMessage = buildBookCatalogStatusMessage(
                diagnostics = bookCatalogDiagnostics,
                sourceSummaries = contentSourceSummaries
            ),
            recentAttempts = recentAttempts,
            selectedSpeakingLocaleTag = speakingLocaleTag,
            speakingCapability = speakingManager.capability(),
            listeningCapability = listeningPlayer.capability()
        )
    }

    private fun buildLevelContent(level: CefrLevel): LevelContent {
        val unitCatalog = buildUnitCatalog(level)
        val activityCatalog = attachUnitMetadata(
            activities = buildActivityCatalog(level),
            units = unitCatalog
        )
        return LevelContent(
            activityCatalog = activityCatalog,
            dailyPlan = buildDailyPlan(unitCatalog, activityCatalog),
            contentBrowserItems = buildContentBrowser(unitCatalog, activityCatalog)
        )
    }

    private fun normalizePilotLevel(level: CefrLevel): CefrLevel {
        return level.takeIf { candidate -> candidate in pilotLevels } ?: defaultPilotLevel
    }

    private fun buildProgressInputs(): List<SkillProgressInput> {
        return listOf(
            SkillProgressInput(
                skill = SkillType.READING,
                completedActivities = 0,
                targetActivities = 10,
                averageScore = 0,
                weakTags = emptyList()
            ),
            SkillProgressInput(
                skill = SkillType.WRITING,
                completedActivities = 0,
                targetActivities = 10,
                averageScore = 0,
                weakTags = emptyList()
            ),
            SkillProgressInput(
                skill = SkillType.LISTENING,
                completedActivities = 0,
                targetActivities = 10,
                averageScore = 0,
                weakTags = emptyList()
            ),
            SkillProgressInput(
                skill = SkillType.SPEAKING,
                completedActivities = 0,
                targetActivities = 10,
                averageScore = 0,
                weakTags = emptyList()
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
                    sourceLabel = unit.sourceLabel,
                    collectionTitle = activity?.collectionTitle
                )
            }
        } else {
            fallbackDailyPlan()
        }
    }

    private fun buildContentBrowser(
        units: List<PracticeUnitAsset>,
        activities: List<PracticeActivityItem>
    ): List<ContentBrowserItem> {
        val unitsById = units.associateBy { unit -> unit.id }
        return activities.map { activity ->
            val unit = activity.unitId?.let(unitsById::get)
            ContentBrowserItem(
                activityId = activity.id,
                collectionTitle = activity.collectionTitle,
                unitTitle = unit?.title ?: activity.title,
                title = activity.title,
                skill = activity.skill,
                exerciseType = activity.exerciseType,
                sourceLabel = unit?.sourceLabel ?: activity.sourceLabel,
                tags = activity.tags,
                difficulty = activity.difficulty,
                focus = unit?.description ?: activity.supportNote,
                promptPreview = activity.prompt,
                effortLabel = buildEffortLabel(activity),
                responseTargetLabel = buildResponseTargetLabel(activity)
            )
        }.sortedWith(
            compareBy<ContentBrowserItem>(
                { skillBrowseOrder(it.skill) },
                { it.sourceLabel },
                { it.unitTitle },
                { it.title }
            )
        )
    }

    private fun fallbackDailyPlan(): List<DailyPracticeItem> {
        return listOf(
            DailyPracticeItem(
                skill = SkillType.READING,
                title = "Read and summarize an editorial",
                focus = "main idea, support, and more precise vocabulary",
                exerciseType = ExerciseType.READ_AND_SUMMARIZE,
                estimatedMinutes = 15,
                sourceLabel = "Fallback seed",
                collectionTitle = "Fallback seed"
            ),
            DailyPracticeItem(
                skill = SkillType.WRITING,
                title = "Write a short opinion response",
                focus = "clarity, connectors, and stronger collocations",
                exerciseType = ExerciseType.OPEN_TEXT,
                estimatedMinutes = 15,
                sourceLabel = "Fallback seed",
                collectionTitle = "Fallback seed"
            ),
            DailyPracticeItem(
                skill = SkillType.LISTENING,
                title = "Listen to a short debate and capture key details",
                focus = "detail recall and contrast markers",
                exerciseType = ExerciseType.LISTEN_AND_SUMMARIZE,
                estimatedMinutes = 15,
                sourceLabel = "Fallback seed",
                collectionTitle = "Fallback seed"
            ),
            DailyPracticeItem(
                skill = SkillType.SPEAKING,
                title = "Answer a prompt aloud and improve the retry",
                focus = "fluency, relevance, and longer structured answers",
                exerciseType = ExerciseType.SPEAK_RESPONSE,
                estimatedMinutes = 15,
                sourceLabel = "Fallback seed",
                collectionTitle = "Fallback seed"
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

    private fun skillBrowseOrder(skill: SkillType): Int {
        return when (skill) {
            SkillType.READING -> 0
            SkillType.WRITING -> 1
            SkillType.LISTENING -> 2
            SkillType.SPEAKING -> 3
        }
    }

    private fun buildEffortLabel(activity: PracticeActivityItem): String {
        val baseMinutes = when (activity.exerciseType) {
            ExerciseType.READ_AND_SUMMARIZE -> 12
            ExerciseType.LISTEN_AND_SUMMARIZE -> 14
            ExerciseType.SPEAK_RESPONSE -> 12
            ExerciseType.OPEN_TEXT -> 10
            ExerciseType.ERROR_CORRECTION,
            ExerciseType.SENTENCE_TRANSFORMATION -> 9

            ExerciseType.FILL_IN_BLANK,
            ExerciseType.MULTIPLE_CHOICE -> 8
        }
        val difficultyBoost = (activity.difficulty ?: 2) - 2
        return "${(baseMinutes + difficultyBoost).coerceAtLeast(6)} min"
    }

    private fun buildResponseTargetLabel(activity: PracticeActivityItem): String {
        return when {
            activity.minimumResponseItems != null -> "${activity.minimumResponseItems}+ items"
            activity.minimumWordCount != null -> "${activity.minimumWordCount}+ words"
            else -> when (activity.exerciseType) {
                ExerciseType.SPEAK_RESPONSE -> "1 complete spoken response"
                ExerciseType.READ_AND_SUMMARIZE,
                ExerciseType.LISTEN_AND_SUMMARIZE -> "1 focused summary"
                else -> "1 complete response"
            }
        }
    }

    private fun fallbackActivityCatalog(): List<PracticeActivityItem> {
        return listOf(
            PracticeActivityItem(
                id = "reading-b2-editorial",
                unitId = "b2_reading_summaries",
                skill = SkillType.READING,
                title = "Editorial summary and tone",
                sourceLabel = "Fallback seed",
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
                sourceLabel = "Fallback seed",
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
                sourceLabel = "Fallback seed",
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
                sourceLabel = "Fallback seed",
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
        return emptyList()
    }

    private fun defaultReviewQueue(
        activityCatalog: List<PracticeActivityItem>
    ): List<ReviewQueueItem> {
        return emptyList()
    }

    private fun attachUnitMetadata(
        activities: List<PracticeActivityItem>,
        units: List<PracticeUnitAsset>
    ): List<PracticeActivityItem> {
        if (activities.isEmpty() || units.isEmpty()) return activities

        val unitsById = units.associateBy { unit -> unit.id }
        return activities.map { activity ->
            val unit = activity.unitId?.let(unitsById::get)
            if (unit == null) {
                activity
            } else {
                activity.copy(unitTitle = unit.title)
            }
        }
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
                defaultReviewQueue(levelContent.activityCatalog)
            }

            _uiState.value = buildUiState(
                currentLevel = normalizedLevel,
                activityCatalog = levelContent.activityCatalog,
                contentBrowserItems = levelContent.contentBrowserItems,
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
            SkillType.READING -> ReadingEvaluator.evaluateSummary(
                answer = answer,
                expectedKeywords = activity.evaluationTargets,
                minimumKeywordMatches = activity.minimumKeywordMatches ?: 2,
                minimumWordCount = activity.minimumWordCount ?: 35,
                requiresToneReference = activity.requiresToneReference ?: true
            )
            SkillType.WRITING -> WritingFeedbackRules.evaluateAnswer(
                answer = answer,
                expectedKeywords = activity.evaluationTargets,
                scoringProfile = activity.scoringProfile,
                minimumWordCount = activity.minimumWordCount,
                minimumResponseItems = activity.minimumResponseItems
            )
            SkillType.LISTENING -> ListeningEvaluator.evaluateSummary(
                answer = answer,
                expectedKeywords = activity.evaluationTargets,
                minimumKeywordMatches = activity.minimumKeywordMatches ?: 2,
                minimumWordCount = activity.minimumWordCount ?: 25,
                requiresContrastMarker = activity.requiresContrastMarker ?: true
            )
            SkillType.SPEAKING -> SpeakingEvaluator.evaluateResponse(
                answer = answer,
                transcriptText = transcriptText ?: answer,
                expectedKeywords = activity.evaluationTargets,
                minimumKeywordMatches = activity.minimumKeywordMatches ?: 2,
                minimumWordCount = activity.minimumWordCount ?: 35
            )
        }
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

    private fun buildBookCatalogStatusMessage(
        diagnostics: BookCatalogLoadDiagnostics,
        sourceSummaries: List<ContentSourceSummary>
    ): String? {
        val loadedBookCatalogActivities = sourceSummaries
            .firstOrNull { summary -> summary.sourceLabel == "Book catalog" }
            ?.activityCount
            ?: 0

        return when {
            diagnostics.lastError != null -> {
                "Book catalog failed to load: ${diagnostics.lastError}"
            }

            loadedBookCatalogActivities == 0 && diagnostics.loadedBooks > 0 -> {
                "Book catalog asset parsed, but no book-catalog activities reached the current level."
            }

            loadedBookCatalogActivities == 0 -> {
                "Book catalog has not been loaded in this app session yet."
            }

            else -> {
                "Book catalog loaded: ${diagnostics.loadedBooks} books, ${diagnostics.loadedChapters} chapters, ${diagnostics.loadedPrompts} prompts."
            }
        }
    }
}
