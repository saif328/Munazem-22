package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.SubjectEntity
import com.example.domain.SubjectAnalytics
import com.example.ui.components.*
import com.example.ui.theme.CrimsonUrgent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SubjectColors

@Composable
fun SubjectsScreen(
    subjectAnalytics: List<SubjectAnalytics>,
    onOpenSubject: (String) -> Unit,
    onSaveSubject: (SubjectEntity) -> Unit,
    onDeleteSubject: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSubject by remember { mutableStateOf<SubjectEntity?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "المواد الدراسية",
                subtitle = "إدارة الوحدات والفصول والدروس والواجبات والمراجعات لكل مادة",
                actionLabel = "+ مادة جديدة",
                onActionClick = {
                    editingSubject = null
                    showAddDialog = true
                }
            )
        }

        if (subjectAnalytics.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("لا توجد مواد مضافة بعد.", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "أضف موادك الدراسية لتتمكن من تقسيمها لوحدات ودروس ومتابعة ساعات مذاكرتك.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    PrimaryActionButton(
                        text = "إضافة مادة دراسية",
                        icon = Icons.Filled.Add,
                        testTag = "empty_add_subject_btn",
                        onClick = { showAddDialog = true }
                    )
                }
            }
        } else {
            items(subjectAnalytics, key = { it.subject.id }) { item ->
                val subj = item.subject
                val tint = parseColorHex(subj.colorHex)
                GlassCard(
                    modifier = Modifier.fillMaxWidth().testTag("subject_card_${subj.id}"),
                    borderColor = tint.copy(alpha = 0.45f),
                    onClick = { onOpenSubject(subj.id) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            CircleIconBadge(icon = Icons.Filled.MenuBook, tint = tint)
                            Column {
                                Text(subj.name, style = MaterialTheme.typography.titleLarge)
                                Text(
                                    if (subj.teacherName.isNotBlank()) "المعلم: ${subj.teacherName}" else "صعوبة ${subj.difficulty}/5 • أولوية ${subj.priority}/5",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row {
                            IconButton(onClick = {
                                editingSubject = subj
                                showAddDialog = true
                            }) {
                                Icon(Icons.Filled.Edit, contentDescription = "تعديل المادة")
                            }
                            IconButton(onClick = { onDeleteSubject(subj.id) }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف المادة", tint = CrimsonUrgent)
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { (item.completionPercent / 100f).coerceIn(0f, 1f) },
                        color = tint,
                        trackColor = tint.copy(alpha = 0.16f),
                        modifier = Modifier.fillMaxWidth().height(8.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "الإنجاز: ${item.completedLessons}/${item.totalLessons} (${item.completionPercent}%)",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldSuccess
                        )
                        Text(
                            "ساعات: ${String.format("%.1f", item.studyMinutes / 60f)}",
                            style = MaterialTheme.typography.labelMedium
                        )
                        if (item.backlogCount > 0) {
                            StatusPill("متراكم: ${item.backlogCount}", CrimsonUrgent)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        SubjectEditorDialog(
            initial = editingSubject,
            onSave = { subj ->
                onSaveSubject(subj)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun SubjectEditorDialog(
    initial: SubjectEntity?,
    onSave: (SubjectEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var teacher by remember { mutableStateOf(initial?.teacherName ?: "") }
    var colorHex by remember { mutableStateOf(initial?.colorHex ?: SubjectColors.first()) }
    var difficulty by remember { mutableIntStateOf(initial?.difficulty ?: 3) }
    var priority by remember { mutableIntStateOf(initial?.priority ?: 3) }
    var notesSummary by remember { mutableStateOf(initial?.notesSummary ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "إضافة مادة دراسية" else "تعديل المادة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم المادة *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    label = { Text("اسم المعلم (اختياري)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("لون المادة المميز", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(SubjectColors) { hex ->
                        val c = parseColorHex(hex)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (colorHex == hex) 3.dp else 0.dp,
                                    color = if (colorHex == hex) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { colorHex = hex }
                        )
                    }
                }
                Text("مستوى الصعوبة: $difficulty / 5", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = difficulty.toFloat(),
                    onValueChange = { difficulty = it.toInt() },
                    valueRange = 1f..5f,
                    steps = 3
                )
                Text("الأولوية: $priority / 5", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = priority.toFloat(),
                    onValueChange = { priority = it.toInt() },
                    valueRange = 1f..5f,
                    steps = 3
                )
                OutlinedTextField(
                    value = notesSummary,
                    onValueChange = { notesSummary = it },
                    label = { Text("ملاحظات عامة حول المادة") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        (initial ?: SubjectEntity(name = name.trim())).copy(
                            name = name.trim(),
                            teacherName = teacher.trim(),
                            colorHex = colorHex,
                            difficulty = difficulty,
                            priority = priority,
                            notesSummary = notesSummary.trim()
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
