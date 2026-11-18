package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.*
import com.example.domain.AnalyticsEngine
import com.example.domain.SubjectAnalytics
import com.example.ui.components.*
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonUrgent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SkyPrimary
import com.example.ui.viewmodel.AiBacklogState
import java.time.LocalDate

@Composable
fun BacklogScreen(
    subjects: List<SubjectEntity>,
    lessons: List<LessonEntity>,
    subjectAnalytics: List<SubjectAnalytics>,
    aiBacklogState: AiBacklogState,
    onGenerateAiPlan: () -> Unit,
    onRedistributeAcrossDays: () -> Unit,
    onSplitLesson: (LessonEntity, Int) -> Unit,
    onUpdateBacklogDetails: (LessonEntity, String, Int, Long, LessonStatus) -> Unit,
    onCompleteQuick: (LessonEntity, Int, Int, Int, Int, String) -> Unit
) {
    val today = LocalDate.now().toEpochDay()
    val subjectMap = subjects.associateBy { it.id }

    val backlogLessons = remember(lessons, today) {
        lessons.filter { AnalyticsEngine.isBacklogLesson(it, today) }
            .sortedByDescending { it.difficultyOrImportance }
    }

    var editingLesson by remember { mutableStateOf<LessonEntity?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "المتراكم الذكي",
                subtitle = "معالجة الدروس المتأخرة والواجبات والمراجعات بدون إرهاق الطالب"
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PrimaryActionButton(
                    text = "خطة AI للمتراكم",
                    icon = Icons.Filled.AutoAwesome,
                    testTag = "ai_backlog_plan_btn",
                    modifier = Modifier.weight(1f),
                    onClick = onGenerateAiPlan
                )
                FilledTonalButton(
                    onClick = onRedistributeAcrossDays,
                    modifier = Modifier.height(50.dp)
                ) {
                    Icon(Icons.Filled.CalendarMonth, null)
                    Spacer(Modifier.width(4.dp))
                    Text("توزيع تلقائي")
                }
            }
        }

        // AI Backlog Recovery Plan Card
        when (aiBacklogState) {
            is AiBacklogState.Loading -> {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                            Text("يقوم MUNAZZEM AI بإعداد خطة مريحة لتصفية المتراكم...")
                        }
                    }
                }
            }
            is AiBacklogState.Success -> {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = SkyPrimary.copy(alpha = 0.5f)) {
                        StatusPill("خطة التخلص من المتراكم (بدون إرهاق)", SkyPrimary, icon = Icons.Filled.Psychology)
                        Spacer(Modifier.height(8.dp))
                        Text(aiBacklogState.planText, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            is AiBacklogState.Error -> {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = CrimsonUrgent.copy(alpha = 0.5f)) {
                        Text(aiBacklogState.message, color = CrimsonUrgent, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            AiBacklogState.Idle -> Unit
        }

        // Subject Backlog Indicator Gauge
        if (subjectAnalytics.isNotEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("مؤشر التراكم حسب المادة", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    subjectAnalytics.sortedByDescending { it.backlogCount }.forEach { stat ->
                        val levelColor = when {
                            stat.backlogCount >= 4 -> CrimsonUrgent
                            stat.backlogCount in 1..3 -> AmberWarning
                            else -> EmeraldSuccess
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stat.subject.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            LinearProgressIndicator(
                                progress = { (stat.backlogCount / 8f).coerceIn(0f, 1f) },
                                color = levelColor,
                                trackColor = levelColor.copy(alpha = 0.15f),
                                modifier = Modifier.width(100.dp).height(6.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("${stat.backlogCount}", style = MaterialTheme.typography.labelLarge, color = levelColor)
                        }
                    }
                }
            }
        }

        if (backlogLessons.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = EmeraldSuccess.copy(alpha = 0.5f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircleIconBadge(Icons.Filled.Verified, tint = EmeraldSuccess)
                        Column {
                            Text("سجلك خالٍ من المتراكم!", style = MaterialTheme.typography.titleMedium, color = EmeraldSuccess)
                            Text("كل دروسك وواجباتك محدثة. واصل هذا التوازن الرائع.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        } else {
            items(backlogLessons, key = { it.id }) { les ->
                val subj = subjectMap[les.subjectId]
                val status = LessonStatus.fromRaw(les.status)
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = CrimsonUrgent.copy(alpha = 0.45f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${subj?.name ?: "مادة"}: ${les.title}", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "الحالة: ${status.arabicLabel} • الأهمية: ${les.difficultyOrImportance}/5 • المدة: ${les.estimatedMinutes} د",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (les.delayReason.isNotBlank()) {
                                Text("سبب التأخير: ${les.delayReason}", style = MaterialTheme.typography.labelSmall, color = AmberWarning)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { onSplitLesson(les, 2) }) {
                            Icon(Icons.Filled.CallSplit, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("تقسيم لجزأين")
                        }
                        OutlinedButton(onClick = { editingLesson = les }) {
                            Text("تعديل / جدولة")
                        }
                        FilledTonalButton(onClick = {
                            onCompleteQuick(les, les.estimatedMinutes, 0, 0, 80, "تم إنجازه من قسم المتراكم")
                        }) {
                            Icon(Icons.Filled.Check, null, Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }

    editingLesson?.let { target ->
        var reason by remember { mutableStateOf(target.delayReason) }
        var importance by remember { mutableIntStateOf(target.difficultyOrImportance) }
        var dueDaysAhead by remember { mutableIntStateOf(2) }
        var newStatus by remember { mutableStateOf(LessonStatus.fromRaw(target.status)) }

        AlertDialog(
            onDismissRequest = { editingLesson = null },
            title = { Text("معالجة المهمة المتراكمة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("سبب التأخير (اختياري)") },
                        singleLine = true
                    )
                    Text("أهمية المهمة: $importance / 5")
                    Slider(value = importance.toFloat(), onValueChange = { importance = it.toInt() }, valueRange = 1f..5f, steps = 3)
                    Text("إعادة الجدولة بعد: $dueDaysAhead يوم")
                    Slider(value = dueDaysAhead.toFloat(), onValueChange = { dueDaysAhead = it.toInt() }, valueRange = 0f..14f)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(LessonStatus.IN_PROGRESS, LessonStatus.NEEDS_REVIEW, LessonStatus.POSTPONED).forEach { st ->
                            FilterChip(selected = newStatus == st, onClick = { newStatus = st }, label = { Text(st.arabicLabel) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateBacklogDetails(target, reason, importance, today + dueDaysAhead, newStatus)
                    editingLesson = null
                }) { Text("حفظ") }
            },
            dismissButton = { TextButton(onClick = { editingLesson = null }) { Text("إلغاء") } }
        )
    }
}
