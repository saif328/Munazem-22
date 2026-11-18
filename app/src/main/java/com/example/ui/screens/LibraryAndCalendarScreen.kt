package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.*
import com.example.ui.components.*
import com.example.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun LibraryAndCalendarScreen(
    subjects: List<SubjectEntity>,
    notes: List<StudyNoteEntity>,
    scheduleBlocks: List<ScheduleBlockEntity>,
    exams: List<ExamEntity>,
    lessons: List<LessonEntity>,
    holidays: List<HolidayOrBreakEntity>,
    onSaveNote: (StudyNoteEntity) -> Unit,
    onDeleteNote: (String) -> Unit,
    onSaveHoliday: (HolidayOrBreakEntity) -> Unit,
    onDeleteHoliday: (String) -> Unit
) {
    var topTab by remember { mutableIntStateOf(0) } // 0 = Library, 1 = Calendar

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = topTab, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Tab(selected = topTab == 0, onClick = { topTab = 0 }, text = { Text("المكتبة والملاحظات") })
            Tab(selected = topTab == 1, onClick = { topTab = 1 }, text = { Text("التقويم الدراسي") })
        }

        if (topTab == 0) {
            NotesLibraryTab(
                subjects = subjects,
                notes = notes,
                onSaveNote = onSaveNote,
                onDeleteNote = onDeleteNote
            )
        } else {
            AcademicCalendarTab(
                subjects = subjects,
                scheduleBlocks = scheduleBlocks,
                exams = exams,
                lessons = lessons,
                holidays = holidays,
                onSaveHoliday = onSaveHoliday,
                onDeleteHoliday = onDeleteHoliday
            )
        }
    }
}

@Composable
private fun NotesLibraryTab(
    subjects: List<SubjectEntity>,
    notes: List<StudyNoteEntity>,
    onSaveNote: (StudyNoteEntity) -> Unit,
    onDeleteNote: (String) -> Unit
) {
    val context = LocalContext.current
    val subjectMap = subjects.associateBy { it.id }

    var searchQuery by remember { mutableStateOf("") }
    var filterSubjectId by remember { mutableStateOf<String?>(null) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<StudyNoteEntity?>(null) }

    val filteredNotes = remember(notes, searchQuery, filterSubjectId) {
        notes.filter { note ->
            val matchSubj = filterSubjectId == null || note.subjectId == filterSubjectId
            val matchQuery = searchQuery.isBlank() ||
                note.title.contains(searchQuery, ignoreCase = true) ||
                note.content.contains(searchQuery, ignoreCase = true)
            matchSubj && matchQuery
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionHeader(
                title = "المكتبة الدراسية",
                subtitle = "ملاحظات، ملفات PDF، صور المذكرات، وروابط تعليمية",
                actionLabel = "+ ملاحظة / ملف",
                onActionClick = {
                    editingNote = null
                    showNoteDialog = true
                }
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("ابحث داخل الملاحظات...") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (subjects.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(selected = filterSubjectId == null, onClick = { filterSubjectId = null }, label = { Text("الكل") })
                    }
                    items(subjects) { s ->
                        FilterChip(selected = filterSubjectId == s.id, onClick = { filterSubjectId = s.id }, label = { Text(s.name) })
                    }
                }
            }
        }

        if (filteredNotes.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("لا توجد ملاحظات مطابقة. أضف ملاحظة أو صورة مذكرة أو ملف PDF أو رابطاً تعليمياً.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            items(filteredNotes, key = { it.id }) { note ->
                val attType = AttachmentType.fromRaw(note.attachmentType)
                val subjName = note.subjectId?.let { subjectMap[it]?.name }
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (note.important) AmberWarning.copy(alpha = 0.55f) else null
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (note.important) {
                                    Icon(Icons.Filled.Star, contentDescription = "مهم", tint = AmberWarning, modifier = Modifier.size(18.dp))
                                }
                                Text(note.title, style = MaterialTheme.typography.titleMedium)
                            }
                            if (!subjName.isNullOrBlank()) {
                                Text(subjName, style = MaterialTheme.typography.labelSmall, color = SkyPrimary)
                            }
                        }
                        Row {
                            IconButton(onClick = { onSaveNote(note.copy(important = !note.important)) }) {
                                Icon(
                                    if (note.important) Icons.Filled.Star else Icons.Filled.StarBorder,
                                    contentDescription = "علامة مهمة",
                                    tint = AmberWarning
                                )
                            }
                            IconButton(onClick = { onDeleteNote(note.id) }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف", tint = CrimsonUrgent)
                            }
                        }
                    }
                    if (note.content.isNotBlank()) {
                        Text(note.content, style = MaterialTheme.typography.bodySmall)
                    }
                    if (attType == AttachmentType.IMAGE && note.attachmentUri.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        AsyncImage(
                            model = note.attachmentUri,
                            contentDescription = "صورة المذكرة",
                            modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    if (attType == AttachmentType.PDF && note.attachmentUri.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(Uri.parse(note.attachmentUri), "application/pdf")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }) {
                            Icon(Icons.Filled.PictureAsPdf, null, tint = CrimsonUrgent)
                            Spacer(Modifier.width(6.dp))
                            Text("فتح ملف PDF المرفق")
                        }
                    }
                    if (note.linkUrl.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        TextButton(onClick = {
                            try {
                                val fixedUrl = if (note.linkUrl.startsWith("http")) note.linkUrl else "https://${note.linkUrl}"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fixedUrl)))
                            } catch (_: Exception) {}
                        }) {
                            Icon(Icons.Filled.Link, null)
                            Spacer(Modifier.width(4.dp))
                            Text(note.linkUrl, maxLines = 1)
                        }
                    }
                }
            }
        }
    }

    if (showNoteDialog) {
        NoteEditorDialog(
            initial = editingNote,
            subjects = subjects,
            onSave = {
                onSaveNote(it)
                showNoteDialog = false
            },
            onDismiss = { showNoteDialog = false }
        )
    }
}

