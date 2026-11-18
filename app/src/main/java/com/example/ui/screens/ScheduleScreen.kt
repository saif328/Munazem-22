package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.*
import com.example.ui.components.*
import com.example.ui.theme.*
import java.time.LocalDate

@Composable
fun ScheduleScreen(
    scheduleBlocks: List<ScheduleBlockEntity>,
    overflowLessons: List<LessonEntity>,
    warnings: List<String>,
    subjects: List<SubjectEntity>,
    holidays: List<HolidayOrBreakEntity>,
    onRegenerateSchedule: () -> Unit,
    onAddOrUpdateBlock: (ScheduleBlockEntity) -> Unit,
    onTogglePin: (ScheduleBlockEntity) -> Unit,
    onToggleComplete: (ScheduleBlockEntity) -> Unit,
    onMoveOrder: (List<ScheduleBlockEntity>, Int, Int) -> Unit,
    onDeleteBlock: (String) -> Unit,
    onSaveHoliday: (HolidayOrBreakEntity) -> Unit,
    onDeleteHoliday: (String) -> Unit
) {
    val today = LocalDate.now()
    val todayDayOfWeek = (today.dayOfWeek.value % 7) + 1 // 1=Sun..7=Sat
    var selectedDay by remember { mutableIntStateOf(todayDayOfWeek) }
    var weeklyViewMode by remember { mutableStateOf(false) }

    var showAddBlockDialog by remember { mutableStateOf(false) }
    var editingBlock by remember { mutableStateOf<ScheduleBlockEntity?>(null) }
    var showHolidayDialog by remember { mutableStateOf(false) }

    val subjectMap = subjects.associateBy { it.id }

    val dayBlocks = remember(scheduleBlocks, selectedDay) {
        scheduleBlocks
            .filter { it.dayOfWeek == selectedDay || it.dayOfWeek == 0 }
            .sortedWith(compareBy({ it.startMinutes }, { it.orderIndex }))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "جدول المذاكرة الذكي",
                subtitle = "جدول يومي وأسبوعي بدون تعارض وباحترام وقت النوم والراحة",
                actionLabel = if (weeklyViewMode) "عرض يومي" else "عرض أسبوعي",
                onActionClick = { weeklyViewMode = !weeklyViewMode }
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PrimaryActionButton(
                    text = "توليد الجدول الذكي",
                    icon = Icons.Filled.AutoFixHigh,
                    testTag = "regenerate_schedule_btn",
                    modifier = Modifier.weight(1f),
                    onClick = onRegenerateSchedule
                )
                FilledTonalButton(
                    onClick = {
                        editingBlock = null
                        showAddBlockDialog = true
                    },
                    modifier = Modifier.height(50.dp).testTag("add_schedule_block_btn"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "إضافة")
                    Spacer(Modifier.width(4.dp))
                    Text("موعد")
                }
                FilledTonalIconButton(
                    onClick = { showHolidayDialog = true },
                    modifier = Modifier.size(50.dp)
                ) {
                    Icon(Icons.Filled.BeachAccess, contentDescription = "الإجازات والراحة")
                }
            }
        }

        if (warnings.isNotEmpty()) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = AmberWarning.copy(alpha = 0.6f)
                ) {
                    StatusPill("تعارضات في المواعيد المثبتة", AmberWarning, icon = Icons.Filled.Warning)
                    Spacer(Modifier.height(6.dp))
                    warnings.forEach { w ->
                        Text("• $w", style = MaterialTheme.typography.bodySmall, color = AmberWarning)
                    }
                }
            }
        }

        if (!weeklyViewMode) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items((1..7).toList()) { day ->
                        FilterChip(
                            selected = selectedDay == day,
                            onClick = { selectedDay = day },
                            label = {
                                Text(
                                    if (day == todayDayOfWeek) "${arabicDayName(day)} (اليوم)"
                                    else arabicDayName(day)
                                )
                            }
                        )
                    }
                }
            }

            if (dayBlocks.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "لا توجد حصص أو مهام مجدولة ليوم ${arabicDayName(selectedDay)}. اضغط «توليد الجدول الذكي» أو «+ موعد» لإضافة حصة أو فترة راحة.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                itemsIndexed(dayBlocks, key = { _, item -> item.id }) { idx, blk ->
                    ScheduleBlockCard(
                        block = blk,
                        subjectName = blk.subjectId?.let { subjectMap[it]?.name },
                        canMoveUp = idx > 0,
                        canMoveDown = idx < dayBlocks.lastIndex,
                        onMoveUp = { onMoveOrder(dayBlocks, idx, idx - 1) },
                        onMoveDown = { onMoveOrder(dayBlocks, idx, idx + 1) },
                        onToggleComplete = { onToggleComplete(blk) },
                        onTogglePin = { onTogglePin(blk) },
                        onEdit = {
                            editingBlock = blk
                            showAddBlockDialog = true
                        },
                        onDelete = { onDeleteBlock(blk.id) }
                    )
                }
            }
        } else {
            items((1..7).toList()) { day ->
                val list = scheduleBlocks.filter { it.dayOfWeek == day || it.dayOfWeek == 0 }
                    .sortedBy { it.startMinutes }
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(arabicDayName(day), style = MaterialTheme.typography.titleMedium, color = SkyPrimary)
                        Text("${list.size} فترات", style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    if (list.isEmpty()) {
                        Text("يوم حر / بدون مهام مجدولة", style = MaterialTheme.typography.bodySmall)
                    } else {
                        list.forEach { blk ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(blk.title, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                Text(
                                    "${formatMinutesArabic(blk.startMinutes)} - ${formatMinutesArabic(blk.endMinutes)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Overflow Lessons ("المهام المتبقية" instead of cramming student's day)
        if (overflowLessons.isNotEmpty()) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = AmberWarning.copy(alpha = 0.5f)
                ) {
                    StatusPill("المهام المتبقية (بدون ضغط على يومك)", AmberWarning, icon = Icons.Filled.HourglassBottom)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "لم يستوعب الجدول الصحي هذه المهام دون المساس بوقت النوم أو الراحة، لذا احتفظنا بها هنا للأيام التالية:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    overflowLessons.forEach { les ->
                        val sName = subjectMap[les.subjectId]?.name ?: "مادة"
                        Text("• $sName: ${les.title} (${les.estimatedMinutes} د)", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    if (showAddBlockDialog) {
        AddEditScheduleBlockDialog(
            initialBlock = editingBlock,
            defaultDayOfWeek = selectedDay,
            subjects = subjects,
            onSave = { blk ->
                onAddOrUpdateBlock(blk)
                showAddBlockDialog = false
            },
            onDismiss = { showAddBlockDialog = false }
        )
    }

    if (showHolidayDialog) {
        HolidaysManagerDialog(
            holidays = holidays,
            onSaveHoliday = onSaveHoliday,
            onDeleteHoliday = onDeleteHoliday,
            onDismiss = { showHolidayDialog = false }
        )
    }
}

@Composable
private fun ScheduleBlockCard(
    block: ScheduleBlockEntity,
    subjectName: String?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onToggleComplete: () -> Unit,
    onTogglePin: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val category = BlockCategory.fromRaw(block.category)
    val accent = when (category) {
        BlockCategory.PRIVATE_LESSON -> AmberWarning
        BlockCategory.SCHOOL -> IndigoAccent
        BlockCategory.BREAK, BlockCategory.MEAL -> EmeraldSuccess
        BlockCategory.HOLIDAY -> SkyBright
        BlockCategory.SLEEP -> TextSecondaryDark
        else -> SkyPrimary
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = accent.copy(alpha = 0.4f),
        contentPadding = PaddingValues(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Checkbox(checked = block.completed, onCheckedChange = { onToggleComplete() })
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(block.title, style = MaterialTheme.typography.titleSmall)
                    if (block.fixed) {
                        StatusPill("مثبت", accent, icon = Icons.Filled.PushPin)
                    }
                }
                Text(
                    "${formatMinutesArabic(block.startMinutes)} - ${formatMinutesArabic(block.endMinutes)} • ${category.arabicLabel}" +
                        (if (!subjectName.isNullOrBlank()) " • $subjectName" else ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            if (canMoveUp) {
                IconButton(onClick = onMoveUp) {
                    Icon(Icons.Filled.ArrowUpward, contentDescription = "تقديم")
                }
            }
            if (canMoveDown) {
                IconButton(onClick = onMoveDown) {
                    Icon(Icons.Filled.ArrowDownward, contentDescription = "تأخير")
                }
            }
            IconButton(onClick = onTogglePin) {
                Icon(
                    Icons.Filled.PushPin,
                    contentDescription = "تثبيت الموعد",
                    tint = if (block.fixed) accent else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "تعديل")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف", tint = CrimsonUrgent)
            }
        }
    }
}

@Composable
private fun AddEditScheduleBlockDialog(
    initialBlock: ScheduleBlockEntity?,
    defaultDayOfWeek: Int,
    subjects: List<SubjectEntity>,
    onSave: (ScheduleBlockEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initialBlock?.title ?: "") }
    var category by remember { mutableStateOf(BlockCategory.fromRaw(initialBlock?.category)) }
    var dayOfWeek by remember { mutableIntStateOf(initialBlock?.dayOfWeek ?: defaultDayOfWeek) }
    var startHour by remember { mutableIntStateOf((initialBlock?.startMinutes ?: 960) / 60) }
    var startMin by remember { mutableIntStateOf((initialBlock?.startMinutes ?: 960) % 60) }
    var durationMin by remember { mutableIntStateOf(((initialBlock?.endMinutes ?: 1020) - (initialBlock?.startMinutes ?: 960)).coerceAtLeast(15)) }
    var fixed by remember { mutableStateOf(initialBlock?.fixed ?: true) }
    var selectedSubjectId by remember { mutableStateOf(initialBlock?.subjectId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialBlock == null) "إضافة موعد أو فترة راحة" else "تعديل الموعد") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("العنوان (مثال: درس فيزياء / راحة)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(
                        listOf(
                            BlockCategory.STUDY,
                            BlockCategory.PRIVATE_LESSON,
                            BlockCategory.BREAK,
                            BlockCategory.SCHOOL,
                            BlockCategory.PERSONAL
                        )
                    ) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.arabicLabel) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("البداية: ${formatMinutesArabic(startHour * 60 + startMin)}")
                    Row {
                        IconButton(onClick = { startHour = (startHour - 1).coerceAtLeast(0) }) {
                            Icon(Icons.Filled.Remove, contentDescription = "نقص ساعة")
                        }
                        IconButton(onClick = { startHour = (startHour + 1).coerceAtMost(23) }) {
                            Icon(Icons.Filled.Add, contentDescription = "زيادة ساعة")
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("المدة: $durationMin دقيقة")
                    Row {
                        IconButton(onClick = { durationMin = (durationMin - 15).coerceAtLeast(15) }) {
                            Icon(Icons.Filled.Remove, contentDescription = "نقص مدة")
                        }
                        IconButton(onClick = { durationMin = (durationMin + 15).coerceAtMost(240) }) {
                            Icon(Icons.Filled.Add, contentDescription = "زيادة مدة")
                        }
                    }
                }
                if (subjects.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(subjects) { s ->
                            FilterChip(
                                selected = selectedSubjectId == s.id,
                                onClick = { selectedSubjectId = if (selectedSubjectId == s.id) null else s.id },
                                label = { Text(s.name) }
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = fixed, onCheckedChange = { fixed = it })
                    Text("تثبيت الموعد (لا يتغير عند التوليد التلقائي)")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val startTotal = (startHour * 60 + startMin).coerceIn(0, 1420)
                    val endTotal = (startTotal + durationMin).coerceAtMost(1440)
                    onSave(
                        (initialBlock ?: ScheduleBlockEntity(
                            dayOfWeek = dayOfWeek,
                            title = title.ifBlank { category.arabicLabel },
                            startMinutes = startTotal,
                            endMinutes = endTotal
                        )).copy(
                            dayOfWeek = dayOfWeek,
                            title = title.ifBlank { category.arabicLabel },
                            category = category.name,
                            subjectId = selectedSubjectId,
                            startMinutes = startTotal,
                            endMinutes = endTotal,
                            fixed = fixed
                        )
                    )
                }
            ) { Text("حفظ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
private fun HolidaysManagerDialog(
    holidays: List<HolidayOrBreakEntity>,
    onSaveHoliday: (HolidayOrBreakEntity) -> Unit,
    onDeleteHoliday: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var daysFromToday by remember { mutableIntStateOf(0) }
    var kind by remember { mutableStateOf(HolidayKind.HOLIDAY) }
    var blockAllStudy by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("الإجازات وأيام الراحة والمناسبات") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("اسم الإجازة / المناسبة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val targetDate = LocalDate.now().plusDays(daysFromToday.toLong())
                    Text("التاريخ: $targetDate")
                    Row {
                        IconButton(onClick = { daysFromToday = (daysFromToday - 1).coerceAtLeast(0) }) {
                            Icon(Icons.Filled.Remove, contentDescription = "يوم سابق")
                        }
                        IconButton(onClick = { daysFromToday++ }) {
                            Icon(Icons.Filled.Add, contentDescription = "يوم تالي")
                        }
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(HolidayKind.entries) { k ->
                        FilterChip(
                            selected = kind == k,
                            onClick = { kind = k },
                            label = { Text(k.arabicLabel) }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = blockAllStudy, onCheckedChange = { blockAllStudy = it })
                    Text("إيقاف جدولة المذاكرة طوال هذا اليوم")
                }
                if (holidays.isNotEmpty()) {
                    HorizontalDivider()
                    holidays.forEach { h ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${h.title} (${LocalDate.ofEpochDay(h.dateEpochDay)})",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { onDeleteHoliday(h.id) }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف", tint = CrimsonUrgent)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank(),
                onClick = {
                    onSaveHoliday(
                        HolidayOrBreakEntity(
                            dateEpochDay = LocalDate.now().plusDays(daysFromToday.toLong()).toEpochDay(),
                            title = title.trim(),
                            kind = kind.name,
                            blockAllStudy = blockAllStudy
                        )
                    )
                    title = ""
                }
            ) { Text("إضافة إجازة") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        }
    )
}
