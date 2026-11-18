package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.MunazzemAiRepository
import com.example.ai.ParsedDaySchedule
import com.example.data.cloud.CloudSyncRepository
import com.example.data.cloud.DailyTask
import com.example.data.cloud.DailyTasksRepository
import com.example.data.local.*
import com.example.data.preferences.AppPreferences
import com.example.data.preferences.AppSettingsState
import com.example.domain.*
import com.example.notifications.ReminderScheduler
import com.example.ui.theme.SubjectColors
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.util.UUID

sealed interface AiDayParseState {
    data object Idle : AiDayParseState
    data object Loading : AiDayParseState
    data class Success(val parsed: ParsedDaySchedule) : AiDayParseState
    data class Error(val message: String) : AiDayParseState
}

sealed interface AiChatState {
    data object Idle : AiChatState
    data object Loading : AiChatState
    data class Streaming(val partialText: String) : AiChatState
    data class Error(val message: String) : AiChatState
}

sealed interface AiBacklogState {
    data object Idle : AiBacklogState
    data object Loading : AiBacklogState
    data class Success(val planText: String) : AiBacklogState
    data class Error(val message: String) : AiBacklogState
}

sealed interface SyncState {
    data object Idle : SyncState
    data object Syncing : SyncState
    data class Message(val text: String, val isError: Boolean = false) : SyncState
}

sealed interface DailyTasksUiState {
    data object SignedOut : DailyTasksUiState
    data object Loading : DailyTasksUiState
    data class Success(val tasks: List<DailyTask>, val dateEpochDay: Long) : DailyTasksUiState
    data class Error(val message: String) : DailyTasksUiState
}

data class FocusTimerState(
    val isRunning: Boolean = false,
    val isBreakPhase: Boolean = false,
    val focusMinutes: Int = 25,
    val breakMinutes: Int = 5,
    val remainingSeconds: Int = 25 * 60,
    val linkedSubjectId: String? = null,
    val linkedLessonId: String? = null,
    val mode: FocusModeType = FocusModeType.POMODORO,
    val fullscreenMode: Boolean = false
)

