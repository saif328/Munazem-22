package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ai.ExtractedFixedLesson
import com.example.ui.components.GlassCard
import com.example.ui.components.PrimaryActionButton
import com.example.ui.components.StatusPill
import com.example.ui.components.arabicDayName
import com.example.ui.components.formatMinutesArabic
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonUrgent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SkyPrimary
import com.example.ui.viewmodel.AiDayParseState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiDayParserSheet(
    parseState: AiDayParseState,
    onAnalyze: (String) -> Unit,
    onConfirmAndBuild: (
        wakeMinutes: Int?,
        sleepMinutes: Int?,
        schoolStartMinutes: Int?,
        schoolEndMinutes: Int?,
        lessons: List<ExtractedFixedLesson>
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var descriptionText by remember { mutableStateOf("") }

    // Editable review states populated when AI returns Success
    var editableWake by remember { mutableStateOf<Int?>(null) }
    var editableSleep by remember { mutableStateOf<Int?>(null) }
    var editableSchoolStart by remember { mutableStateOf<Int?>(null) }
    var editableSchoolEnd by remember { mutableStateOf<Int?>(null) }
    val editableLessons = remember { mutableStateListOf<ExtractedFixedLesson>() }

    LaunchedEffect(parseState) {
        if (parseState is AiDayParseState.Success) {
            editableWake = parseState.parsed.wakeTimeMinutes
            editableSleep = parseState.parsed.sleepTimeMinutes
            editableSchoolStart = parseState.parsed.schoolStartMinutes
            editableSchoolEnd = parseState.parsed.schoolEndMinutes
            editableLessons.clear()
            editableLessons.addAll(parseState.parsed.fixedLessons)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Filled.Psychology, contentDescription = null, tint = SkyPrimary, modifier = Modifier.size(28.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("وصف يومك بالذكاء الاصطناعي", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "اكتب يومك باللهجة المصرية أو الفصحى وسيستخرج المساعد مواعيدك فقط دون اختراع أي مواعيد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedTextField(
                value = descriptionText,
                onValueChange = { descriptionText = it },
                label = { Text("صف يومك ودروسك ومواعيد نومك...") },
                placeholder = {
                    Text("مثال: أنا بصحى الساعة 6 الصبح، بروح المدرسة 7 وبرجع 2، يوم الأحد عندي درس رياضة الساعة 5، والثلاثاء درس عربي الساعة 7، وبنام الساعة 11.")
                },
                minLines = 4,
                maxLines = 7,
                modifier = Modifier.fillMaxWidth().testTag("ai_day_description_input")
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PrimaryActionButton(
                    text = if (parseState is AiDayParseState.Loading) "جارٍ التحليل..." else "تحليل يومي الآن",
                    icon = Icons.Filled.AutoAwesome,
                    enabled = parseState !is AiDayParseState.Loading && descriptionText.isNotBlank(),
                    testTag = "ai_analyze_day_button",
                    modifier = Modifier.weight(1f),
                    onClick = { onAnalyze(descriptionText) }
                )
            }

            when (parseState) {
                is AiDayParseState.Loading -> {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.5.dp)
                            Text("يقوم MUNAZZEM AI بتحليل مواعيدك واكتشاف أوقات الفراغ والتعارضات...")
                        }
                    }
                }
                is AiDayParseState.Error -> {
                    Surface(
                        color = CrimsonUrgent.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, CrimsonUrgent.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = CrimsonUrgent)
                            Text(parseState.message, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                is AiDayParseState.Success -> {
                    val parsed = parseState.parsed
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "البيانات المستخرجة للمراجعة والتعديل",
                            style = MaterialTheme.typography.titleMedium,
                            color = EmeraldSuccess
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(parsed.summaryArabic, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(10.dp))

                        // Editable extracted routine times
                        TimeAdjustRow(
                            label = "الاستيقاظ",
                            minutes = editableWake,
                            onAdjust = { delta ->
                                editableWake = ((editableWake ?: 360) + delta).coerceIn(0, 1439)
                            }
                        )
                        TimeAdjustRow(
                            label = "النوم",
                            minutes = editableSleep,
                            onAdjust = { delta ->
                                editableSleep = ((editableSleep ?: 1380) + delta).coerceIn(0, 1439)
                            }
                        )
                        TimeAdjustRow(
                            label = "بداية المدرسة",
                            minutes = editableSchoolStart,
                            onAdjust = { delta ->
                                editableSchoolStart = ((editableSchoolStart ?: 450) + delta).coerceIn(0, 1439)
                            }
                        )
                        TimeAdjustRow(
                            label = "نهاية المدرسة",
                            minutes = editableSchoolEnd,
                            onAdjust = { delta ->
                                editableSchoolEnd = ((editableSchoolEnd ?: 840) + delta).coerceIn(0, 1439)
                            }
                        )
                    }

                    if (editableLessons.isNotEmpty()) {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text("الدروس والحصص المستخرجة (${editableLessons.size})", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            editableLessons.forEachIndexed { idx, les ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(les.subjectName, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "${arabicDayName(les.dayOfWeek)} • ${formatMinutesArabic(les.startMinutes)} (${les.durationMinutes} د)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { editableLessons.removeAt(idx) }) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف الدرس", tint = CrimsonUrgent)
                                    }
                                }
                            }
                        }
                    }

                    if (parsed.conflicts.isNotEmpty()) {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = AmberWarning.copy(alpha = 0.6f)
                        ) {
                            StatusPill("تعارضات مكتشفة", AmberWarning, icon = Icons.Filled.Warning)
                            Spacer(Modifier.height(6.dp))
                            parsed.conflicts.forEach { c ->
                                Text("• $c", style = MaterialTheme.typography.bodySmall, color = AmberWarning)
                            }
                        }
                    }

                    if (parsed.freeSlotsSummary.isNotEmpty()) {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text("أوقات الفراغ المكتشفة", style = MaterialTheme.typography.titleSmall, color = SkyPrimary)
                            Spacer(Modifier.height(4.dp))
                            parsed.freeSlotsSummary.forEach { slot ->
                                Text("• $slot", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    if (parsed.suggestedStudyTips.isNotEmpty()) {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text("اقتراحات جدول المذاكرة", style = MaterialTheme.typography.titleSmall, color = EmeraldSuccess)
                            Spacer(Modifier.height(4.dp))
                            parsed.suggestedStudyTips.forEach { tip ->
                                Text("• $tip", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    PrimaryActionButton(
                        text = "موافقة وحفظ + بناء الجدول الذكي",
                        icon = Icons.Filled.CheckCircle,
                        testTag = "ai_confirm_day_button",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            onConfirmAndBuild(
                                editableWake,
                                editableSleep,
                                editableSchoolStart,
                                editableSchoolEnd,
                                editableLessons.toList()
                            )
                            onDismiss()
                        }
                    )
                }
                AiDayParseState.Idle -> Unit
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TimeAdjustRow(
    label: String,
    minutes: Int?,
    onAdjust: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilledTonalIconButton(
                onClick = { onAdjust(-30) },
                modifier = Modifier.size(34.dp)
            ) {
                Text("-30", style = MaterialTheme.typography.labelSmall)
            }
            Text(
                text = if (minutes != null) formatMinutesArabic(minutes) else "غير محدد",
                style = MaterialTheme.typography.labelLarge,
                color = if (minutes != null) SkyPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            FilledTonalIconButton(
                onClick = { onAdjust(30) },
                modifier = Modifier.size(34.dp)
            ) {
                Text("+30", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
