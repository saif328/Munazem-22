package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.FocusModeType
import com.example.data.local.FocusSessionEntity
import com.example.data.local.LessonEntity
import com.example.data.local.SubjectEntity
import com.example.domain.DashboardAnalytics
import com.example.ui.components.GlassCard
import com.example.ui.components.PrimaryActionButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.SkyPrimary
import com.example.ui.viewmodel.FocusTimerState

@Composable
fun FocusTimerScreen(
    timerState: FocusTimerState,
    subjects: List<SubjectEntity>,
    lessons: List<LessonEntity>,
    focusSessions: List<FocusSessionEntity>,
    analytics: DashboardAnalytics,
    onConfigureTimer: (Int, Int, FocusModeType) -> Unit,
    onLinkTarget: (String?, String?) -> Unit,
    onStartOrPause: () -> Unit,
    onReset: () -> Unit,
    onSwitchPhase: (Boolean) -> Unit,
    onToggleFullscreen: (Boolean) -> Unit
) {
    val subjectMap = subjects.associateBy { it.id }
    val lessonMap = lessons.associateBy { it.id }

    val totalSeconds = (if (timerState.isBreakPhase) timerState.breakMinutes else timerState.focusMinutes) * 60
    val progress = if (totalSeconds > 0) timerState.remainingSeconds.toFloat() / totalSeconds.toFloat() else 0f
    val mins = timerState.remainingSeconds / 60
    val secs = timerState.remainingSeconds % 60
    val timeText = String.format("%02d:%02d", mins, secs)

    val phaseColor = if (timerState.isBreakPhase) EmeraldSuccess else SkyPrimary

    if (timerState.fullscreenMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(NavyDeep, Color(0xFF0A1636)))
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = { onToggleFullscreen(false) },
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(Icons.Filled.FullscreenExit, contentDescription = "الخروج من ملء الشاشة", tint = Color.White)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                StatusPill(
                    text = if (timerState.isBreakPhase) "فترة استراحة" else "وضع التركيز العميق — بدون مشتتات",
                    color = phaseColor
                )
                TimerRing(timeText = timeText, progress = progress, ringColor = phaseColor, sizeDp = 260)
                timerState.linkedSubjectId?.let { sId ->
                    Text(
                        text = subjectMap[sId]?.name ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PrimaryActionButton(
                        text = if (timerState.isRunning) "إيقاف مؤقت" else "استكمال",
                        icon = if (timerState.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        onClick = onStartOrPause
                    )
                    OutlinedButton(onClick = onReset, modifier = Modifier.height(50.dp)) {
                        Icon(Icons.Filled.Refresh, contentDescription = "إعادة", tint = Color.White)
                    }
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "مؤقت المذاكرة والتركيز",
                subtitle = "نظام بومودورو مع تسجيل تلقائي لساعات كل مادة",
                actionLabel = "ملء الشاشة",
                onActionClick = { onToggleFullscreen(true) }
            )
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = phaseColor.copy(alpha = 0.45f)) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !timerState.isBreakPhase,
                            onClick = { onSwitchPhase(false) },
                            label = { Text("تركيز (${timerState.focusMinutes} د)") }
                        )
                        FilterChip(
                            selected = timerState.isBreakPhase,
                            onClick = { onSwitchPhase(true) },
                            label = { Text("راحة (${timerState.breakMinutes} د)") }
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    TimerRing(timeText = timeText, progress = progress, ringColor = phaseColor, sizeDp = 210)
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PrimaryActionButton(
                            text = if (timerState.isRunning) "إيقاف مؤقت" else "ابدأ التركيز",
                            icon = if (timerState.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            testTag = "focus_start_pause_btn",
                            modifier = Modifier.weight(1f),
                            onClick = onStartOrPause
                        )
                        FilledTonalIconButton(
                            onClick = onReset,
                            modifier = Modifier.size(50.dp)
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "إعادة ضبط")
                        }
                        FilledTonalIconButton(
                            onClick = { onToggleFullscreen(true) },
                            modifier = Modifier.size(50.dp)
                        ) {
                            Icon(Icons.Filled.Fullscreen, contentDescription = "وضع ملء الشاشة")
                        }
                    }
                }
            }
        }

        // Customize Durations
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("تخصيص مدة التركيز والراحة", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val presets = listOf(
                        Triple("بومودورو 25/5", 25, 5),
                        Triple("جلسة عميقة 50/10", 50, 10),
                        Triple("مراجعة سريعة 15/3", 15, 3),
                        Triple("جلسة مكثفة 45/15", 45, 15)
                    )
                    items(presets) { (label, f, b) ->
                        FilterChip(
                            selected = timerState.focusMinutes == f && timerState.breakMinutes == b,
                            onClick = {
                                val mode = if (f == 25 && b == 5) FocusModeType.POMODORO else FocusModeType.CUSTOM
                                onConfigureTimer(f, b, mode)
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }

        // Link Subject & Task
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("ربط الجلسة بمادة ومهمة", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                if (subjects.isEmpty()) {
                    Text("أضف موادك لربط ساعات التركيز بها تلقائياً.", style = MaterialTheme.typography.bodySmall)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(subjects) { s ->
                            FilterChip(
                                selected = timerState.linkedSubjectId == s.id,
                                onClick = {
                                    val newSubj = if (timerState.linkedSubjectId == s.id) null else s.id
                                    onLinkTarget(newSubj, null)
                                },
                                label = { Text(s.name) }
                            )
                        }
                    }
                    val subjLessons = lessons.filter { it.subjectId == timerState.linkedSubjectId }
                    if (subjLessons.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(subjLessons) { l ->
                                FilterChip(
                                    selected = timerState.linkedLessonId == l.id,
                                    onClick = {
                                        val newLes = if (timerState.linkedLessonId == l.id) null else l.id
                                        onLinkTarget(timerState.linkedSubjectId, newLes)
                                    },
                                    label = { Text(l.title) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Daily & Weekly Focus Stats + Session Log
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("إحصائيات التركيز", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("ساعات اليوم", style = MaterialTheme.typography.bodySmall)
                        Text("${String.format("%.1f", analytics.todayStudyHours)} س", style = MaterialTheme.typography.titleLarge, color = SkyPrimary)
                    }
                    Column {
                        Text("ساعات الأسبوع", style = MaterialTheme.typography.bodySmall)
                        Text("${String.format("%.1f", analytics.weeklyStudyHours)} س", style = MaterialTheme.typography.titleLarge, color = EmeraldSuccess)
                    }
                    Column {
                        Text("عدد الجلسات", style = MaterialTheme.typography.bodySmall)
                        Text("${focusSessions.size}", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }

        if (focusSessions.isNotEmpty()) {
            item {
                Text("أحدث جلسات التركيز المسجلة", style = MaterialTheme.typography.titleMedium)
            }
            items(focusSessions.take(8), key = { it.id }) { sess ->
                val sName = sess.subjectId?.let { subjectMap[it]?.name } ?: "مذاكرة عامة"
                val lName = sess.lessonId?.let { lessonMap[it]?.title }
                GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(sName, style = MaterialTheme.typography.titleSmall)
                            if (!lName.isNullOrBlank()) {
                                Text(lName, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        StatusPill("${sess.durationMinutes} دقيقة", EmeraldSuccess)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimerRing(
    timeText: String,
    progress: Float,
    ringColor: Color,
    sizeDp: Int
) {
    Box(
        modifier = Modifier.size(sizeDp.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(
                color = ringColor.copy(alpha = 0.16f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Text(
            text = timeText,
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = ringColor
        )
    }
}
