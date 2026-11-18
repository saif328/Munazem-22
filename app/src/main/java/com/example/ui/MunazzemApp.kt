package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.CircleIconBadge
import com.example.ui.components.StatusPill
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.time.LocalDate

object AppRoutes {
    const val DASHBOARD = "dashboard"
    const val SCHEDULE = "schedule"
    const val SUBJECTS = "subjects"
    const val SUBJECT_DETAIL = "subject_detail/{subjectId}"
    const val BACKLOG = "backlog"
    const val EXAMS = "exams"
    const val FOCUS = "focus"
    const val ASSISTANT = "assistant"
    const val LIBRARY = "library"
    const val PROFILE = "profile"
    const val DAILY_TASKS = "daily_tasks"
    const val MORE = "more"

    fun subjectDetail(subjectId: String) = "subject_detail/$subjectId"
}

private data class NavTabItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val badgeCount: Int = 0
)

@Composable
fun MunazzemApp(
    viewModel: MainViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val lessons by viewModel.lessons.collectAsStateWithLifecycle()
    val scheduleBlocks by viewModel.scheduleBlocks.collectAsStateWithLifecycle()
    val exams by viewModel.exams.collectAsStateWithLifecycle()
    val focusSessions by viewModel.focusSessions.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val holidays by viewModel.holidays.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val overflowLessons by viewModel.overflowLessons.collectAsStateWithLifecycle()
    val scheduleWarnings by viewModel.scheduleWarnings.collectAsStateWithLifecycle()
    val analytics by viewModel.analytics.collectAsStateWithLifecycle()
    val gamification by viewModel.gamification.collectAsStateWithLifecycle()
    val aiDayParseState by viewModel.aiDayParseState.collectAsStateWithLifecycle()
    val aiChatState by viewModel.aiChatState.collectAsStateWithLifecycle()
    val aiBacklogState by viewModel.aiBacklogState.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val dailyTasksState by viewModel.dailyTasksState.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()
    val focusTimer by viewModel.focusTimer.collectAsStateWithLifecycle()

    var showAiDayParser by remember { mutableStateOf(false) }
    var showGlobalSearch by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(bannerMessage) {
        val msg = bannerMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    MunazzemTheme(darkTheme = settings.darkTheme) {
        if (!settings.onboardingCompleted) {
            OnboardingScreen(
                onComplete = { name, stage, grade, school, gov, goal, avatarUri, subjList, enableCloud ->
                    viewModel.completeOnboarding(
                        name = name,
                        stage = stage,
                        grade = grade,
                        school = school,
                        governorate = gov,
                        studyGoal = goal,
                        avatarUri = avatarUri,
                        initialSubjectNames = subjList,
                        enableCloudSync = enableCloud
                    )
                },
                onRestoreCloudAfterLogin = {
                    viewModel.restoreFromCloud()
                }
            )
            return@MunazzemTheme
        }

        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        val unreadNotifs = notifications.count { !it.read }
        val todayDayOfWeek = (LocalDate.now().dayOfWeek.value % 7) + 1
        val todayBlocks = remember(scheduleBlocks, todayDayOfWeek) {
            scheduleBlocks
                .filter { it.dayOfWeek == todayDayOfWeek || it.dayOfWeek == 0 }
                .sortedBy { it.startMinutes }
        }

        val primaryTabs = listOf(
            NavTabItem(AppRoutes.DASHBOARD, "الرئيسية", Icons.Filled.Dashboard),
            NavTabItem(AppRoutes.SCHEDULE, "الجدول", Icons.Filled.CalendarViewWeek),
            NavTabItem(AppRoutes.SUBJECTS, "المواد", Icons.Filled.MenuBook),
            NavTabItem(AppRoutes.ASSISTANT, "المساعد", Icons.Filled.SmartToy),
            NavTabItem(AppRoutes.MORE, "المزيد", Icons.Filled.GridView, badgeCount = analytics.backlogCount)
        )

        val tabletRailItems = listOf(
            NavTabItem(AppRoutes.DASHBOARD, "الرئيسية", Icons.Filled.Dashboard),
            NavTabItem(AppRoutes.DAILY_TASKS, "مهام اليوم", Icons.Filled.Checklist),
            NavTabItem(AppRoutes.SCHEDULE, "الجدول", Icons.Filled.CalendarViewWeek),
            NavTabItem(AppRoutes.SUBJECTS, "المواد", Icons.Filled.MenuBook),
            NavTabItem(AppRoutes.BACKLOG, "المتراكم", Icons.Filled.Layers, badgeCount = analytics.backlogCount),
            NavTabItem(AppRoutes.EXAMS, "الامتحانات", Icons.Filled.EventNote),
            NavTabItem(AppRoutes.FOCUS, "التركيز", Icons.Filled.Timer),
            NavTabItem(AppRoutes.ASSISTANT, "المساعد", Icons.Filled.SmartToy),
            NavTabItem(AppRoutes.LIBRARY, "المكتبة", Icons.Filled.FolderCopy),
            NavTabItem(AppRoutes.PROFILE, "حسابي", Icons.Filled.Person)
        )

        fun navigateTop(route: String) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }

        val hideChrome = focusTimer.fullscreenMode && currentRoute == AppRoutes.FOCUS

        GlassBackground {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWideScreen = maxWidth >= 700.dp

                Scaffold(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (!isWideScreen && !hideChrome) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                windowInsets = WindowInsets.navigationBars
                            ) {
                                primaryTabs.forEach { tab ->
                                    val selected = currentRoute == tab.route ||
                                        (tab.route == AppRoutes.MORE && currentRoute in listOf(
                                            AppRoutes.DAILY_TASKS, AppRoutes.BACKLOG, AppRoutes.EXAMS, AppRoutes.FOCUS,
                                            AppRoutes.LIBRARY, AppRoutes.PROFILE
                                        ))
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateTop(tab.route) },
                                        icon = {
                                            BadgedBox(
                                                badge = {
                                                    if (tab.badgeCount > 0) {
                                                        Badge { Text("${tab.badgeCount}") }
                                                    }
                                                }
                                            ) {
                                                Icon(tab.icon, contentDescription = tab.label)
                                            }
                                        },
                                        label = {
                                            Text(
                                                tab.label,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (isWideScreen && !hideChrome) {
                            NavigationRail(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                            ) {
                                tabletRailItems.forEach { item ->
                                    NavigationRailItem(
                                        selected = currentRoute == item.route,
                                        onClick = { navigateTop(item.route) },
                                        icon = {
                                            BadgedBox(
                                                badge = {
                                                    if (item.badgeCount > 0) {
                                                        Badge { Text("${item.badgeCount}") }
                                                    }
                                                }
                                            ) {
                                                Icon(item.icon, contentDescription = item.label)
                                            }
                                        },
                                        label = { Text(item.label, maxLines = 1) }
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            NavHost(
                                navController = navController,
                                startDestination = AppRoutes.DASHBOARD,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                composable(AppRoutes.DASHBOARD) {
                                    DashboardScreen(
                                        profile = profile,
                                        analytics = analytics,
                                        gamification = gamification,
                                        todayBlocks = todayBlocks,
                                        exams = exams,
                                        subjects = subjects,
                                        unreadNotificationsCount = unreadNotifs,
                                        isDarkTheme = settings.darkTheme,
                                        onToggleTheme = { viewModel.setDarkTheme(!settings.darkTheme) },
                                        onOpenSearch = { showGlobalSearch = true },
                                        onOpenNotifications = { showNotificationsSheet = true },
                                        onOpenAiDayParser = {
                                            viewModel.resetDayParseState()
                                            showAiDayParser = true
                                        },
                                        onOpenFocusTimer = { navigateTop(AppRoutes.FOCUS) },
                                        onOpenBacklog = { navigateTop(AppRoutes.BACKLOG) },
                                        onOpenSchedule = { navigateTop(AppRoutes.SCHEDULE) },
                                        onOpenExams = { navigateTop(AppRoutes.EXAMS) },
                                        onOpenDailyTasks = { navController.navigate(AppRoutes.DAILY_TASKS) },
                                        onOpenAssistant = { navigateTop(AppRoutes.ASSISTANT) },
                                        onOpenProfile = { navigateTop(AppRoutes.PROFILE) },
                                        onToggleBlockDone = { viewModel.toggleBlockCompleted(it) }
                                    )
                                }

                                composable(AppRoutes.DAILY_TASKS) {
                                    DailyTasksScreen(
                                        uiState = dailyTasksState,
                                        subjects = subjects,
                                        onBack = { navController.popBackStack() },
                                        onRetry = { viewModel.refreshDailyTasks() },
                                        onAddTask = { title, desc, sId, sName, startMins, estMins, prio ->
                                            viewModel.addDailyTask(title, desc, sId, sName, startMins, estMins, prio)
                                        },
                                        onUpdateTask = { viewModel.updateDailyTask(it) },
                                        onToggleTask = { viewModel.toggleDailyTaskCompletion(it) },
                                        onDeleteTask = { viewModel.deleteDailyTask(it) }
                                    )
                                }

                                composable(AppRoutes.SCHEDULE) {
                                    ScheduleScreen(
                                        scheduleBlocks = scheduleBlocks,
                                        overflowLessons = overflowLessons,
                                        warnings = scheduleWarnings,
                                        subjects = subjects,
                                        holidays = holidays,
                                        onRegenerateSchedule = { viewModel.regenerateSmartSchedule() },
                                        onAddOrUpdateBlock = { viewModel.addOrUpdateScheduleBlock(it) },
                                        onTogglePin = { viewModel.toggleBlockPinned(it) },
                                        onToggleComplete = { viewModel.toggleBlockCompleted(it) },
                                        onMoveOrder = { list, from, to -> viewModel.moveScheduleBlockOrder(list, from, to) },
                                        onDeleteBlock = { viewModel.deleteScheduleBlock(it) },
                                        onSaveHoliday = { viewModel.saveHoliday(it) },
                                        onDeleteHoliday = { viewModel.deleteHoliday(it) }
                                    )
                                }

                                composable(AppRoutes.SUBJECTS) {
                                    SubjectsScreen(
                                        subjectAnalytics = analytics.subjectAnalytics,
                                        onOpenSubject = { id ->
                                            navController.navigate(AppRoutes.subjectDetail(id))
                                        },
                                        onSaveSubject = { viewModel.saveSubject(it) },
                                        onDeleteSubject = { viewModel.deleteSubject(it) }
                                    )
                                }

                                composable(
                                    route = AppRoutes.SUBJECT_DETAIL,
                                    arguments = listOf(navArgument("subjectId") { type = NavType.StringType })
                                ) { backStack ->
                                    val subjId = backStack.arguments?.getString("subjectId") ?: ""
                                    val subj = subjects.firstOrNull { it.id == subjId }
                                    if (subj != null) {
                                        SubjectDetailScreen(
                                            subject = subj,
                                            lessons = lessons,
                                            notes = notes,
                                            onBack = { navController.popBackStack() },
                                            onSaveLesson = { viewModel.saveLesson(it) },
                                            onDeleteLesson = { viewModel.deleteLesson(it) },
                                            onCompleteLessonWithLog = { les, mins, pages, qs, comp, noteTxt ->
                                                viewModel.completeLessonWithLog(les, mins, pages, qs, comp, noteTxt)
                                            },
                                            onAdvanceReview = { les, customDay ->
                                                viewModel.advanceSpacedReview(les, customDay)
                                            },
                                            onOpenFocusForLesson = { sId, lId ->
                                                viewModel.linkFocusTarget(sId, lId)
                                                navigateTop(AppRoutes.FOCUS)
                                            }
                                        )
                                    } else {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("المادة غير موجودة")
                                        }
                                    }
                                }

                                composable(AppRoutes.BACKLOG) {
                                    BacklogScreen(
                                        subjects = subjects,
                                        lessons = lessons,
                                        subjectAnalytics = analytics.subjectAnalytics,
                                        aiBacklogState = aiBacklogState,
                                        onGenerateAiPlan = { viewModel.generateAiBacklogPlan() },
                                        onRedistributeAcrossDays = { viewModel.regenerateSmartSchedule() },
                                        onSplitLesson = { les, parts -> viewModel.splitBigBacklogLesson(les, parts) },
                                        onUpdateBacklogDetails = { les, reason, imp, due, status ->
                                            viewModel.updateLessonBacklogDetails(les, reason, imp, due, status)
                                        },
                                        onCompleteQuick = { les, mins, p, q, c, n ->
                                            viewModel.completeLessonWithLog(les, mins, p, q, c, n)
                                        }
                                    )
                                }

                                composable(AppRoutes.EXAMS) {
                                    ExamsScreen(
                                        exams = exams,
                                        subjects = subjects,
                                        onSaveExam = { viewModel.saveExam(it) },
                                        onDeleteExam = { viewModel.deleteExam(it) }
                                    )
                                }

                                composable(AppRoutes.FOCUS) {
                                    FocusTimerScreen(
                                        timerState = focusTimer,
                                        subjects = subjects,
                                        lessons = lessons,
                                        focusSessions = focusSessions,
                                        analytics = analytics,
                                        onConfigureTimer = { f, b, m -> viewModel.configureFocusTimer(f, b, m) },
                                        onLinkTarget = { sId, lId -> viewModel.linkFocusTarget(sId, lId) },
                                        onStartOrPause = { viewModel.startOrPauseTimer() },
                                        onReset = { viewModel.resetFocusTimer() },
                                        onSwitchPhase = { viewModel.switchTimerPhase(it) },
                                        onToggleFullscreen = { viewModel.toggleFocusFullscreen(it) }
                                    )
                                }

                                composable(AppRoutes.ASSISTANT) {
                                    AssistantScreen(
                                        messages = chatMessages,
                                        chatState = aiChatState,
                                        aiDataContextConsent = settings.aiDataContextConsent,
                                        onToggleDataConsent = { viewModel.setAiDataContextConsent(it) },
                                        onSendMessage = { viewModel.sendAssistantMessage(it) },
                                        onClearChat = { viewModel.clearChatHistory() }
                                    )
                                }

                                composable(AppRoutes.LIBRARY) {
                                    LibraryAndCalendarScreen(
                                        subjects = subjects,
                                        notes = notes,
                                        scheduleBlocks = scheduleBlocks,
                                        exams = exams,
                                        lessons = lessons,
                                        holidays = holidays,
                                        onSaveNote = { viewModel.saveNote(it) },
                                        onDeleteNote = { viewModel.deleteNote(it) },
                                        onSaveHoliday = { viewModel.saveHoliday(it) },
                                        onDeleteHoliday = { viewModel.deleteHoliday(it) }
                                    )
                                }

                                composable(AppRoutes.PROFILE) {
                                    ProfileAndSettingsScreen(
                                        profile = profile,
                                        analytics = analytics,
                                        gamification = gamification,
                                        settings = settings,
                                        syncState = syncState,
                                        onUpdateProfile = { viewModel.updateProfile(it) },
                                        onToggleTheme = { viewModel.setDarkTheme(it) },
                                        onToggleCloudSyncConsent = { viewModel.setCloudSyncConsent(it) },
                                        onUpdateNotificationPrefs = { en, l, t, r, ex, b, lead ->
                                            viewModel.updateNotificationSettings(en, l, t, r, ex, b, lead)
                                        },
                                        onBackupToCloud = { viewModel.backupToCloud() },
                                        onRestoreFromCloud = { viewModel.restoreFromCloud() },
                                        onDeleteAllData = { includeCloud ->
                                            viewModel.deleteAllData(includeCloud)
                                        }
                                    )
                                }

                                composable(AppRoutes.MORE) {
                                    MoreToolsHubScreen(
                                        backlogCount = analytics.backlogCount,
                                        upcomingExamsCount = exams.count { it.examDateEpochDay >= LocalDate.now().toEpochDay() },
                                        notesCount = notes.size,
                                        level = gamification.level,
                                        xp = gamification.xp,
                                        onNavigate = { navigateTop(it) },
                                        onOpenAiDayParser = {
                                            viewModel.resetDayParseState()
                                            showAiDayParser = true
                                        },
                                        onOpenSearch = { showGlobalSearch = true },
                                        onOpenNotifications = { showNotificationsSheet = true }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAiDayParser) {
            AiDayParserSheet(
                parseState = aiDayParseState,
                onAnalyze = { viewModel.analyzeDayDescription(it) },
                onConfirmAndBuild = { wake, sleep, sStart, sEnd, parsedLessons ->
                    viewModel.applyParsedDayAndBuildSchedule(wake, sleep, sStart, sEnd, parsedLessons)
                },
                onDismiss = { showAiDayParser = false }
            )
        }

        if (showGlobalSearch) {
            GlobalSearchSheet(
                onSearch = { viewModel.searchAll(it) },
                onNavigateToRoute = { navigateTop(it) },
                onOpenSubject = { subjId -> navController.navigate(AppRoutes.subjectDetail(subjId)) },
                onDismiss = { showGlobalSearch = false }
            )
        }

        if (showNotificationsSheet) {
            NotificationCenterSheet(
                notifications = notifications,
                onMarkRead = { viewModel.markNotificationRead(it) },
                onMarkAllRead = { viewModel.markAllNotificationsRead() },
                onNavigate = { navigateTop(it) },
                onDismiss = { showNotificationsSheet = false }
            )
        }
    }
}

@Composable
private fun MoreToolsHubScreen(
    backlogCount: Int,
    upcomingExamsCount: Int,
    notesCount: Int,
    level: Int,
    xp: Int,
    onNavigate: (String) -> Unit,
    onOpenAiDayParser: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenNotifications: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("مركز أدوات مُنظِّم", style = MaterialTheme.typography.headlineMedium)
            Text("الوصول السريع لجميع أقسام MUNAZZEM STUDY PRO", style = MaterialTheme.typography.bodySmall)
        }
        item {
            HubCard(
                title = "مهام اليوم (Firestore)",
                subtitle = "إدارة مهام اليوم الحالي ومزامنتها لحظياً مع حسابك السحابي",
                badgeText = "مزامنة فورية",
                tint = EmeraldSuccess,
                icon = Icons.Filled.Checklist,
                testTag = "hub_daily_tasks_card",
                onClick = { onNavigate(AppRoutes.DAILY_TASKS) }
            )
        }
        item {
            HubCard(
                title = "المتراكم الذكي",
                subtitle = "إدارة الدروس المتأخرة، تقسيم المهام الكبيرة، وخطة التصفية",
                badgeText = if (backlogCount > 0) "$backlogCount متراكم" else "مصفى",
                tint = if (backlogCount > 0) CrimsonUrgent else EmeraldSuccess,
                icon = Icons.Filled.Layers,
                testTag = "hub_backlog_card",
                onClick = { onNavigate(AppRoutes.BACKLOG) }
            )
        }
        item {
            HubCard(
                title = "مؤقت التركيز وبومودورو",
                subtitle = "جلسات تركيز، وضع ملء الشاشة، وتسجيل ساعات المواد",
                badgeText = "Focus Mode",
                tint = SkyPrimary,
                icon = Icons.Filled.Timer,
                testTag = "hub_focus_card",
                onClick = { onNavigate(AppRoutes.FOCUS) }
            )
        }
        item {
            HubCard(
                title = "مركز الامتحانات والنتائج",
                subtitle = "عد تنازلي، خطط المراجعة، وتحليل تطور الدرجات",
                badgeText = "$upcomingExamsCount قادم",
                tint = AmberWarning,
                icon = Icons.Filled.EventNote,
                testTag = "hub_exams_card",
                onClick = { onNavigate(AppRoutes.EXAMS) }
            )
        }
        item {
            HubCard(
                title = "المكتبة والتقويم الدراسي",
                subtitle = "ملاحظات، ملفات PDF، صور المذكرات، والتقويم الشهري والإجازات",
                badgeText = "$notesCount ملاحظة",
                tint = IndigoAccent,
                icon = Icons.Filled.FolderCopy,
                testTag = "hub_library_card",
                onClick = { onNavigate(AppRoutes.LIBRARY) }
            )
        }
        item {
            HubCard(
                title = "الملف الشخصي والإنجازات والمزامنة",
                subtitle = "الشارات، نقاط XP، التنبيهات، النسخ الاحتياطي السحابي، والخصوصية",
                badgeText = "Lv.$level • $xp XP",
                tint = EmeraldSuccess,
                icon = Icons.Filled.Person,
                testTag = "hub_profile_card",
                onClick = { onNavigate(AppRoutes.PROFILE) }
            )
        }
        item {
            HubCard(
                title = "تحليل اليوم بالذكاء الاصطناعي",
                subtitle = "اكتب روتينك بالعامية المصرية أو الفصحى لبناء جدولك",
                badgeText = "AI",
                tint = SkyPrimary,
                icon = Icons.Filled.Psychology,
                onClick = onOpenAiDayParser
            )
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onOpenSearch, modifier = Modifier.weight(1f).height(48.dp)) {
                    Icon(Icons.Filled.Search, null)
                    Spacer(Modifier.width(6.dp))
                    Text("بحث شامل")
                }
                OutlinedButton(onClick = onOpenNotifications, modifier = Modifier.weight(1f).height(48.dp)) {
                    Icon(Icons.Filled.Notifications, null)
                    Spacer(Modifier.width(6.dp))
                    Text("التنبيهات")
                }
            }
        }
    }
}

@Composable
private fun HubCard(
    title: String,
    subtitle: String,
    badgeText: String,
    tint: androidx.compose.ui.graphics.Color,
    icon: ImageVector,
    testTag: String = "",
    onClick: () -> Unit
) {
    val mod = if (testTag.isNotBlank()) Modifier.fillMaxWidth().testTag(testTag) else Modifier.fillMaxWidth()
    GlassCard(modifier = mod, borderColor = tint.copy(alpha = 0.4f), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircleIconBadge(icon = icon, tint = tint)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusPill(badgeText, tint)
        }
    }
}
