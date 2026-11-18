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
import com.example.data.local.AppNotificationEntity
import com.example.data.local.NotificationCategory
import com.example.ui.components.CircleIconBadge
import com.example.ui.components.GlassCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonUrgent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SkyPrimary
import com.example.ui.viewmodel.SearchResults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchSheet(
    onSearch: (String) -> SearchResults,
    onNavigateToRoute: (String) -> Unit,
    onOpenSubject: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val results = remember(query) { onSearch(query) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("البحث الشامل في مُنظِّم", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("ابحث عن مادة، درس، مهمة، ملاحظة، امتحان أو ملف...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("global_search_input")
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (query.isBlank()) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "اكتب أي كلمة للبحث في المواد، الدروس، المهام، الملاحظات، الامتحانات، والملفات المرفقة.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    val emptyAll = results.subjects.isEmpty() && results.lessons.isEmpty() &&
                        results.notes.isEmpty() && results.exams.isEmpty() && results.attachments.isEmpty()
                    if (emptyAll) {
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Text("لا توجد نتائج مطابقة لـ \"$query\".", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    if (results.subjects.isNotEmpty()) {
                        item { Text("المواد (${results.subjects.size})", style = MaterialTheme.typography.titleSmall, color = SkyPrimary) }
                        items(results.subjects) { subj ->
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    onDismiss()
                                    onOpenSubject(subj.id)
                                }
                            ) {
                                Text(subj.name, style = MaterialTheme.typography.titleMedium)
                                if (subj.teacherName.isNotBlank()) {
                                    Text("المعلم: ${subj.teacherName}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    if (results.lessons.isNotEmpty()) {
                        item { Text("الدروس والمهام (${results.lessons.size})", style = MaterialTheme.typography.titleSmall, color = EmeraldSuccess) }
                        items(results.lessons) { les ->
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    onDismiss()
                                    onOpenSubject(les.subjectId)
                                }
                            ) {
                                Text(les.title, style = MaterialTheme.typography.titleSmall)
                                if (les.unitName.isNotBlank()) {
                                    Text("${les.unitName} ${les.chapterName}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    if (results.exams.isNotEmpty()) {
                        item { Text("الامتحانات (${results.exams.size})", style = MaterialTheme.typography.titleSmall, color = AmberWarning) }
                        items(results.exams) { ex ->
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    onDismiss()
                                    onNavigateToRoute("exams")
                                }
                            ) {
                                Text(ex.title, style = MaterialTheme.typography.titleSmall)
                                Text(ex.requiredParts, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    if (results.notes.isNotEmpty()) {
                        item { Text("الملاحظات والملفات (${results.notes.size})", style = MaterialTheme.typography.titleSmall, color = SkyPrimary) }
                        items(results.notes) { note ->
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    onDismiss()
                                    onNavigateToRoute("library")
                                }
                            ) {
                                Text(note.title, style = MaterialTheme.typography.titleSmall)
                                Text(note.content.take(80), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterSheet(
    notifications: List<AppNotificationEntity>,
    onMarkRead: (String) -> Unit,
    onMarkAllRead: () -> Unit,
    onNavigate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("مركز التنبيهات والتذكيرات", style = MaterialTheme.typography.titleLarge)
                if (notifications.any { !it.read }) {
                    TextButton(onClick = onMarkAllRead) {
                        Text("تحديد الكل كمقروء")
                    }
                }
            }

            if (notifications.isEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "لا توجد تنبيهات جديدة حالياً. ستظهر هنا تذكيرات الدروس والامتحانات والمراجعات المتباعدة.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notifications, key = { it.id }) { ntf ->
                        val cat = NotificationCategory.fromRaw(ntf.category)
                        val tint = when (cat) {
                            NotificationCategory.EXAM -> CrimsonUrgent
                            NotificationCategory.BACKLOG -> AmberWarning
                            NotificationCategory.REVIEW -> SkyPrimary
                            else -> EmeraldSuccess
                        }
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = if (!ntf.read) tint.copy(alpha = 0.6f) else null,
                            onClick = {
                                onMarkRead(ntf.id)
                                onDismiss()
                                onNavigate(ntf.relatedRoute)
                            }
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                CircleIconBadge(
                                    icon = when (cat) {
                                        NotificationCategory.EXAM -> Icons.Filled.EventNote
                                        NotificationCategory.REVIEW -> Icons.Filled.Replay
                                        NotificationCategory.BACKLOG -> Icons.Filled.Warning
                                        else -> Icons.Filled.NotificationsActive
                                    },
                                    tint = tint
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(ntf.title, style = MaterialTheme.typography.titleSmall)
                                        if (!ntf.read) {
                                            StatusPill("جديد", tint)
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(ntf.body, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
