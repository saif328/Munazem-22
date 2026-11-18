package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import coil.compose.AsyncImage
import com.example.data.local.StudentProfileEntity
import com.example.data.preferences.AppSettingsState
import com.example.domain.DashboardAnalytics
import com.example.domain.GamificationSnapshot
import com.example.notifications.ReminderScheduler
import com.example.ui.auth.onGoogleSignInClicked
import com.example.ui.auth.signOutGoogle
import com.example.ui.components.*
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonUrgent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SkyPrimary
import com.example.ui.viewmodel.SyncState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileAndSettingsScreen(
    profile: StudentProfileEntity?,
    analytics: DashboardAnalytics,
    gamification: GamificationSnapshot,
    settings: AppSettingsState,
    syncState: SyncState,
    onUpdateProfile: (StudentProfileEntity) -> Unit,
    onToggleTheme: (Boolean) -> Unit,
    onToggleCloudSyncConsent: (Boolean) -> Unit,
    onUpdateNotificationPrefs: (Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Int) -> Unit,
    onBackupToCloud: () -> Unit,
    onRestoreFromCloud: () -> Unit,
    onDeleteAllData: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var currentUserEmail by remember { mutableStateOf(Firebase.auth.currentUser?.email) }
    var authLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    var hasNotifPermission by remember { mutableStateOf(ReminderScheduler.hasNotificationPermission(context)) }
    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
    }

    val avatarPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && profile != null) {
            onUpdateProfile(profile.copy(avatarUri = uri.toString()))
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Student Profile Summary Card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(SkyPrimary.copy(alpha = 0.2f))
                            .clickable {
                                avatarPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!profile?.avatarUri.isNullOrBlank()) {
                            AsyncImage(
                                model = profile?.avatarUri,
                                contentDescription = "الصورة الشخصية",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = SkyPrimary, modifier = Modifier.size(32.dp))
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(profile?.name?.ifBlank { "طالب مُنظِّم" } ?: "طالب مُنظِّم", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "${profile?.educationalStage ?: ""} ${profile?.grade ?: ""} • ${profile?.school ?: ""}".trim(' ', '•'),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!profile?.governorate.isNullOrBlank()) {
                            Text("المحافظة: ${profile?.governorate}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    IconButton(onClick = { showEditProfileDialog = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "تعديل الملف")
                    }
                }
                if (!profile?.studyGoal.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    StatusPill("الهدف: ${profile?.studyGoal}", EmeraldSuccess, icon = Icons.Filled.Flag)
                }
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ProfileStat("أيام الالتزام", "${gamification.streakDays}")
                    ProfileStat("ساعات المذاكرة", String.format("%.1f", analytics.weeklyStudyHours))
                    ProfileStat("إنجاز المهام", "${analytics.completionRatePercent}%")
                    ProfileStat("المتراكم", "${analytics.backlogCount}")
                }
                val topSubjects = analytics.subjectAnalytics.take(2).map { it.subject.name }
                if (topSubjects.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "المواد الأكثر تقدماً: ${topSubjects.joinToString(" • ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SkyPrimary
                    )
                }
            }
        }

        // Gamification & Achievements
        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = AmberWarning.copy(alpha = 0.4f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("التحفيز والإنجازات", style = MaterialTheme.typography.titleLarge)
                        Text("المستوى ${gamification.level}: ${gamification.levelTitle} (${gamification.xp} XP)", style = MaterialTheme.typography.bodySmall, color = SkyPrimary)
                    }
                    StatusPill("${gamification.streakDays} يوم", AmberWarning, icon = Icons.Filled.LocalFireDepartment)
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { gamification.xpIntoCurrentLevel / gamification.xpForNextLevel.toFloat() },
                    color = SkyPrimary,
                    modifier = Modifier.fillMaxWidth().height(8.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text(gamification.gentleStreakMessage, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(10.dp))

                Text(gamification.dailyChallenge.title + " (${gamification.dailyChallenge.progressText})", style = MaterialTheme.typography.labelMedium)
                LinearProgressIndicator(
                    progress = { gamification.dailyChallenge.progressFraction },
                    color = EmeraldSuccess,
                    modifier = Modifier.fillMaxWidth().height(6.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text(gamification.weeklyChallenge.title + " (${gamification.weeklyChallenge.progressText})", style = MaterialTheme.typography.labelMedium)
                LinearProgressIndicator(
                    progress = { gamification.weeklyChallenge.progressFraction },
                    color = AmberWarning,
                    modifier = Modifier.fillMaxWidth().height(6.dp)
                )

                Spacer(Modifier.height(12.dp))
                Text("شارات الإنجاز", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    gamification.badges.forEach { badge ->
                        StatusPill(
                            text = badge.title,
                            color = if (badge.unlocked) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                            icon = if (badge.unlocked) Icons.Filled.EmojiEvents else Icons.Filled.Lock
                        )
                    }
                }
            }
        }

        // Theme & Notifications Settings
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("المظهر والتنبيهات", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("الوضع الليلي الزجاجي (Dark Glass)", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = settings.darkTheme, onCheckedChange = onToggleTheme)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تفعيل تنبيهات النظام", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = settings.notificationsEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPermission) {
                                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            onUpdateNotificationPrefs(
                                enabled,
                                settings.notifyLessons,
                                settings.notifyTasks,
                                settings.notifyReviews,
                                settings.notifyExams,
                                settings.notifyBacklog,
                                settings.reminderLeadMinutes
                            )
                        }
                    )
                }
                if (settings.notificationsEnabled) {
                    OutlinedButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPermission) {
                                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                ReminderScheduler.showImmediateNotification(
                                    context,
                                    "تذكير MUNAZZEM STUDY PRO",
                                    "الإشعارات تعمل بنجاح! وقتك منظم، ومستقبلك أقرب."
                                )
                            }
                        }
                    ) {
                        Icon(Icons.Filled.NotificationsActive, null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (hasNotifPermission) "اختبار إشعار النظام" else "السماح بالإشعارات")
                    }
                }
            }
        }

        // Cloud Sync, Security & Account Management
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("الأمان والمزامنة السحابية", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text(
                    "بياناتك محفوظة محلياً وتعمل دون إنترنت. المزامنة السحابية اختيارية ومعزولة تماماً بحسابك.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))

                if (currentUserEmail == null) {
                    PrimaryActionButton(
                        text = if (authLoading) "جارٍ الاتصال..." else "تسجيل الدخول بحساب Google",
                        icon = Icons.Filled.AccountCircle,
                        enabled = !authLoading,
                        testTag = "profile_google_signin_btn",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            authLoading = true
                            authError = null
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    authLoading = false
                                    currentUserEmail = Firebase.auth.currentUser?.email
                                },
                                onAuthError = { msg ->
                                    authLoading = false
                                    authError = msg
                                },
                                scope = scope,
                                onAuthCancelled = { authLoading = false }
                            )
                        }
                    )
                } else {
                    Surface(
                        color = EmeraldSuccess.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("مسجل الدخول بحساب Google", style = MaterialTheme.typography.labelLarge, color = EmeraldSuccess)
                                Text(currentUserEmail ?: "", style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = {
                                signOutGoogle(credentialManager, scope) {
                                    currentUserEmail = null
                                    onToggleCloudSyncConsent(false)
                                }
                            }) {
                                Text("خروج", color = CrimsonUrgent)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("مزامنة تلقائية بموافقتي", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = settings.cloudSyncConsent, onCheckedChange = onToggleCloudSyncConsent)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        FilledTonalButton(onClick = onBackupToCloud, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.CloudUpload, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("نسخ احتياطي")
                        }
                        FilledTonalButton(onClick = onRestoreFromCloud, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.CloudDownload, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("استعادة")
                        }
                    }
                }

                if (authError != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(authError ?: "", color = CrimsonUrgent, style = MaterialTheme.typography.bodySmall)
                }
                when (syncState) {
                    is SyncState.Syncing -> {
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    is SyncState.Message -> {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            syncState.text,
                            color = if (syncState.isError) CrimsonUrgent else EmeraldSuccess,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    SyncState.Idle -> Unit
                }

                Spacer(Modifier.height(14.dp))
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonUrgent),
                    border = BorderStroke(1.dp, CrimsonUrgent.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.DeleteForever, null)
                    Spacer(Modifier.width(6.dp))
                    Text("حذف الحساب وجميع البيانات")
                }
            }
        }
    }

    if (showEditProfileDialog && profile != null) {
        EditProfileDialog(
            profile = profile,
            onSave = {
                onUpdateProfile(it)
                showEditProfileDialog = false
            },
            onDismiss = { showEditProfileDialog = false }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("تأكيد حذف البيانات") },
            text = { Text("سيتم حذف جميع بياناتك وموادك وجدولك وملاحظاتك من الجهاز ومن السحابة (إن وجدت). هل أنت متأكد؟") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonUrgent),
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteAllData(true)
                    }
                ) { Text("نعم، احذف كل شيء") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("إلغاء") } }
        )
    }
}

