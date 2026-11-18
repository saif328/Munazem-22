package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.*
import com.example.domain.DashboardAnalytics
import com.example.domain.GamificationSnapshot
import com.example.ui.components.*
import com.example.ui.theme.*
import java.time.LocalDate

@Composable
fun DashboardScreen(
    profile: StudentProfileEntity?,
    analytics: DashboardAnalytics,
    gamification: GamificationSnapshot,
    todayBlocks: List<ScheduleBlockEntity>,
    exams: List<ExamEntity>,
    subjects: List<SubjectEntity>,
    unreadNotificationsCount: Int,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenAiDayParser: () -> Unit,
    onOpenFocusTimer: () -> Unit,
    onOpenBacklog: () -> Unit,
    onOpenSchedule: () -> Unit,
    onOpenExams: () -> Unit,
    onOpenDailyTasks: () -> Unit,
    onOpenAssistant: () -> Unit,
    onOpenProfile: () -> Unit,
    onToggleBlockDone: (ScheduleBlockEntity) -> Unit
) {
    val today = LocalDate.now()
    val todayEpoch = today.toEpochDay()
    val nextExam = exams.filter { it.examDateEpochDay >= todayEpoch }.minByOrNull { it.examDateEpochDay }
    val subjectMap = subjects.associateBy { it.id }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f).clickable { onOpenProfile() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SkyPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!profile?.avatarUri.isNullOrBlank()) {
                            AsyncImage(
                                model = profile?.avatarUri,
                                contentDescription = "صورة الطالب",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = SkyPrimary)
                        }
                    }
                    Column {
                        Text(
                            text = "مرحباً، ${profile?.name?.ifBlank { "بطل" } ?: "طالب"}",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "المستوى ${gamification.level} • ${gamification.levelTitle}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SkyPrimary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onOpenSearch,
                        modifier = Modifier.testTag("dashboard_search_btn")
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = "بحث")
                    }
                    IconButton(
                        onClick = onOpenNotifications,
                        modifier = Modifier.testTag("dashboard_notifications_btn")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotificationsCount > 0) {
                                    Badge { Text("$unreadNotificationsCount") }
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Notifications, contentDescription = "التنبيهات")
                        }
                    }
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = "تبديل المظهر"
                        )
                    }
                }
            }
        }

        // Hero Banner Card ("وصف يومك بالذكاء الاصطناعي")
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                borderColor = SkyPrimary.copy(alpha = 0.45f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        StatusPill("MUNAZZEM STUDY PRO", SkyPrimary, icon = Icons.Filled.AutoAwesome)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "وقتك منظم، ومستقبلك أقرب.",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = profile?.studyGoal?.ifBlank { "اخبر الذكاء الاصطناعي بيومك ليرتب لك جدولك بدون تعارض" }
                                ?: "اخبر الذكاء الاصطناعي بيومك ليرتب لك جدولك بدون تعارض",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PrimaryActionButton(
                        text = "صف يومك للذكاء الاصطناعي",
                        icon = Icons.Filled.Psychology,
                        testTag = "open_ai_day_parser_btn",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenAiDayParser
                    )
                    FilledTonalButton(
                        onClick = onOpenFocusTimer,
                        modifier = Modifier.height(50.dp).testTag("quick_focus_btn"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Filled.Timer, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("تركيز")
                    }
                }
            }
        }

        // 4 KPI Cards Row
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    KpiMiniCard(
                        title = "مذاكرة اليوم",
                        value = "${String.format("%.1f", analytics.todayStudyHours)} س",
                        subtitle = "الأسبوع: ${String.format("%.1f", analytics.weeklyStudyHours)} س",
                        icon = Icons.Filled.Schedule,
                        color = SkyPrimary,
                        onClick = onOpenFocusTimer
                    )
                }
                item {
                    KpiMiniCard(
                        title = "نسبة الإنجاز",
                        value = "${analytics.completionRatePercent}%",
                        subtitle = "${analytics.completedTasksCount}/${analytics.totalTasksCount} مهمة",
                        icon = Icons.Filled.TaskAlt,
                        color = EmeraldSuccess,
                        onClick = onOpenSchedule
                    )
                }
                item {
                    KpiMiniCard(
                        title = "المتراكم",
                        value = "${analytics.backlogCount}",
                        subtitle = if (analytics.backlogCount == 0) "يومك نظيف!" else "اضغط للمعالجة",
                        icon = Icons.Filled.Layers,
                        color = if (analytics.backlogCount > 0) CrimsonUrgent else EmeraldSuccess,
                        onClick = onOpenBacklog
                    )
                }
                item {
                    KpiMiniCard(
                        title = "سلسلة الالتزام",
                        value = "${gamification.streakDays} يوم",
                        subtitle = "${gamification.xp} XP",
                        icon = Icons.Filled.LocalFireDepartment,
                        color = AmberWarning,
                        onClick = onOpenProfile
                    )
                }
            }
        }

        // Next Exam Countdown
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenExams,
                borderColor = if (nextExam != null) AmberWarning.copy(alpha = 0.45f) else null
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircleIconBadge(
                        icon = Icons.Filled.EventAvailable,
                        tint = if (nextExam != null) AmberWarning else SkyPrimary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        if (nextExam != null) {
                            val daysLeft = (nextExam.examDateEpochDay - todayEpoch).coerceAtLeast(0)
                            val subjName = subjectMap[nextExam.subjectId]?.name ?: ""
                            Text(
                                text = "الامتحان القادم: ${nextExam.title} ($subjName)",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "متبقي $daysLeft يوم • الاستعداد ${nextExam.readinessPercent}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = AmberWarning
                            )
                        } else {
                            Text("مركز الامتحانات والمراجعة", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "أضف امتحاناتك القادمة ليجهز لك المنظم عداداً تنازلياً وخطة مراجعة.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(Icons.Filled.ChevronLeft, contentDescription = null)
                }
            }
        }

        // Daily Tasks (Firestore) Quick Entry Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_daily_tasks_card"),
                onClick = onOpenDailyTasks,
                borderColor = EmeraldSuccess.copy(alpha = 0.45f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircleIconBadge(
                        icon = Icons.Filled.Checklist,
                        tint = EmeraldSuccess
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("مهام اليوم السحابية", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "قائمة مهامك لليوم الحالي مع المزامنة الفورية عبر Firestore",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StatusPill("Firestore", EmeraldSuccess, icon = Icons.Filled.CloudDone)
                    Icon(Icons.Filled.ChevronLeft, contentDescription = null)
                }
            }
        }

        // Today's Schedule Preview
        item {
            SectionHeader(
                title = "جدول اليوم",
                subtitle = "المهام والحصص المجدولة لليوم",
                actionLabel = "الجدول الكامل",
                onActionClick = onOpenSchedule
            )
            Spacer(Modifier.height(8.dp))
            if (todayBlocks.isEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "لا توجد مهام مجدولة لليوم بعد. استخدم «صف يومك للذكاء الاصطناعي» أو أضف مهامك من الجدول.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    todayBlocks.take(4).forEach { blk ->
                        val cat = BlockCategory.fromRaw(blk.category)
                        val tint = when (cat) {
                            BlockCategory.PRIVATE_LESSON -> AmberWarning
                            BlockCategory.SCHOOL -> IndigoAccent
                            BlockCategory.BREAK, BlockCategory.MEAL -> EmeraldSuccess
                            BlockCategory.HOLIDAY -> SkyBright
                            else -> SkyPrimary
                        }
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(12.dp),
                            onClick = { onToggleBlockDone(blk) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Checkbox(
                                    checked = blk.completed,
                                    onCheckedChange = { onToggleBlockDone(blk) }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(blk.title, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${formatMinutesArabic(blk.startMinutes)} - ${formatMinutesArabic(blk.endMinutes)} • ${cat.arabicLabel}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (blk.fixed) {
                                    Icon(
                                        Icons.Filled.PushPin,
                                        contentDescription = "مثبت",
                                        tint = tint,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Weekly Study Hours Chart
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("إحصائيات ساعات المذاكرة الأسبوعية", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "أكثر مادة استهلاكاً للوقت: ${analytics.topTimeSubjectName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SkyPrimary
                        )
                    }
                    StatusPill(
                        text = "التزام ${analytics.consistencyPercent}%",
                        color = EmeraldSuccess
                    )
                }
                Spacer(Modifier.height(14.dp))
                WeeklyHoursBarChart(
                    points = analytics.dailyChartPoints.map { it.dayLabel to it.hours },
                    barColor = SkyPrimary
                )
                Spacer(Modifier.height(8.dp))
                val diff = analytics.weeklyStudyHours - analytics.previousWeekStudyHours
                val cmpText = when {
                    diff > 0.1f -> "زيادة بمقدار ${String.format("%.1f", diff)} ساعة عن الأسبوع الماضي"
                    diff < -0.1f -> "أقل بـ ${String.format("%.1f", -diff)} ساعة مقارنة بالأسبوع الماضي"
                    else -> "مساوٍ تقريباً لمعدل الأسبوع الماضي"
                }
                Text(
                    text = "مقارنة الأداء: $cmpText • نسبة التأخير: ${analytics.delayRatePercent}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Weekly Summary Report
        item {
            val report = analytics.weeklyReport
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Summarize, contentDescription = null, tint = EmeraldSuccess)
                    Text("التقرير الأسبوعي الذكي", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.height(10.dp))
                ReportLine("ماذا أنجزت؟", report.completedSummary, EmeraldSuccess)
                ReportLine("ماذا لم يُنجز؟", report.pendingSummary, AmberWarning)
                ReportLine(
                    "مواد تحتاج اهتماماً:",
                    if (report.attentionSubjects.isEmpty()) "جميع المواد تسير بمستوى جيد."
                    else report.attentionSubjects.joinToString(" • "),
                    CrimsonUrgent
                )
                ReportLine("أفضل أيام المذاكرة:", report.bestStudyDay, SkyPrimary)
                ReportLine("خطة الأسبوع القادم:", report.nextWeekSuggestion, IndigoAccent)

                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onOpenAssistant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.SmartToy, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("ناقش مستواك مع MUNAZZEM AI")
                }
            }
        }
    }
}

@Composable
private fun KpiMiniCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.width(152.dp),
        cornerRadius = 20.dp,
        borderColor = color.copy(alpha = 0.35f),
        onClick = onClick
    ) {
        CircleIconBadge(icon = icon, tint = color, size = 36.dp)
        Spacer(Modifier.height(10.dp))
        Text(value, style = MaterialTheme.typography.headlineMedium, color = color, fontWeight = FontWeight.Bold)
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(
            subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun WeeklyHoursBarChart(
    points: List<Pair<String, Float>>,
    barColor: Color
) {
    val maxVal = (points.maxOfOrNull { it.second } ?: 1f).coerceAtLeast(1f)
    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
        ) {
            val count = points.size.coerceAtLeast(1)
            val slotWidth = size.width / count
            val barWidth = (slotWidth * 0.45f).coerceAtMost(32.dp.toPx())
            points.forEachIndexed { idx, (_, value) ->
                val ratio = (value / maxVal).coerceIn(0f, 1f)
                val barHeight = (size.height - 8.dp.toPx()) * ratio.coerceAtLeast(0.06f)
                val left = idx * slotWidth + (slotWidth - barWidth) / 2f
                val top = size.height - barHeight
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(barColor, barColor.copy(alpha = 0.35f))
                    ),
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            points.forEach { (label, hrs) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(String.format("%.1f", hrs), style = MaterialTheme.typography.labelSmall, color = barColor)
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ReportLine(label: String, text: String, accent: Color) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = accent)
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}