@Composable
private fun NoteEditorDialog(
    initial: StudyNoteEntity?,
    subjects: List<SubjectEntity>,
    onSave: (StudyNoteEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var content by remember { mutableStateOf(initial?.content ?: "") }
    var linkUrl by remember { mutableStateOf(initial?.linkUrl ?: "") }
    var subjectId by remember { mutableStateOf(initial?.subjectId) }
    var important by remember { mutableStateOf(initial?.important ?: false) }
    var attachmentUri by remember { mutableStateOf(initial?.attachmentUri ?: "") }
    var attachmentType by remember { mutableStateOf(AttachmentType.fromRaw(initial?.attachmentType)) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            attachmentUri = uri.toString()
            attachmentType = AttachmentType.IMAGE
        }
    }
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}
            attachmentUri = uri.toString()
            attachmentType = AttachmentType.PDF
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة ملاحظة / مرفق") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("العنوان *") }, singleLine = true)
                OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("نص الملاحظة") }, maxLines = 4)
                OutlinedTextField(value = linkUrl, onValueChange = { linkUrl = it }, label = { Text("رابط تعليمي (اختياري)") }, singleLine = true)
                if (subjects.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(subjects) { s ->
                            FilterChip(
                                selected = subjectId == s.id,
                                onClick = { subjectId = if (subjectId == s.id) null else s.id },
                                label = { Text(s.name) }
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Icon(Icons.Filled.Image, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("صورة")
                    }
                    OutlinedButton(onClick = { pdfPicker.launch(arrayOf("application/pdf")) }) {
                        Icon(Icons.Filled.PictureAsPdf, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("PDF")
                    }
                }
                if (attachmentUri.isNotBlank()) {
                    Text("مرفق محدد: ${attachmentType.arabicLabel}", style = MaterialTheme.typography.labelSmall, color = EmeraldSuccess)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = important, onCheckedChange = { important = it })
                    Text("تمييز كملاحظة مهمة")
                }
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank(),
                onClick = {
                    val resolvedType = when {
                        attachmentType != AttachmentType.NONE -> attachmentType
                        linkUrl.isNotBlank() -> AttachmentType.LINK
                        else -> AttachmentType.NONE
                    }
                    onSave(
                        (initial ?: StudyNoteEntity(title = title.trim(), content = content.trim())).copy(
                            subjectId = subjectId,
                            title = title.trim(),
                            content = content.trim(),
                            linkUrl = linkUrl.trim(),
                            attachmentUri = attachmentUri,
                            attachmentType = resolvedType.name,
                            important = important
                        )
                    )
                }
            ) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun AcademicCalendarTab(
    subjects: List<SubjectEntity>,
    scheduleBlocks: List<ScheduleBlockEntity>,
    exams: List<ExamEntity>,
    lessons: List<LessonEntity>,
    holidays: List<HolidayOrBreakEntity>,
    onSaveHoliday: (HolidayOrBreakEntity) -> Unit,
    onDeleteHoliday: (String) -> Unit
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var calendarMode by remember { mutableIntStateOf(0) } // 0=Month, 1=Week, 2=Day

    val selectedEpoch = selectedDate.toEpochDay()
    val selectedDayOfWeek = (selectedDate.dayOfWeek.value % 7) + 1

    val dayBlocks = scheduleBlocks.filter {
        it.specificDateEpochDay == selectedEpoch || it.dayOfWeek == selectedDayOfWeek || it.dayOfWeek == 0
    }.sortedBy { it.startMinutes }
    val dayExams = exams.filter { it.examDateEpochDay == selectedEpoch }
    val dayReviews = lessons.filter { it.nextReviewEpochDay == selectedEpoch || it.dueDateEpochDay == selectedEpoch }
    val dayHolidays = holidays.filter { it.dateEpochDay == selectedEpoch }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("شهري", "أسبوعي", "يومي").forEachIndexed { idx, label ->
                    FilterChip(
                        selected = calendarMode == idx,
                        onClick = { calendarMode = idx },
                        label = { Text(label) }
                    )
                }
            }
        }

        if (calendarMode == 0) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                            Icon(Icons.Filled.ChevronRight, contentDescription = "الشهر السابق")
                        }
                        Text("${currentMonth.monthValue} / ${currentMonth.year}", style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                            Icon(Icons.Filled.ChevronLeft, contentDescription = "الشهر التالي")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    val daysInMonth = currentMonth.lengthOfMonth()
                    val firstDay = currentMonth.atDay(1)
                    val startOffset = (firstDay.dayOfWeek.value % 7) // 0=Sun..6=Sat
                    val totalCells = startOffset + daysInMonth
                    val rows = (totalCells + 6) / 7

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (r in 0 until rows) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                for (c in 0 until 7) {
                                    val cellIdx = r * 7 + c
                                    val dayNum = cellIdx - startOffset + 1
                                    if (dayNum in 1..daysInMonth) {
                                        val cellDate = currentMonth.atDay(dayNum)
                                        val cellEpoch = cellDate.toEpochDay()
                                        val isSelected = cellDate == selectedDate
                                        val hasExam = exams.any { it.examDateEpochDay == cellEpoch }
                                        val hasHoliday = holidays.any { it.dateEpochDay == cellEpoch }

                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    when {
                                                        isSelected -> SkyPrimary.copy(alpha = 0.28f)
                                                        hasExam -> CrimsonUrgent.copy(alpha = 0.2f)
                                                        hasHoliday -> EmeraldSuccess.copy(alpha = 0.2f)
                                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                                    }
                                                )
                                                .border(
                                                    width = if (isSelected) 1.5.dp else 0.dp,
                                                    color = if (isSelected) SkyPrimary else androidx.compose.ui.graphics.Color.Transparent,
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .clickable { selectedDate = cellDate },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("$dayNum", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                                        }
                                    } else {
                                        Spacer(Modifier.size(38.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (calendarMode == 1) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items((0..6).map { selectedDate.minusDays(2).plusDays(it.toLong()) }) { d ->
                        FilterChip(
                            selected = d == selectedDate,
                            onClick = { selectedDate = d },
                            label = { Text("${arabicDayName((d.dayOfWeek.value % 7) + 1)} ${d.dayOfMonth}") }
                        )
                    }
                }
            }
        }

        // Day Details Inspector
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "تفاصيل يوم ${arabicDayName(selectedDayOfWeek)} ($selectedDate)",
                        style = MaterialTheme.typography.titleMedium,
                        color = SkyPrimary
                    )
                    TextButton(onClick = {
                        onSaveHoliday(
                            HolidayOrBreakEntity(
                                dateEpochDay = selectedEpoch,
                                title = "إجازة مخصصة",
                                kind = HolidayKind.HOLIDAY.name,
                                blockAllStudy = true
                            )
                        )
                    }) {
                        Text("+ تسجيل كإجازة")
                    }
                }
                Spacer(Modifier.height(6.dp))
                if (dayHolidays.isNotEmpty()) {
                    dayHolidays.forEach { h ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusPill("إجازة: ${h.title}", EmeraldSuccess)
                            IconButton(onClick = { onDeleteHoliday(h.id) }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف الإجازة", tint = CrimsonUrgent)
                            }
                        }
                    }
                }
                if (dayExams.isNotEmpty()) {
                    dayExams.forEach { ex ->
                        Text("• امتحان: ${ex.title} (${formatMinutesArabic(ex.timeMinutes)})", color = CrimsonUrgent, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (dayReviews.isNotEmpty()) {
                    dayReviews.forEach { rev ->
                        Text("• مهمة / مراجعة: ${rev.title}", color = AmberWarning, style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (dayBlocks.isNotEmpty()) {
                    dayBlocks.forEach { b ->
                        Text(
                            "• ${formatMinutesArabic(b.startMinutes)}: ${b.title}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (dayHolidays.isEmpty() && dayExams.isEmpty() && dayReviews.isEmpty() && dayBlocks.isEmpty()) {
                    Text("لا توجد التزامات مسجلة في هذا اليوم.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