@Composable
private fun ProfileStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = SkyPrimary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EditProfileDialog(
    profile: StudentProfileEntity,
    onSave: (StudentProfileEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var stage by remember { mutableStateOf(profile.educationalStage) }
    var grade by remember { mutableStateOf(profile.grade) }
    var school by remember { mutableStateOf(profile.school) }
    var gov by remember { mutableStateOf(profile.governorate) }
    var goal by remember { mutableStateOf(profile.studyGoal) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل الملف الشخصي") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("الاسم") }, singleLine = true)
                OutlinedTextField(value = stage, onValueChange = { stage = it }, label = { Text("المرحلة الدراسية") }, singleLine = true)
                OutlinedTextField(value = grade, onValueChange = { grade = it }, label = { Text("الصف") }, singleLine = true)
                OutlinedTextField(value = school, onValueChange = { school = it }, label = { Text("المدرسة") }, singleLine = true)
                OutlinedTextField(value = gov, onValueChange = { gov = it }, label = { Text("المحافظة") }, singleLine = true)
                OutlinedTextField(value = goal, onValueChange = { goal = it }, label = { Text("الهدف الدراسي") }, maxLines = 2)
            }
        },
        confirmButton = {
            Button(enabled = name.isNotBlank(), onClick = {
                onSave(
                    profile.copy(
                        name = name.trim(),
                        educationalStage = stage.trim(),
                        grade = grade.trim(),
                        school = school.trim(),
                        governorate = gov.trim(),
                        studyGoal = goal.trim()
                    )
                )
            }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}
