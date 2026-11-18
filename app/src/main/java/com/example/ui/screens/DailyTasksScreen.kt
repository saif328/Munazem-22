package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.cloud.DailyTask
import com.example.data.cloud.DailyTaskPriority
import com.example.data.local.SubjectEntity
import com.example.ui.auth.onGoogleSignInClicked
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.DailyTasksUiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class DailyTaskFilter(val label: String) {
    ALL("الكل"),
    PENDING("المتبقية"),
    COMPLETED("المكتملة")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DailyTasksScreen(
    uiState: DailyTasksUiState,
    subjects: List<SubjectEntity>,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onAddTask: (
        title: String,
        description: String,
        subjectId: String?,
        subjectName: String,
        scheduledStartMinutes: Long?,
        estimatedMinutes: Long,
        priority: Long
    ) -> Unit,
    onUpdateTask: (DailyTask) -> Unit,
    onToggleTask: (DailyTask) -> Unit,
    onDeleteTask: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var authLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    var selectedFilter by remember { mutableStateOf(DailyTaskFilter.ALL) }
    var showTaskDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<DailyTask?>(null) }

    val todayFormatted = remember {
        LocalDate.now().format(
            DateTimeFormatter.ofPattern("EEEE، d MMMM yyyy", Locale("ar"))
        )
    }

    val currentUser = Firebase.auth.currentUser

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("daily_tasks_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع"
                            )
                        }
                        Column {
                            Text(
                                text = "مهام اليوم",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = todayFormatted,
                                style = MaterialTheme.typography.bodySmall,
                                color = SkyPrimary
                            )
                        }
                    }

                    StatusPill(
                        text = if (uiState is DailyTasksUiState.Success) "مزامنة فورية" else "Firestore",
                        color = if (uiState is DailyTasksUiState.Success) EmeraldSuccess else SkyPrimary,
                        icon = Icons.Filled.CloudDone
                    )
                }
            }

            when (uiState) {
                is DailyTasksUiState.SignedOut -> {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 24.dp,
                            borderColor = SkyPrimary.copy(alpha = 0.45f)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircleIconBadge(
                                    icon = Icons.Filled.Checklist,
                                    tint = SkyPrimary,
                                    size = 64.dp
                                )
                                Text(
                                    text = "سجّل الدخول لعرض مهام اليوم",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = "تُحفظ مهامك اليومية بأمان في Cloud Firestore وتُزامَن لحظياً مع حسابك، لتبقى متاحة أينما كنت.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!authError.isNullOrBlank()) {
                                    Text(
                                        text = authError!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CrimsonUrgent
                                    )
                                }
                                Button(
                                    onClick = {
                                        authError = null
                                        authLoading = true
                                        onGoogleSignInClicked(
                                            context = context,
                                            credentialManager = credentialManager,
                                            onAuthSuccess = {
                                                authLoading = false
                                                onRetry()
                                            },
                                            onAuthError = { msg ->
                                                authLoading = false
                                                authError = msg
                                            },
                                            scope = scope,
                                            onAuthCancelled = { authLoading = false }
                                        )
                                    },
                                    enabled = !authLoading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("daily_tasks_google_signin_btn"),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    if (authLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(Icons.Filled.Login, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("تسجيل الدخول بحساب Google")
                                    }
                                }
                            }
                        }
                    }
                }

                is DailyTasksUiState.Loading -> {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(color = SkyPrimary)
                                Text(
                                    text = "جاري تحميل مهام اليوم من Firestore...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                is DailyTasksUiState.Error -> {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = CrimsonUrgent.copy(alpha = 0.5f)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircleIconBadge(icon = Icons.Filled.ErrorOutline, tint = CrimsonUrgent)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("تعذر جلب المهام", style = MaterialTheme.typography.titleMedium, color = CrimsonUrgent)
                                        Text(uiState.message, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                OutlinedButton(
                                    onClick = onRetry,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("daily_tasks_retry_btn")
                                ) {
                                    Icon(Icons.Filled.Refresh, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("إعادة المحاولة")
                                }
                            }
                        }
                    }
                }

                is DailyTasksUiState.Success -> {
                    val allTasks = uiState.tasks
                    val completedCount = allTasks.count { it.completed }
                    val pendingMinutes = allTasks.filter { !it.completed }.sumOf { it.estimatedMinutes.toInt() }
                    val progress = if (allTasks.isEmpty()) 0f else completedCount.toFloat() / allTasks.size.toFloat()

                    val filteredTasks = when (selectedFilter) {
                        DailyTaskFilter.ALL -> allTasks
                        DailyTaskFilter.PENDING -> allTasks.filter { !it.completed }
                        DailyTaskFilter.COMPLETED -> allTasks.filter { it.completed }
                    }

                    // Daily Progress Card
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 22.dp,
                            borderColor = SkyPrimary.copy(alpha = 0.4f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "إنجاز اليوم",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = if (allTasks.isEmpty()) "لا توجد مهام مضافة لهذا اليوم"
                                        else "$completedCount من ${allTasks.size} مهمة مكتملة • متبقي ${formatDurationArabic(pendingMinutes)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (!currentUser?.email.isNullOrBlank()) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = "الحساب: ${currentUser?.email}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SkyPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                StatusPill(
                                    text = "${(progress * 100).toInt()}%",
                                    color = if (progress >= 1f && allTasks.isNotEmpty()) EmeraldSuccess else SkyPrimary,
                                    icon = Icons.Filled.TrendingUp
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                color = if (progress >= 1f && allTasks.isNotEmpty()) EmeraldSuccess else SkyPrimary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                            )
                        }
                    }

                    // Filter Chips
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(DailyTaskFilter.entries) { filter ->
                                val count = when (filter) {
                                    DailyTaskFilter.ALL -> allTasks.size
                                    DailyTaskFilter.PENDING -> allTasks.count { !it.completed }
                                    DailyTaskFilter.COMPLETED -> completedCount
                                }
                                FilterChip(
                                    selected = selectedFilter == filter,
                                    onClick = { selectedFilter = filter },
                                    label = { Text("${filter.label} ($count)") },
                                    modifier = Modifier.testTag("daily_task_filter_${filter.name.lowercase()}")
                                )
                            }
                        }
                    }

                    if (filteredTasks.isEmpty()) {
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircleIconBadge(
                                        icon = Icons.Filled.TaskAlt,
                                        tint = EmeraldSuccess,
                                        size = 54.dp
                                    )
                                    Text(
                                        text = when {
                                            allTasks.isEmpty() -> "يومك جاهز للتخطيط!"
                                            selectedFilter == DailyTaskFilter.PENDING -> "أنجزت جميع مهامك المتبقية لليوم!"
                                            else -> "لا توجد مهام مكتملة بعد"
                                        },
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = if (allTasks.isEmpty()) {
                                            "أضف مهامك الدراسية لليوم وسيتم حفظها ومزامنتها تلقائياً عبر Firestore."
                                        } else {
                                            "يمكنك تغيير التصفية أو إضافة مهمة جديدة لليوم."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (allTasks.isEmpty()) {
                                        PrimaryActionButton(
                                            text = "إضافة أول مهمة لليوم",
                                            icon = Icons.Filled.AddTask,
                                            testTag = "daily_tasks_empty_add_btn",
                                            onClick = {
                                                editingTask = null
                                                showTaskDialog = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        items(filteredTasks, key = { it.id }) { task ->
                            DailyTaskCard(
                                task = task,
                                onToggle = { onToggleTask(task) },
                                onEdit = {
                                    editingTask = task
                                    showTaskDialog = true
                                },
                                onDelete = { onDeleteTask(task.id) }
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = uiState is DailyTasksUiState.Success || uiState is DailyTasksUiState.Error,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            ExtendedFloatingActionButton(
                onClick = {
                    editingTask = null
                    showTaskDialog = true
                },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("مهمة لليوم") },
                containerColor = SkyPrimary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_daily_task_fab")
            )
        }
    }

    if (showTaskDialog) {
        DailyTaskEditorDialog(
            initialTask = editingTask,
            subjects = subjects,
            onDismiss = { showTaskDialog = false },
            onSave = { title, desc, subjId, subjName, startMins, estMins, prio ->
                val existing = editingTask
                if (existing == null) {
                    onAddTask(title, desc, subjId, subjName, startMins, estMins, prio)
                } else {
                    onUpdateTask(
                        existing.copy(
                            title = title,
                            description = desc,
                            subjectId = subjId,
                            subjectName = subjName,
                            scheduledStartMinutes = startMins,
                            estimatedMinutes = estMins,
                            priority = prio
                        )
                    )
                }
                showTaskDialog = false
            }
        )
    }
}

@Composable
private fun DailyTaskCard(
    task: DailyTask,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val prio = task.priorityEnum
    val prioColor = when (prio) {
        DailyTaskPriority.URGENT -> CrimsonUrgent
        DailyTaskPriority.HIGH -> AmberWarning
        DailyTaskPriority.MEDIUM -> SkyPrimary
        DailyTaskPriority.LOW -> EmeraldSuccess
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_task_card_${task.id}"),
        borderColor = if (task.completed) EmeraldSuccess.copy(alpha = 0.35f) else prioColor.copy(alpha = 0.45f),
        contentPadding = PaddingValues(12.dp),
        onClick = onToggle
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Checkbox(
                checked = task.completed,
                onCheckedChange = { onToggle() },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("daily_task_checkbox_${task.id}")
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (task.completed) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                if (task.description.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(
                        text = prio.arabicLabel,
                        color = prioColor
                    )
                    if (task.subjectName.isNotBlank()) {
                        StatusPill(
                            text = task.subjectName,
                            color = IndigoAccent,
                            icon = Icons.Filled.MenuBook
                        )
                    }
                    Text(
                        text = buildString {
                            if (task.scheduledStartMinutes != null) {
                                append(formatMinutesArabic(task.scheduledStartMinutes.toInt()))
                                append(" • ")
                            }
                            append(formatDurationArabic(task.estimatedMinutes.toInt()))
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("daily_task_edit_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "تعديل المهمة",
                        tint = SkyPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("daily_task_delete_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "حذف المهمة",
                        tint = CrimsonUrgent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DailyTaskEditorDialog(
    initialTask: DailyTask?,
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String,
        subjectId: String?,
        subjectName: String,
        scheduledStartMinutes: Long?,
        estimatedMinutes: Long,
        priority: Long
    ) -> Unit
) {
    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var description by remember { mutableStateOf(initialTask?.description ?: "") }
    var selectedSubjectId by remember { mutableStateOf(initialTask?.subjectId) }
    var selectedSubjectName by remember { mutableStateOf(initialTask?.subjectName ?: "") }
    var estimatedMinutes by remember { mutableStateOf(initialTask?.estimatedMinutes ?: 30L) }
    var selectedPriority by remember { mutableStateOf(initialTask?.priority ?: DailyTaskPriority.MEDIUM.level) }
    var hasScheduledTime by remember { mutableStateOf(initialTask?.scheduledStartMinutes != null) }
    var startHour by remember { mutableStateOf(((initialTask?.scheduledStartMinutes ?: 960L) / 60L).toInt()) }
    var startMinute by remember { mutableStateOf(((initialTask?.scheduledStartMinutes ?: 960L) % 60L).toInt()) }
    var titleError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialTask == null) "إضافة مهمة لليوم" else "تعديل المهمة")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (titleError != null) titleError = null
                    },
                    label = { Text("عنوان المهمة *") },
                    placeholder = { Text("مثال: حل 15 مسألة تفاضل") },
                    singleLine = true,
                    isError = titleError != null,
                    supportingText = titleError?.let { err -> { Text(err) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("daily_task_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("تفاصيل إضافية (اختياري)") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("daily_task_desc_input")
                )

                Text("الأولوية", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DailyTaskPriority.entries.forEach { p ->
                        FilterChip(
                            selected = selectedPriority == p.level,
                            onClick = { selectedPriority = p.level },
                            label = { Text(p.arabicLabel) }
                        )
                    }
                }

                Text("المدة التقديرية", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(15L, 30L, 45L, 60L, 90L).forEach { mins ->
                        FilterChip(
                            selected = estimatedMinutes == mins,
                            onClick = { estimatedMinutes = mins },
                            label = { Text("$mins د") }
                        )
                    }
                }

                if (subjects.isNotEmpty()) {
                    Text("المادة (اختياري)", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = selectedSubjectId == null,
                                onClick = {
                                    selectedSubjectId = null
                                    selectedSubjectName = ""
                                },
                                label = { Text("عام") }
                            )
                        }
                        items(subjects) { subj ->
                            FilterChip(
                                selected = selectedSubjectId == subj.id,
                                onClick = {
                                    selectedSubjectId = subj.id
                                    selectedSubjectName = subj.name
                                },
                                label = { Text(subj.name) }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تحديد وقت بدء", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = hasScheduledTime,
                        onCheckedChange = { hasScheduledTime = it }
                    )
                }

                if (hasScheduledTime) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { startHour = (startHour + 1) % 24 },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("الساعة: ${startHour.toString().padStart(2, '0')}")
                        }
                        OutlinedButton(
                            onClick = { startMinute = (startMinute + 15) % 60 },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("الدقيقة: ${startMinute.toString().padStart(2, '0')}")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = title.trim()
                    if (clean.isEmpty()) {
                        titleError = "يرجى كتابة عنوان للمهمة"
                        return@Button
                    }
                    val startMins = if (hasScheduledTime) {
                        (startHour * 60L + startMinute).coerceIn(0L, 1439L)
                    } else null
                    onSave(
                        clean,
                        description.trim(),
                        selectedSubjectId,
                        selectedSubjectName,
                        startMins,
                        estimatedMinutes,
                        selectedPriority
                    )
                },
                modifier = Modifier.testTag("save_daily_task_btn")
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
