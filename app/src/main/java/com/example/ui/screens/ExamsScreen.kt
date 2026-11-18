package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.ExamEntity
import com.example.data.local.SubjectEntity
import com.example.ui.components.*
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonUrgent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SkyPrimary
import java.time.LocalDate

@Composable
fun ExamsScreen(
    exams: List<ExamEntity>,
    subjects: List<SubjectEntity>,
    onSaveExam: (ExamEntity) -> Unit,
    onDeleteExam: (String) -> Unit
) {
    val today = LocalDate.now().toEpochDay()
    val subjectMap = subjects.associateBy { it.id }

    val upcomingExams = remember(exams, today) {
        exams.filter { it.examDateEpochDay >= today }.sortedBy { it.examDateEpochDay }
    }
    val pastExams = remember(exams, today) {
        exams.filter { it.examDateEpochDay < today || it.achievedScore != null }
            .sortedBy { it.examDateEpochDay }
    }

    var showAddExamDialog by remember { mutableStateOf(false) }
    var editingExam by remember { mutableStateOf<ExamEntity?>(null) }
    var loggingScoreExam by remember { mutableStateOf<ExamEntity?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "مركز الامتحانات والنتائج",
                subtitle = "عد تنازلي للامتحانات، خطط المراجعة، وتحليل تطور الدرجات",
                actionLabel = "+ امتحان",
                onActionClick = {
                    editingExam = null
                    showAddExamDialog = true
                }
            )
        }

        val gradedExams = pastExams.filter { it.achievedScore != null && it.maxScore > 0 }
        if (gradedExams.isNotEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("تحليل تطور الدرجات", style = MaterialTheme.typography.titleMedium, color = EmeraldSuccess)
                    Spacer(Modifier.height(10.dp))
                    ScoreProgressionChart(
                        scores = gradedExams.map {
                            val pct = ((it.achievedScore ?: 0.0) / it.maxScore * 100.0).toFloat()
                            it.title to pct
                        }
                    )
                }
            }
        }

        if (upcomingExams.isEmpty() && pastExams.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("لا توجد امتحانات مسجلة بعد.", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "أضف مواعيد امتحاناتك ليبدأ العد التنازلي ويقترح لك المنظم خطة مراجعة قبل كل امتحان.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    PrimaryActionButton(
                        text = "إضافة امتحان جديد",
                        icon = Icons.Filled.Add,
                        testTag = "add_exam_empty_btn",
                        onClick = { showAddExamDialog = true }
                    )
                }
            }
        } else {
            if (upcomingExams.isNotEmpty()) {
                item {
                    Text("الامتحانات القادمة (${upcomingExams.size})", style = MaterialTheme.typography.titleMedium, color = AmberWarning)
                }
                items(upcomingExams, key = { it.id }) { ex ->
                    val subj = subjectMap[ex.subjectId]
                    val daysLeft = (ex.examDateEpochDay - today).coerceAtLeast(0)
                    val urgencyColor = when {
                        daysLeft <= 3 -> CrimsonUrgent
                        daysLeft <= 7 -> AmberWarning
                        else -> SkyPrimary
                    }
                    GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = urgencyColor.copy(alpha = 0.45f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ex.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${subj?.name ?: "مادة"} • ${LocalDate.ofEpochDay(ex.examDateEpochDay)} • ${formatMinutesArabic(ex.timeMinutes)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusPill(
                                text = if (daysLeft == 0L) "اليوم!" else "بعد $daysLeft يوم",
                                color = urgencyColor
                            )
                        }
                        if (ex.requiredParts.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text("الأجزاء المطلوبة: ${ex.requiredParts}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (ex.revisionPlanSummary.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text("خطة المراجعة: ${ex.revisionPlanSummary}", style = MaterialTheme.typography.bodySmall, color = SkyPrimary)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("مستوى الاستعداد: ${ex.readinessPercent}%", style = MaterialTheme.typography.labelMedium)
                        LinearProgressIndicator(
                            progress = { ex.readinessPercent / 100f },
                            color = EmeraldSuccess,
                            trackColor = EmeraldSuccess.copy(alpha = 0.16f),
                            modifier = Modifier.fillMaxWidth().height(7.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(onClick = { loggingScoreExam = ex }) {
                                Text("رصد الدرجة")
                            }
                            OutlinedButton(onClick = {
                                editingExam = ex
                                showAddExamDialog = true
                            }) {
                                Text("تعديل / خطة")
                            }
                            IconButton(onClick = { onDeleteExam(ex.id) }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف", tint = CrimsonUrgent)
                            }
                        }
                    }
                }
            }

            if (pastExams.isNotEmpty()) {
                item {
                    Text("سجل نتائج الامتحانات (${pastExams.size})", style = MaterialTheme.typography.titleMedium, color = EmeraldSuccess)
                }
                items(pastExams, key = { "past_${it.id}" }) { ex ->
                    val subj = subjectMap[ex.subjectId]
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${ex.title} (${subj?.name ?: ""})", style = MaterialTheme.typography.titleSmall)
                                Text("${LocalDate.ofEpochDay(ex.examDateEpochDay)}", style = MaterialTheme.typography.bodySmall)
                            }
                            if (ex.achievedScore != null) {
                                StatusPill("${ex.achievedScore} / ${ex.maxScore}", EmeraldSuccess)
                            } else {
                                TextButton(onClick = { loggingScoreExam = ex }) {
                                    Text("إضافة الدرجة")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddExamDialog) {
        ExamEditorDialog(
            initial = editingExam,
            subjects = subjects,
            onSave = { ex ->
                onSaveExam(ex)
                showAddExamDialog = false
            },
            onDismiss = { showAddExamDialog = false }
        )
    }

    loggingScoreExam?.let { target ->
        var scoreText by remember { mutableStateOf(target.achievedScore?.toString() ?: "") }
        var maxScoreText by remember { mutableStateOf(target.maxScore.toString()) }
        AlertDialog(
            onDismissRequest = { loggingScoreExam = null },
            title = { Text("تسجيل درجة الامتحان") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(target.title, style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(value = scoreText, onValueChange = { scoreText = it }, label = { Text("الدرجة المحققة") }, singleLine = true)
                    OutlinedTextField(value = maxScoreText, onValueChange = { maxScoreText = it }, label = { Text("الدرجة النهائية") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val s = scoreText.toDoubleOrNull()
                    val m = maxScoreText.toDoubleOrNull()?.coerceAtLeast(1.0) ?: target.maxScore
                    if (s != null) {
                        onSaveExam(target.copy(achievedScore = s.coerceIn(0.0, m), maxScore = m))
                    }
                    loggingScoreExam = null
                }) { Text("حفظ الدرجة") }
            },
            dismissButton = { TextButton(onClick = { loggingScoreExam = null }) { Text("إلغاء") } }
        )
    }
}

@Composable
private fun ScoreProgressionChart(scores: List<Pair<String, Float>>) {
    Column {
        Canvas(modifier = Modifier.fillMaxWidth().height(95.dp)) {
            if (scores.size == 1) {
                val y = size.height * (1f - (scores[0].second / 100f).coerceIn(0f, 1f))
                drawCircle(EmeraldSuccess, radius = 6.dp.toPx(), center = Offset(size.width / 2f, y))
            } else if (scores.size > 1) {
                val path = Path()
                val stepX = size.width / (scores.size - 1).coerceAtLeast(1)
                scores.forEachIndexed { i, (_, pct) ->
                    val x = i * stepX
                    val y = size.height * (1f - (pct / 100f).coerceIn(0.05f, 0.95f))
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    drawCircle(SkyPrimary, radius = 5.dp.toPx(), center = Offset(x, y))
                }
                drawPath(path, color = EmeraldSuccess, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            scores.takeLast(5).forEach { (title, pct) ->
                Text("${title.take(10)}: ${pct.toInt()}%", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun ExamEditorDialog(
    initial: ExamEntity?,
    subjects: List<SubjectEntity>,
    onSave: (ExamEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val today = LocalDate.now().toEpochDay()
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var selectedSubjectId by remember { mutableStateOf(initial?.subjectId ?: subjects.firstOrNull()?.id ?: "") }
    var daysAhead by remember { mutableIntStateOf(((initial?.examDateEpochDay ?: (today + 5)) - today).toInt().coerceAtLeast(0)) }
    var requiredParts by remember { mutableStateOf(initial?.requiredParts ?: "") }
    var readiness by remember { mutableIntStateOf(initial?.readinessPercent ?: 50) }
    var revisionPlan by remember { mutableStateOf(initial?.revisionPlanSummary ?: "") }
    var reminder by remember { mutableStateOf(initial?.reminderEnabled ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "إضافة امتحان" else "تعديل الامتحان") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("اسم الامتحان *") }, singleLine = true)
                if (subjects.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(subjects) { s ->
                            FilterChip(selected = selectedSubjectId == s.id, onClick = { selectedSubjectId = s.id }, label = { Text(s.name) })
                        }
                    }
                }
                Text("موعد الامتحان: بعد $daysAhead يوم (${LocalDate.now().plusDays(daysAhead.toLong())})")
                Slider(value = daysAhead.toFloat(), onValueChange = { daysAhead = it.toInt() }, valueRange = 0f..60f)
                OutlinedTextField(value = requiredParts, onValueChange = { requiredParts = it }, label = { Text("الأجزاء المطلوبة") }, maxLines = 2)
                Text("نسبة الاستعداد الحالية: $readiness%")
                Slider(value = readiness.toFloat(), onValueChange = { readiness = it.toInt() }, valueRange = 0f..100f)
                OutlinedTextField(value = revisionPlan, onValueChange = { revisionPlan = it }, label = { Text("خطة المراجعة قبل الامتحان") }, maxLines = 2)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = reminder, onCheckedChange = { reminder = it })
                    Text("تفعيل التذكير قبل الامتحان")
                }
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank() && selectedSubjectId.isNotBlank(),
                onClick = {
                    val autoPlan = if (revisionPlan.isBlank()) {
                        "مراجعة الأجزاء المطلوبة على ${daysAhead.coerceIn(1, 5)} أيام + حل نماذج سابقة"
                    } else revisionPlan.trim()
                    onSave(
                        (initial ?: ExamEntity(title = title.trim(), subjectId = selectedSubjectId, examDateEpochDay = today + daysAhead)).copy(
                            title = title.trim(),
                            subjectId = selectedSubjectId,
                            examDateEpochDay = today + daysAhead,
                            requiredParts = requiredParts.trim(),
                            readinessPercent = readiness,
                            revisionPlanSummary = autoPlan,
                            reminderEnabled = reminder
                        )
                    )
                }
            ) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}