data class SearchResults(
    val subjects: List<SubjectEntity> = emptyList(),
    val lessons: List<LessonEntity> = emptyList(),
    val notes: List<StudyNoteEntity> = emptyList(),
    val exams: List<ExamEntity> = emptyList(),
    val attachments: List<StudyNoteEntity> = emptyList()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).appDao()
    private val prefs = AppPreferences(application)
    private val aiRepo = MunazzemAiRepository()
    private val cloudRepo = CloudSyncRepository(application, dao)
    private val dailyTasksRepo = DailyTasksRepository(application)

    val settings: StateFlow<AppSettingsState> = prefs.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppSettingsState())

    val profile: StateFlow<StudentProfileEntity?> = dao.observeProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val subjects: StateFlow<List<SubjectEntity>> = dao.observeSubjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val lessons: StateFlow<List<LessonEntity>> = dao.observeLessons()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val scheduleBlocks: StateFlow<List<ScheduleBlockEntity>> = dao.observeScheduleBlocks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val exams: StateFlow<List<ExamEntity>> = dao.observeExams()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val focusSessions: StateFlow<List<FocusSessionEntity>> = dao.observeFocusSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val notes: StateFlow<List<StudyNoteEntity>> = dao.observeNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val holidays: StateFlow<List<HolidayOrBreakEntity>> = dao.observeHolidays()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val notifications: StateFlow<List<AppNotificationEntity>> = dao.observeNotifications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val chatMessages: StateFlow<List<AiChatMessageEntity>> = dao.observeChatMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _overflowLessons = MutableStateFlow<List<LessonEntity>>(emptyList())
    val overflowLessons: StateFlow<List<LessonEntity>> = _overflowLessons.asStateFlow()

    private val _scheduleWarnings = MutableStateFlow<List<String>>(emptyList())
    val scheduleWarnings: StateFlow<List<String>> = _scheduleWarnings.asStateFlow()

    val analytics: StateFlow<DashboardAnalytics> = combine(
        subjects, lessons, focusSessions, exams
    ) { subjs, les, foc, exm ->
        AnalyticsEngine.compute(subjs, les, foc, exm)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        AnalyticsEngine.compute(emptyList(), emptyList(), emptyList(), emptyList())
    )

    val gamification: StateFlow<GamificationSnapshot> = combine(
        profile, lessons, focusSessions
    ) { prof, les, foc ->
        GamificationEngine.evaluate(prof, les, foc, LocalDate.now().toEpochDay())
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        GamificationEngine.evaluate(null, emptyList(), emptyList(), LocalDate.now().toEpochDay())
    )

    private val _aiDayParseState = MutableStateFlow<AiDayParseState>(AiDayParseState.Idle)
    val aiDayParseState: StateFlow<AiDayParseState> = _aiDayParseState.asStateFlow()

    private val _aiChatState = MutableStateFlow<AiChatState>(AiChatState.Idle)
    val aiChatState: StateFlow<AiChatState> = _aiChatState.asStateFlow()

    private val _aiBacklogState = MutableStateFlow<AiBacklogState>(AiBacklogState.Idle)
    val aiBacklogState: StateFlow<AiBacklogState> = _aiBacklogState.asStateFlow()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    private val _focusTimer = MutableStateFlow(FocusTimerState())
    val focusTimer: StateFlow<FocusTimerState> = _focusTimer.asStateFlow()
    private var timerJob: Job? = null

    private val _dailyTasksRefreshTick = MutableStateFlow(0)

    private val authUidFlow: Flow<String?> = callbackFlow {
        val auth = Firebase.auth
        trySend(auth.currentUser?.uid)
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.uid)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    val dailyTasksState: StateFlow<DailyTasksUiState> = combine(
        authUidFlow,
        _dailyTasksRefreshTick
    ) { uid, _ ->
        uid to LocalDate.now().toEpochDay()
    }.flatMapLatest { (uid, todayEpochDay) ->
        if (uid.isNullOrBlank()) {
            flowOf(DailyTasksUiState.SignedOut)
        } else {
            dailyTasksRepo.observeTasksForDate(uid, todayEpochDay)
                .map<List<DailyTask>, DailyTasksUiState> { tasks ->
                    DailyTasksUiState.Success(tasks = tasks, dateEpochDay = todayEpochDay)
                }
                .onStart { emit(DailyTasksUiState.Loading) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    emit(
                        DailyTasksUiState.Error(
                            rootError.localizedMessage
                                ?: "تعذر تحميل مهام اليوم من Firestore. تحقق من الاتصال أو الصلاحيات."
                        )
                    )
                }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = if (Firebase.auth.currentUser == null) DailyTasksUiState.SignedOut else DailyTasksUiState.Loading
    )

    fun clearBanner() {
        _bannerMessage.value = null
    }

    fun clearSyncState() {
        _syncState.value = SyncState.Idle
    }

    private fun canCloudSync(): Boolean {
        return settings.value.cloudSyncConsent && Firebase.auth.currentUser != null
    }

    private fun launchCloudSync(block: suspend () -> Unit) {
        if (!canCloudSync()) return
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("MainViewModel", "Background cloud sync error: ${e.message}")
            }
        }
    }

    // --- Onboarding & Profile ---
    fun completeOnboarding(
        name: String,
        stage: String,
        grade: String,
        school: String,
        governorate: String,
        studyGoal: String,
        avatarUri: String,
        initialSubjectNames: List<String>,
        enableCloudSync: Boolean
    ) {
        viewModelScope.launch {
            val cleanName = name.trim().ifBlank { "طالب" }
            val profileEntity = StudentProfileEntity(
                id = "main",
                name = cleanName,
                educationalStage = stage.trim(),
                grade = grade.trim(),
                school = school.trim(),
                governorate = governorate.trim(),
                studyGoal = studyGoal.trim(),
                avatarUri = avatarUri,
                lastActiveEpochDay = LocalDate.now().toEpochDay(),
                currentStreakDays = 1,
                longestStreakDays = 1
            )
            dao.upsertProfile(profileEntity)

            val createdSubjects = initialSubjectNames
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .mapIndexed { idx, subjName ->
                    SubjectEntity(
                        name = subjName,
                        colorHex = SubjectColors[idx % SubjectColors.size],
                        difficulty = 3,
                        priority = 3
                    )
                }
            if (createdSubjects.isNotEmpty()) {
                dao.upsertSubjects(createdSubjects)
            }

            prefs.setCloudSyncConsent(enableCloudSync)
            prefs.setOnboardingCompleted(true)

            if (enableCloudSync && Firebase.auth.currentUser != null) {
                cloudRepo.backupAllToCloud()
            }
            pushNotification(
                title = "أهلاً بك في MUNAZZEM STUDY PRO",
                body = "وقتك منظم، ومستقبلك أقرب. ابدأ بوصف يومك ليصنع لك الذكاء الاصطناعي جدولاً متوازناً.",
                category = NotificationCategory.GOAL,
                route = "dashboard"
            )
        }
    }

    fun updateProfile(updated: StudentProfileEntity) {
        viewModelScope.launch {
            val entity = updated.copy(updatedAtEpochMs = System.currentTimeMillis())
            dao.upsertProfile(entity)
            launchCloudSync { cloudRepo.syncProfile(entity) }
            _bannerMessage.value = "تم حفظ بيانات الملف الشخصي."
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { prefs.setDarkTheme(enabled) }
    }

    fun setCloudSyncConsent(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setCloudSyncConsent(enabled)
            if (enabled && Firebase.auth.currentUser != null) {
                backupToCloud()
            }
        }
    }

    fun setAiDataContextConsent(enabled: Boolean) {
        viewModelScope.launch { prefs.setAiDataContextConsent(enabled) }
    }

    fun updateNotificationSettings(
        enabled: Boolean,
        lessons: Boolean,
        tasks: Boolean,
        reviews: Boolean,
        exams: Boolean,
        backlog: Boolean,
        leadMinutes: Int
    ) {
        viewModelScope.launch {
            prefs.updateNotificationSettings(enabled, lessons, tasks, reviews, exams, backlog, leadMinutes)
            _bannerMessage.value = "تم تحديث إعدادات التنبيهات."
        }
    }

    // --- Subjects & Lessons ---
    fun saveSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            dao.upsertSubject(subject)
            launchCloudSync { cloudRepo.syncSubject(subject) }
            _bannerMessage.value = "تم حفظ المادة: ${subject.name}"
        }
    }

    fun deleteSubject(subjectId: String) {
        viewModelScope.launch {
            dao.deleteLessonsBySubject(subjectId)
            dao.deleteSubject(subjectId)
            launchCloudSync { cloudRepo.deleteCloudSubject(subjectId) }
            _bannerMessage.value = "تم حذف المادة ومهامها."
        }
    }

    fun saveLesson(lesson: LessonEntity) {
        viewModelScope.launch {
            dao.upsertLesson(lesson)
            launchCloudSync { cloudRepo.syncLesson(lesson) }
        }
    }

    fun deleteLesson(lessonId: String) {
        viewModelScope.launch {
            dao.deleteLesson(lessonId)
            launchCloudSync { cloudRepo.deleteCloudLesson(lessonId) }
        }
    }

    fun completeLessonWithLog(
        lesson: LessonEntity,
        actualMinutes: Int,
        pagesCount: Int,
        questionsSolved: Int,
        comprehensionPercent: Int,
        notes: String
    ) {
        viewModelScope.launch {
            val today = LocalDate.now().toEpochDay()
            val (nextStage, nextReviewDay) = SpacedRepetitionHelper.computeNextReviewEpochDay(
                lesson.reviewStage,
                today
            )
            val updated = lesson.copy(
                status = LessonStatus.COMPLETED.name,
                actualMinutes = actualMinutes.coerceAtLeast(1),
                pagesCount = pagesCount.coerceAtLeast(0),
                questionsSolved = questionsSolved.coerceAtLeast(0),
                comprehensionPercent = comprehensionPercent.coerceIn(0, 100),
                completionNotes = notes.trim(),
                completedAtEpochMs = System.currentTimeMillis(),
                reviewStage = nextStage,
                nextReviewEpochDay = nextReviewDay
            )
            dao.upsertLesson(updated)
            launchCloudSync { cloudRepo.syncLesson(updated) }

            // Add logged minutes to subject
            subjects.value.firstOrNull { it.id == lesson.subjectId }?.let { subj ->
                val updatedSubj = subj.copy(
                    studyMinutesLogged = subj.studyMinutesLogged + actualMinutes.coerceAtLeast(0)
                )
                dao.upsertSubject(updatedSubj)
                launchCloudSync { cloudRepo.syncSubject(updatedSubj) }
            }

            // Award XP & update streak
            awardXpAndStreak(xpDelta = 40)

            if (nextReviewDay > 0) {
                pushNotification(
                    title = "تمت جدولة مراجعة متباعدة",
                    body = "الدرس \"${lesson.title}\" جاهز للمراجعة القادمة (${SpacedRepetitionHelper.stageLabelArabic(nextStage)}).",
                    category = NotificationCategory.REVIEW,
                    route = "subjects"
                )
            }
            _bannerMessage.value = "أحسنت! تم إتمام الدرس وتسجيل +40 XP وجدولة المراجعة الذكية."
        }
    }

    fun advanceSpacedReview(lesson: LessonEntity, customNextEpochDay: Long? = null) {
        viewModelScope.launch {
            val today = LocalDate.now().toEpochDay()
            val (nextStage, autoNextDay) = SpacedRepetitionHelper.computeNextReviewEpochDay(
                lesson.reviewStage,
                today
            )
            val nextDay = customNextEpochDay ?: autoNextDay
            val updated = lesson.copy(
                status = LessonStatus.COMPLETED.name,
                reviewStage = nextStage,
                nextReviewEpochDay = nextDay
            )
            dao.upsertLesson(updated)
            launchCloudSync { cloudRepo.syncLesson(updated) }
            awardXpAndStreak(25)
            _bannerMessage.value = "تم تسجيل المراجعة الذكية بنجاح (+25 XP)."
        }
    }

    fun splitBigBacklogLesson(lesson: LessonEntity, partsCount: Int = 2) {
        viewModelScope.launch {
            val parts = partsCount.coerceIn(2, 4)
            val minsPerPart = (lesson.estimatedMinutes / parts).coerceAtLeast(15)
            val today = LocalDate.now().toEpochDay()
            val newParts = (1..parts).map { index ->
                LessonEntity(
                    subjectId = lesson.subjectId,
                    unitName = lesson.unitName,
                    chapterName = lesson.chapterName,
                    title = "${lesson.title} — جزء $index/$parts",
                    partLabel = "جزء $index",
                    kind = lesson.kind,
                    status = LessonStatus.NOT_STARTED.name,
                    estimatedMinutes = minsPerPart,
                    difficultyOrImportance = lesson.difficultyOrImportance,
                    delayReason = lesson.delayReason,
                    startDateEpochDay = today + (index - 1),
                    dueDateEpochDay = today + index
                )
            }
            dao.deleteLesson(lesson.id)
            dao.upsertLessons(newParts)
            launchCloudSync {
                cloudRepo.deleteCloudLesson(lesson.id)
                newParts.forEach { cloudRepo.syncLesson(it) }
            }
            _bannerMessage.value = "تم تقسيم المهمة الكبيرة إلى $parts أجزاء صغيرة مريحة."
        }
    }

    fun updateLessonBacklogDetails(
        lesson: LessonEntity,
        delayReason: String,
        importance: Int,
        newDueEpochDay: Long,
        newStatus: LessonStatus
    ) {
        viewModelScope.launch {
            val updated = lesson.copy(
                delayReason = delayReason.trim(),
                difficultyOrImportance = importance.coerceIn(1, 5),
                dueDateEpochDay = newDueEpochDay,
                status = newStatus.name
            )
            dao.upsertLesson(updated)
            launchCloudSync { cloudRepo.syncLesson(updated) }
            _bannerMessage.value = "تم تحديث تفاصيل المهمة."
        }
    }

    // --- Smart Scheduler & Manual Schedule Editing ---
    fun regenerateSmartSchedule() {
        viewModelScope.launch {
            val currentProfile = dao.getProfileOnce() ?: StudentProfileEntity()
            val currentSubjects = dao.getSubjectsOnce()
            val currentLessons = dao.getLessonsOnce()
            val existingBlocks = dao.getScheduleBlocksOnce()
            val currentExams = dao.getExamsOnce()
            val currentHolidays = dao.getHolidaysOnce()

            val result = SmartSchedulerEngine.generateWeeklySchedule(
                profile = currentProfile,
                subjects = currentSubjects,
                lessons = currentLessons,
                existingBlocks = existingBlocks,
                exams = currentExams,
                holidays = currentHolidays
            )

            // Replace auto-generated blocks while preserving fixed blocks
            dao.deleteAutoGeneratedBlocks()
            val nonFixedNonStudy = existingBlocks.filter {
                !it.fixed && (it.category == BlockCategory.SCHOOL.name ||
                    it.category == BlockCategory.MEAL.name ||
                    it.category == BlockCategory.HOLIDAY.name)
            }
            nonFixedNonStudy.forEach { dao.deleteScheduleBlock(it.id) }
            dao.upsertScheduleBlocks(result.generatedBlocks)

            _overflowLessons.value = result.overflowLessons
            _scheduleWarnings.value = result.warnings

            launchCloudSync {
                result.generatedBlocks.forEach { cloudRepo.syncScheduleBlock(it) }
            }

            _bannerMessage.value = if (result.overflowLessons.isEmpty()) {
                "تم بناء جدول أسبوعي متوازن وخالٍ من التعارض!"
            } else {
                "تم بناء الجدول مع حفظ ${result.overflowLessons.size} مهام في قائمة المتبقية حتى لا نضغط يومك."
            }
        }
    }

    fun addOrUpdateScheduleBlock(block: ScheduleBlockEntity) {
        viewModelScope.launch {
            val conflict = SmartSchedulerEngine.hasConflict(
                candidateStart = block.startMinutes,
                candidateEnd = block.endMinutes,
                dayOfWeek = block.dayOfWeek,
                excludeBlockId = block.id,
                existingBlocks = scheduleBlocks.value
            )
            if (conflict != null) {
                _bannerMessage.value = "تنبيه تعارض: الموعد يتقاطع مع \"${conflict.title}\"."
                return@launch
            }
            dao.upsertScheduleBlock(block)
            launchCloudSync { cloudRepo.syncScheduleBlock(block) }
            _bannerMessage.value = "تم حفظ الحصة/المهمة في الجدول."
        }
    }

    fun toggleBlockPinned(block: ScheduleBlockEntity) {
        viewModelScope.launch {
            val updated = block.copy(fixed = !block.fixed)
            dao.upsertScheduleBlock(updated)
            launchCloudSync { cloudRepo.syncScheduleBlock(updated) }
        }
    }

    fun toggleBlockCompleted(block: ScheduleBlockEntity) {
        viewModelScope.launch {
            val updated = block.copy(completed = !block.completed)
            dao.upsertScheduleBlock(updated)
            launchCloudSync { cloudRepo.syncScheduleBlock(updated) }
            if (updated.completed) {
                awardXpAndStreak(20)
            }
        }
    }

    fun moveScheduleBlockOrder(dayBlocks: List<ScheduleBlockEntity>, fromIndex: Int, toIndex: Int) {
        if (fromIndex !in dayBlocks.indices || toIndex !in dayBlocks.indices || fromIndex == toIndex) return
        viewModelScope.launch {
            val a = dayBlocks[fromIndex]
            val b = dayBlocks[toIndex]
            // Swap their time slots cleanly while preserving durations without overlap
            val durA = (a.endMinutes - a.startMinutes).coerceAtLeast(15)
            val durB = (b.endMinutes - b.startMinutes).coerceAtLeast(15)
            val earlier = minOf(a.startMinutes, b.startMinutes)
            val firstBlock = if (fromIndex > toIndex) a else b
            val secondBlock = if (fromIndex > toIndex) b else a
            val firstDur = if (fromIndex > toIndex) durA else durB
            val secondDur = if (fromIndex > toIndex) durB else durA

            val updatedFirst = firstBlock.copy(
                startMinutes = earlier,
                endMinutes = (earlier + firstDur).coerceAtMost(1439),
                orderIndex = minOf(a.orderIndex, b.orderIndex)
            )
            val updatedSecond = secondBlock.copy(
                startMinutes = (earlier + firstDur + 5).coerceAtMost(1439),
                endMinutes = (earlier + firstDur + 5 + secondDur).coerceAtMost(1440),
                orderIndex = maxOf(a.orderIndex, b.orderIndex)
            )
            dao.upsertScheduleBlocks(listOf(updatedFirst, updatedSecond))
            launchCloudSync {
                cloudRepo.syncScheduleBlock(updatedFirst)
                cloudRepo.syncScheduleBlock(updatedSecond)
            }
        }
    }

    fun deleteScheduleBlock(blockId: String) {
        viewModelScope.launch {
            dao.deleteScheduleBlock(blockId)
            launchCloudSync { cloudRepo.deleteCloudScheduleBlock(blockId) }
        }
    }

    // --- Natural Language AI Day Parser ---
    fun analyzeDayDescription(text: String) {
        if (text.isBlank()) {
            _aiDayParseState.value = AiDayParseState.Error("اكتب وصف يومك أولاً ليقوم الذكاء الاصطناعي بتحليله.")
            return
        }
        viewModelScope.launch {
            _aiDayParseState.value = AiDayParseState.Loading
            val res = aiRepo.parseStudentDayDescription(text)
            _aiDayParseState.value = res.fold(
                onSuccess = { AiDayParseState.Success(it) },
                onFailure = { AiDayParseState.Error(it.message ?: "تعذر تحليل النص.") }
            )
        }
    }

    fun resetDayParseState() {
        _aiDayParseState.value = AiDayParseState.Idle
    }

    fun applyParsedDayAndBuildSchedule(
        wakeMinutes: Int?,
        sleepMinutes: Int?,
        schoolStartMinutes: Int?,
        schoolEndMinutes: Int?,
        lessons: List<com.example.ai.ExtractedFixedLesson>
    ) {
        viewModelScope.launch {
            val existingProfile = dao.getProfileOnce() ?: StudentProfileEntity(name = "طالب")
            val updatedProfile = existingProfile.copy(
                wakeTimeMinutes = wakeMinutes ?: existingProfile.wakeTimeMinutes,
                sleepTimeMinutes = sleepMinutes ?: existingProfile.sleepTimeMinutes,
                schoolStartMinutes = schoolStartMinutes ?: existingProfile.schoolStartMinutes,
                schoolEndMinutes = schoolEndMinutes ?: existingProfile.schoolEndMinutes,
                hasSchoolRoutine = (schoolStartMinutes != null && schoolEndMinutes != null && schoolEndMinutes > schoolStartMinutes)
                    || existingProfile.hasSchoolRoutine,
                updatedAtEpochMs = System.currentTimeMillis()
            )
            dao.upsertProfile(updatedProfile)
            launchCloudSync { cloudRepo.syncProfile(updatedProfile) }

            // Ensure subjects exist for mentioned lessons & add fixed schedule blocks
            val currentSubjects = dao.getSubjectsOnce().toMutableList()
            val blocksToSave = mutableListOf<ScheduleBlockEntity>()

            lessons.forEachIndexed { idx, exLesson ->
                var matchedSubj = currentSubjects.firstOrNull {
                    it.name.contains(exLesson.subjectName, ignoreCase = true) ||
                        exLesson.subjectName.contains(it.name, ignoreCase = true)
                }
                if (matchedSubj == null) {
                    matchedSubj = SubjectEntity(
                        name = exLesson.subjectName,
                        colorHex = SubjectColors[(currentSubjects.size + idx) % SubjectColors.size]
                    )
                    currentSubjects.add(matchedSubj)
                    dao.upsertSubject(matchedSubj)
                    launchCloudSync { cloudRepo.syncSubject(matchedSubj) }
                }
                blocksToSave.add(
                    ScheduleBlockEntity(
                        dayOfWeek = exLesson.dayOfWeek,
                        title = "درس ${exLesson.subjectName}",
                        category = BlockCategory.PRIVATE_LESSON.name,
                        subjectId = matchedSubj.id,
                        startMinutes = exLesson.startMinutes,
                        endMinutes = (exLesson.startMinutes + exLesson.durationMinutes).coerceAtMost(1440),
                        fixed = true,
                        orderIndex = idx
                    )
                )
            }
            if (blocksToSave.isNotEmpty()) {
                dao.upsertScheduleBlocks(blocksToSave)
                launchCloudSync {
                    blocksToSave.forEach { cloudRepo.syncScheduleBlock(it) }
                }
            }

            _aiDayParseState.value = AiDayParseState.Idle
            regenerateSmartSchedule()
        }
    }

    // --- Exams ---
    fun saveExam(exam: ExamEntity) {
        viewModelScope.launch {
            dao.upsertExam(exam)
            launchCloudSync { cloudRepo.syncExam(exam) }
            if (exam.reminderEnabled) {
                val today = LocalDate.now().toEpochDay()
                val daysLeft = exam.examDateEpochDay - today
                if (daysLeft in 0..7) {
                    pushNotification(
                        title = "تذكير امتحان قريب: ${exam.title}",
                        body = "متبقي $daysLeft يوم على الامتحان — راجع الأجزاء المطلوبة في مركز الامتحانات.",
                        category = NotificationCategory.EXAM,
                        route = "exams"
                    )
                }
            }
            _bannerMessage.value = "تم حفظ الامتحان."
        }
    }

    fun deleteExam(examId: String) {
        viewModelScope.launch {
            dao.deleteExam(examId)
            launchCloudSync { cloudRepo.deleteCloudExam(examId) }
        }
    }

    // --- Focus Timer (Pomodoro + Custom) ---
    fun configureFocusTimer(focusMin: Int, breakMin: Int, mode: FocusModeType) {
        val current = _focusTimer.value
        if (current.isRunning) return
        _focusTimer.value = current.copy(
            focusMinutes = focusMin.coerceIn(5, 180),
            breakMinutes = breakMin.coerceIn(1, 60),
            remainingSeconds = if (current.isBreakPhase) breakMin * 60 else focusMin * 60,
            mode = mode
        )
        viewModelScope.launch {
            prefs.updatePomodoroDefaults(focusMin, breakMin)
        }
    }

    fun linkFocusTarget(subjectId: String?, lessonId: String?) {
        _focusTimer.value = _focusTimer.value.copy(
            linkedSubjectId = subjectId,
            linkedLessonId = lessonId
        )
    }

    fun toggleFocusFullscreen(fullscreen: Boolean) {
        _focusTimer.value = _focusTimer.value.copy(fullscreenMode = fullscreen)
    }

    fun startOrPauseTimer() {
        val current = _focusTimer.value
        if (current.isRunning) {
            timerJob?.cancel()
            _focusTimer.value = current.copy(isRunning = false)
            return
        }
        _focusTimer.value = current.copy(isRunning = true)
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_focusTimer.value.isRunning && _focusTimer.value.remainingSeconds > 0) {
                delay(1000L)
                val now = _focusTimer.value
                if (!now.isRunning) break
                val nextSec = (now.remainingSeconds - 1).coerceAtLeast(0)
                _focusTimer.value = now.copy(remainingSeconds = nextSec)
                if (nextSec == 0) {
                    onTimerPhaseFinished()
                }
            }
        }
    }

    fun resetFocusTimer() {
        timerJob?.cancel()
        val current = _focusTimer.value
        val targetMin = if (current.isBreakPhase) current.breakMinutes else current.focusMinutes
        _focusTimer.value = current.copy(
            isRunning = false,
            remainingSeconds = targetMin * 60
        )
    }

    fun switchTimerPhase(toBreak: Boolean) {
        timerJob?.cancel()
        val current = _focusTimer.value
        val targetMin = if (toBreak) current.breakMinutes else current.focusMinutes
        _focusTimer.value = current.copy(
            isRunning = false,
            isBreakPhase = toBreak,
            remainingSeconds = targetMin * 60
        )
    }

    private suspend fun onTimerPhaseFinished() {
        val state = _focusTimer.value
        _focusTimer.value = state.copy(isRunning = false)

        if (!state.isBreakPhase) {
            val session = FocusSessionEntity(
                subjectId = state.linkedSubjectId,
                lessonId = state.linkedLessonId,
                durationMinutes = state.focusMinutes,
                breakMinutes = state.breakMinutes,
                mode = state.mode.name
            )
            dao.insertFocusSession(session)
            launchCloudSync { cloudRepo.syncFocusSession(session) }

            state.linkedSubjectId?.let { subjId ->
                subjects.value.firstOrNull { it.id == subjId }?.let { subj ->
                    val updatedSubj = subj.copy(
                        studyMinutesLogged = subj.studyMinutesLogged + state.focusMinutes
                    )
                    dao.upsertSubject(updatedSubj)
                    launchCloudSync { cloudRepo.syncSubject(updatedSubj) }
                }
            }
            awardXpAndStreak(30)
            ReminderScheduler.showImmediateNotification(
                getApplication(),
                "انتهت جلسة التركيز بنجاح!",
                "أتممت ${state.focusMinutes} دقيقة مذاكرة (+30 XP). خذ استراحة ${state.breakMinutes} دقائق."
            )
            // Automatically prepare break phase
            _focusTimer.value = state.copy(
                isRunning = false,
                isBreakPhase = true,
                remainingSeconds = state.breakMinutes * 60
            )
        } else {
            ReminderScheduler.showImmediateNotification(
                getApplication(),
                "انتهت فترة الراحة",
                "هل أنت مستعد لجلسة تركيز جديدة؟"
            )
            _focusTimer.value = state.copy(
                isRunning = false,
                isBreakPhase = false,
                remainingSeconds = state.focusMinutes * 60
            )
        }
    }

    // --- Notes & Library ---
    fun saveNote(note: StudyNoteEntity) {
        viewModelScope.launch {
            val updated = note.copy(updatedAtEpochMs = System.currentTimeMillis())
            dao.upsertNote(updated)
            launchCloudSync { cloudRepo.syncNote(updated) }
            _bannerMessage.value = "تم حفظ الملاحظة في مكتبتك."
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            dao.deleteNote(noteId)
            launchCloudSync { cloudRepo.deleteCloudNote(noteId) }
        }
    }

    // --- Holidays & Rest Days ---
    fun saveHoliday(holiday: HolidayOrBreakEntity) {
        viewModelScope.launch {
            dao.upsertHoliday(holiday)
            launchCloudSync { cloudRepo.syncHoliday(holiday) }
            regenerateSmartSchedule()
        }
    }

    fun deleteHoliday(holidayId: String) {
        viewModelScope.launch {
            dao.deleteHoliday(holidayId)
            launchCloudSync { cloudRepo.deleteCloudHoliday(holidayId) }
            regenerateSmartSchedule()
        }
    }

    // --- MUNAZZEM AI Chat ---
    fun sendAssistantMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            val userMsg = AiChatMessageEntity(role = "user", text = trimmed)
            dao.insertChatMessage(userMsg)
            _aiChatState.value = AiChatState.Loading

            val contextSummary = if (settings.value.aiDataContextConsent) {
                buildStudentContextSummary()
            } else {
                null
            }

            try {
                var finalReply = ""
                aiRepo.streamAssistantReply(
                    userMessage = trimmed,
                    recentHistory = chatMessages.value,
                    studentContextSummary = contextSummary
                ).collect { partial ->
                    finalReply = partial
                    _aiChatState.value = AiChatState.Streaming(partial)
                }
                if (finalReply.isNotBlank()) {
                    dao.insertChatMessage(
                        AiChatMessageEntity(role = "model", text = finalReply)
                    )
                }
                _aiChatState.value = AiChatState.Idle
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiChatState.value = AiChatState.Error(e.message ?: "تعذر الحصول على رد من المساعد.")
            }
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            dao.clearChatMessages()
            _aiChatState.value = AiChatState.Idle
        }
    }

    fun generateAiBacklogPlan() {
        viewModelScope.launch {
            val today = LocalDate.now().toEpochDay()
            val backlog = lessons.value.filter { AnalyticsEngine.isBacklogLesson(it, today) }
            if (backlog.isEmpty()) {
                _aiBacklogState.value = AiBacklogState.Success("لا توجد مهام متراكمة حالياً! أنت على المسار الصحيح.")
                return@launch
            }
            _aiBacklogState.value = AiBacklogState.Loading
            val subjMap = subjects.value.associateBy { it.id }
            val summary = backlog.joinToString("\n") { l ->
                val subj = subjMap[l.subjectId]?.name ?: "مادة"
                "- $subj: ${l.title} (المدة المتوقعة ${l.estimatedMinutes} د، الأهمية ${l.difficultyOrImportance}/5، الحالة: ${LessonStatus.fromRaw(l.status).arabicLabel})"
            }
            val result = aiRepo.generateBacklogPlan(summary, dailyHealthyStudyMinutes = 90)
            _aiBacklogState.value = result.fold(
                onSuccess = { AiBacklogState.Success(it) },
                onFailure = { AiBacklogState.Error(it.message ?: "تعذر اقتراح خطة.") }
            )
        }
    }

    private fun buildStudentContextSummary(): String {
        val prof = profile.value
        val today = LocalDate.now().toEpochDay()
        val subjMap = subjects.value.associateBy { it.id }
        val stats = analytics.value
        val backlogList = lessons.value.filter { AnalyticsEngine.isBacklogLesson(it, today) }
        val nearExams = exams.value.filter { it.examDateEpochDay >= today }.take(5)

        return buildString {
            if (prof != null) {
                appendLine("الطالب: ${prof.name} | المرحلة: ${prof.educationalStage} - ${prof.grade} | الهدف: ${prof.studyGoal}")
                appendLine("الاستيقاظ: ${prof.wakeTimeMinutes / 60}:${prof.wakeTimeMinutes % 60} | النوم: ${prof.sleepTimeMinutes / 60}:${prof.sleepTimeMinutes % 60}")
            }
            appendLine("نسبة الإنجاز: ${stats.completionRatePercent}% | ساعات الأسبوع: ${String.format("%.1f", stats.weeklyStudyHours)}")
            appendLine("المواد (${subjects.value.size}): " + subjects.value.joinToString("، ") { "${it.name} (صعوبة ${it.difficulty}/5)" })
            if (backlogList.isNotEmpty()) {
                appendLine("المتراكم (${backlogList.size}): " + backlogList.take(8).joinToString("، ") {
                    "${subjMap[it.subjectId]?.name ?: ""}: ${it.title} (${it.estimatedMinutes}د)"
                })
            } else {
                appendLine("المتراكم: لا يوجد")
            }
            if (nearExams.isNotEmpty()) {
                appendLine("الامتحانات القادمة: " + nearExams.joinToString("، ") {
                    "${it.title} (${subjMap[it.subjectId]?.name ?: ""}) بعد ${it.examDateEpochDay - today} يوم - استعداد ${it.readinessPercent}%"
                })
            }
        }
    }

    // --- Notifications ---
    fun pushNotification(
        title: String,
        body: String,
        category: NotificationCategory,
        route: String
    ) {
        viewModelScope.launch {
            dao.upsertNotification(
                AppNotificationEntity(
                    title = title,
                    body = body,
                    category = category.name,
                    relatedRoute = route
                )
            )
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch { dao.markNotificationRead(id) }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch { dao.markAllNotificationsRead() }
    }

    // --- Search ---
    fun searchAll(query: String): SearchResults {
        val q = query.trim()
        if (q.isBlank()) return SearchResults()
        return SearchResults(
            subjects = subjects.value.filter {
                it.name.contains(q, ignoreCase = true) || it.teacherName.contains(q, ignoreCase = true)
            },
            lessons = lessons.value.filter {
                it.title.contains(q, ignoreCase = true) || it.unitName.contains(q, ignoreCase = true) ||
                    it.chapterName.contains(q, ignoreCase = true)
            },
            notes = notes.value.filter {
                it.title.contains(q, ignoreCase = true) || it.content.contains(q, ignoreCase = true)
            },
            exams = exams.value.filter {
                it.title.contains(q, ignoreCase = true) || it.requiredParts.contains(q, ignoreCase = true)
            },
            attachments = notes.value.filter {
                (it.attachmentUri.isNotBlank() || it.linkUrl.isNotBlank()) &&
                    (it.title.contains(q, ignoreCase = true) || it.linkUrl.contains(q, ignoreCase = true))
            }
        )
    }

    // --- Cloud Sync & Data Management ---
    fun backupToCloud() {
        viewModelScope.launch {
            if (Firebase.auth.currentUser == null) {
                _syncState.value = SyncState.Message("سجّل الدخول بحساب Google أولاً لتفعيل النسخ الاحتياطي السحابي.", isError = true)
                return@launch
            }
            _syncState.value = SyncState.Syncing
            val res = cloudRepo.backupAllToCloud()
            _syncState.value = res.fold(
                onSuccess = { count -> SyncState.Message("تم نسخ $count سجل احتياطياً إلى السحابة بأمان.") },
                onFailure = { err -> SyncState.Message(err.message ?: "تعذر النسخ الاحتياطي السحابي.", isError = true) }
            )
        }
    }

    fun restoreFromCloud() {
        viewModelScope.launch {
            if (Firebase.auth.currentUser == null) {
                _syncState.value = SyncState.Message("سجّل الدخول بحساب Google أولاً لاستعادة بياناتك.", isError = true)
                return@launch
            }
            _syncState.value = SyncState.Syncing
            val res = cloudRepo.restoreFromCloud()
            _syncState.value = res.fold(
                onSuccess = { count -> SyncState.Message("تمت استعادة $count سجل من السحابة بنجاح.") },
                onFailure = { err -> SyncState.Message(err.message ?: "تعذر استعادة البيانات.", isError = true) }
            )
        }
    }

    fun deleteAllData(includeCloud: Boolean, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            if (includeCloud && Firebase.auth.currentUser != null) {
                cloudRepo.deleteAllCloudData()
            }
            dao.clearProfile()
            dao.clearSubjects()
            dao.clearLessons()
            dao.clearScheduleBlocks()
            dao.clearExams()
            dao.clearFocusSessions()
            dao.clearNotes()
            dao.clearHolidays()
            dao.clearNotifications()
            dao.clearChatMessages()
            prefs.clearAll()
            onDone()
        }
    }

    // --- Daily Tasks (Firestore) ---
    fun refreshDailyTasks() {
        _dailyTasksRefreshTick.value += 1
    }

    fun addDailyTask(
        title: String,
        description: String,
        subjectId: String?,
        subjectName: String,
        scheduledStartMinutes: Long?,
        estimatedMinutes: Long,
        priority: Long
    ) {
        viewModelScope.launch {
            val todayEpochDay = LocalDate.now().toEpochDay()
            val task = DailyTask(
                title = title.trim(),
                description = description.trim(),
                subjectId = subjectId,
                subjectName = subjectName.trim(),
                dateEpochDay = todayEpochDay,
                scheduledStartMinutes = scheduledStartMinutes,
                estimatedMinutes = estimatedMinutes.coerceIn(1L, 600L),
                priority = priority.coerceIn(1L, 4L),
                completed = false
            )
            val result = dailyTasksRepo.addTask(task)
            result.fold(
                onSuccess = { _bannerMessage.value = "تمت إضافة المهمة لليوم في السحابة." },
                onFailure = { err -> _bannerMessage.value = err.localizedMessage ?: "تعذر حفظ المهمة." }
            )
        }
    }

    fun updateDailyTask(task: DailyTask) {
        viewModelScope.launch {
            val result = dailyTasksRepo.updateTask(task)
            result.fold(
                onSuccess = { _bannerMessage.value = "تم تحديث المهمة." },
                onFailure = { err -> _bannerMessage.value = err.localizedMessage ?: "تعذر تحديث المهمة." }
            )
        }
    }

    fun toggleDailyTaskCompletion(task: DailyTask) {
        viewModelScope.launch {
            val wasCompleted = task.completed
            val result = dailyTasksRepo.toggleTaskCompleted(task)
            result.fold(
                onSuccess = {
                    if (!wasCompleted) {
                        awardXpAndStreak(15)
                        _bannerMessage.value = "أحسنت! تم إنجاز المهمة (+15 XP)."
                    }
                },
                onFailure = { err -> _bannerMessage.value = err.localizedMessage ?: "تعذر تحديث حالة المهمة." }
            )
        }
    }

    fun deleteDailyTask(taskId: String) {
        viewModelScope.launch {
            val result = dailyTasksRepo.deleteTask(taskId)
            result.fold(
                onSuccess = { _bannerMessage.value = "تم حذف المهمة." },
                onFailure = { err -> _bannerMessage.value = err.localizedMessage ?: "تعذر حذف المهمة." }
            )
        }
    }

    private suspend fun awardXpAndStreak(xpDelta: Int) {
        val prof = dao.getProfileOnce() ?: return
        val today = LocalDate.now().toEpochDay()
        val newStreak = when {
            prof.lastActiveEpochDay == today -> prof.currentStreakDays.coerceAtLeast(1)
            prof.lastActiveEpochDay == today - 1 -> prof.currentStreakDays + 1
            else -> 1
        }
        val newXp = prof.xp + xpDelta
        val newLevel = (newXp / 250) + 1
        val updated = prof.copy(
            xp = newXp,
            level = newLevel,
            currentStreakDays = newStreak,
            longestStreakDays = maxOf(prof.longestStreakDays, newStreak),
            lastActiveEpochDay = today,
            updatedAtEpochMs = System.currentTimeMillis()
        )
        dao.upsertProfile(updated)
        launchCloudSync { cloudRepo.syncProfile(updated) }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
