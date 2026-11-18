package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.local.*
import com.example.domain.AnalyticsEngine
import com.example.domain.SpacedRepetitionHelper
import com.example.ui.components.*
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonUrgent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SkyPrimary
import java.time.LocalDate

@Composable
fun SubjectDetailScreen(
    subject: SubjectEntity,
    lessons: List<LessonEntity>,
    notes: List<StudyNoteEntity>,
    onBack: () -> Unit,
    onSaveLesson: (LessonEntity) -> Unit,
    onDeleteLesson: (String) -> Unit,
    onCompleteLessonWithLog: (LessonEntity, Int, Int, Int, Int, String) -> Unit,
    onAdvanceReview: (LessonEntity, Long?) -> Unit,
    onOpenFocusForLesson: (String, String) -> Unit
) {
    val today = LocalDate.now().toEpochDay()
    val tint = parseColorHex(subject.colorHex)
    val subjectLessons = remember(lessons, subject.id) { lessons.filter { it.subjectId == subject.id } }
    val subjectNotes = remember(notes, subject.id) { notes.filter { it.subjectId == subject.id } }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddLessonDialog by remember { mutableStateOf(false) }
    var completingLesson by remember { mutableStateOf<LessonEntity?>(null) }
    var editingReviewLesson by remember { mutableStateOf<LessonEntity?>(null) }

    val completedCount = subjectLessons.count { LessonStatus.fromRaw(it.status) == LessonStatus.COMPLETED }
    val backlogCount = subjectLessons.count { AnalyticsEngine.isBacklogLesson(it, today) }
    val pct = if (subjectLessons.isNotEmpty()) (completedCount * 100) / subjectLessons.size else 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(subject.name, style = MaterialTheme.typography.headlineMedium, color = tint)
                    if (subject.teacherName.isNotBlank()) {
                        Text("المعلم: ${subject.teacherName}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                FilledTonalButton(onClick = { showAddLessonDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("إضافة درس/واجب")
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = tint.copy(alpha = 0.45f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("الإجمالي: ${subjectLessons.size}", style = MaterialTheme.typography.labelLarge)
                        Text("المنتهي: $completedCount", style = MaterialTheme.typography.bodySmall, color = EmeraldSuccess)
                    }
                    Column {
                        Text("المتراكم: $backlogCount", style = MaterialTheme.typography.labelLarge, color = CrimsonUrgent)
                        Text("ساعات المذاكرة: ${String.format("%.1f", subject.studyMinutesLogged / 60f)}", style = MaterialTheme.typography.bodySmall)
                    }
                    StatusPill("$pct%", tint)
                }
                if (subject.notesSummary.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text("ملاحظات المادة: ${subject.notesSummary}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("الدروس والوحدات") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("المراجعة المتباعدة") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("الملاحظات (${subjectNotes.size})") })
            }
        }

        when (selectedTab) {
            0 -> {
                if (subjectLessons.isEmpty()) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text("أضف وحدات وفصول ودروس وواجبات المادة للبدء في متابعتها.", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    items(subjectLessons, key = { it.id }) { les ->
                        val status = LessonStatus.fromRaw(les.status)
                        val kind = LessonKind.fromRaw(les.kind)
                        val statusColor = when (status) {
                            LessonStatus.COMPLETED -> EmeraldSuccess
                            LessonStatus.IN_PROGRESS -> SkyPrimary
                            LessonStatus.NEEDS_REVIEW -> AmberWarning
                            LessonStatus.POSTPONED -> CrimsonUrgent
                            LessonStatus.NOT_STARTED -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = statusColor.copy(alpha = 0.35f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(les.title, style = MaterialTheme.typography.titleMedium)
                                    val hierarchy = listOf(les.unitName, les.chapterName, les.partLabel)
                                        .filter { it.isNotBlank() }
                                        .joinToString(" • ")
                                    if (hierarchy.isNotBlank()) {
                                        Text(hierarchy, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        StatusPill(kind.arabicLabel, tint)
                                        StatusPill(status.arabicLabel, statusColor)
                                        Text("${les.estimatedMinutes} د", style = MaterialTheme.typography.labelSmall)
                                    }
                                    if (les.comprehensionPercent > 0) {
                                        Text(
                                            "الفهم: ${les.comprehensionPercent}% • صفحات: ${les.pagesCount} • أسئلة: ${les.questionsSolved}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EmeraldSuccess
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (status != LessonStatus.COMPLETED) {
                                    FilledTonalButton(onClick = { completingLesson = les }) {
                                        Icon(Icons.Filled.Check, null, Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("تسجيل الإنجاز")
                                    }
                                }
                                OutlinedButton(onClick = { onOpenFocusForLesson(subject.id, les.id) }) {
                                    Icon(Icons.Filled.Timer, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("مؤقت")
                                }
                                IconButton(onClick = { onDeleteLesson(les.id) }) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف", tint = CrimsonUrgent)
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                val reviewItems = subjectLessons.filter {
                    LessonStatus.fromRaw(it.status) == LessonStatus.COMPLETED || it.nextReviewEpochDay > 0L
                }
                if (reviewItems.isEmpty()) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "عند إنهاء أي درس ستظهر هنا مواعيد المراجعة المتباعدة (+1 يوم، +3 أيام، +أسبوع، +أسبوعين) مع إمكانية تعديلها.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    items(reviewItems, key = { it.id }) { les ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text(les.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                SpacedRepetitionHelper.stageLabelArabic(les.reviewStage),
                                style = MaterialTheme.typography.bodySmall,
                                color = SkyPrimary
                            )
                            if (les.nextReviewEpochDay > 0L) {
                                val nextDate = LocalDate.ofEpochDay(les.nextReviewEpochDay)
                                Text("موعد المراجعة القادمة: $nextDate", style = MaterialTheme.typography.labelMedium)
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (les.reviewStage < 4) {
                                    FilledTonalButton(onClick = { onAdvanceReview(les, null) }) {
                                        Text("تمت المراجعة الآن")
                                    }
                                }
                                OutlinedButton(onClick = { editingReviewLesson = les }) {
                                    Text("تعديل الموعد")
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                if (subjectNotes.isEmpty()) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text("لا توجد ملاحظات مسجلة لهذه المادة بعد. أضف ملاحظاتك من تبويب المكتبة.", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    items(subjectNotes, key = { it.id }) { note ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text(note.title, style = MaterialTheme.typography.titleMedium)
                            Text(note.content, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    if (showAddLessonDialog) {
        AddLessonDialog(
            subjectId = subject.id,
            onSave = {
                onSaveLesson(it)
                showAddLessonDialog = false
            },
            onDismiss = { showAddLessonDialog = false }
        )
    }

    completingLesson?.let { target ->
        CompleteLessonDialog(
            lesson = target,
            onConfirm = { mins, pages, qs, comp, notesText ->
                onCompleteLessonWithLog(target, mins, pages, qs, comp, notesText)
                completingLesson = null
            },
            onDismiss = { completingLesson = null }
        )
    }

    editingReviewLesson?.let { target ->
        var daysFromToday by remember { mutableIntStateOf(((target.nextReviewEpochDay - today).coerceAtLeast(1L)).toInt()) }
        AlertDialog(
            onDismissRequest = { editingReviewLesson = null },
            title = { Text("تعديل موعد المراجعة") },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("بعد $daysFromToday يوم (${LocalDate.now().plusDays(daysFromToday.toLong())})")
                    Row {
                        IconButton(onClick = { daysFromToday = (daysFromToday - 1).coerceAtLeast(1) }) {
                            Icon(Icons.Filled.Remove, null)
                        }
                        IconButton(onClick = { daysFromToday++ }) {
                            Icon(Icons.Filled.Add, null)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSaveLesson(target.copy(nextReviewEpochDay = today + daysFromToday))
                    editingReviewLesson = null
                }) { Text("حفظ") }
            },
            dismissButton = {
                TextButton(onClick = { editingReviewLesson = null }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
private fun AddLessonDialog(
    subjectId: String,
    onSave: (LessonEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var unitName by remember { mutableStateOf("") }
    var chapterName by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var partLabel by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(LessonKind.LESSON) }
    var estimatedMinutes by remember { mutableIntStateOf(45) }
    var importance by remember { mutableIntStateOf(3) }
    var dueOffsetDays by remember { mutableIntStateOf(1) }

    val today = LocalDate.now().toEpochDay()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة درس / واجب / مراجعة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(LessonKind.entries) { k ->
                        FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text(k.arabicLabel) })
                    }
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("اسم الدرس / المهمة *") }, singleLine = true)
                OutlinedTextField(value = unitName, onValueChange = { unitName = it }, label = { Text("الوحدة (اختياري)") }, singleLine = true)
                OutlinedTextField(value = chapterName, onValueChange = { chapterName = it }, label = { Text("الفصل (اختياري)") }, singleLine = true)
                OutlinedTextField(value = partLabel, onValueChange = { partLabel = it }, label = { Text("الجزء (اختياري)") }, singleLine = true)
                Text("الوقت المتوقع: $estimatedMinutes دقيقة")
                Slider(value = estimatedMinutes.toFloat(), onValueChange = { estimatedMinutes = it.toInt() }, valueRange = 15f..180f)
                Text("الموعد النهائي: بعد $dueOffsetDays يوم")
                Slider(value = dueOffsetDays.toFloat(), onValueChange = { dueOffsetDays = it.toInt() }, valueRange = 0f..14f)
                Text("الأهمية: $importance / 5")
                Slider(value = importance.toFloat(), onValueChange = { importance = it.toInt() }, valueRange = 1f..5f, steps = 3)
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank(),
                onClick = {
                    onSave(
                        LessonEntity(
                            subjectId = subjectId,
                            unitName = unitName.trim(),
                            chapterName = chapterName.trim(),
                            title = title.trim(),
                            partLabel = partLabel.trim(),
                            kind = kind.name,
                            estimatedMinutes = estimatedMinutes,
                            difficultyOrImportance = importance,
                            startDateEpochDay = today,
                            dueDateEpochDay = today + dueOffsetDays
                        )
                    )
                }
            ) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun CompleteLessonDialog(
    lesson: LessonEntity,
    onConfirm: (Int, Int, Int, Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    var actualMinutes by remember { mutableStateOf(lesson.estimatedMinutes.toString()) }
    var pagesCount by remember { mutableStateOf("10") }
    var questionsSolved by remember { mutableStateOf("15") }
    var comprehension by remember { mutableIntStateOf(85) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسجيل تفاصيل المذاكرة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(lesson.title, style = MaterialTheme.typography.titleSmall, color = SkyPrimary)
                OutlinedTextField(value = actualMinutes, onValueChange = { actualMinutes = it }, label = { Text("وقت المذاكرة الفعلي (بالدقائق)") }, singleLine = true)
                OutlinedTextField(value = pagesCount, onValueChange = { pagesCount = it }, label = { Text("عدد الصفحات") }, singleLine = true)
                OutlinedTextField(value = questionsSolved, onValueChange = { questionsSolved = it }, label = { Text("عدد الأسئلة المحلولة") }, singleLine = true)
                Text("نسبة فهمك للدرس: $comprehension%")
                Slider(value = comprehension.toFloat(), onValueChange = { comprehension = it.toInt() }, valueRange = 10f..100f)
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("ملاحظات بعد الانتهاء") }, maxLines = 2)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        actualMinutes.toIntOrNull() ?: lesson.estimatedMinutes,
                        pagesCount.toIntOrNull() ?: 0,
                        questionsSolved.toIntOrNull() ?: 0,
                        comprehension,
                        notes
                    )
                }
            ) { Text("إتمام وجدولة المراجعة") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}
