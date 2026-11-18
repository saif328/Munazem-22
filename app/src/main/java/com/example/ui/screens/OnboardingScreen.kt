package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import coil.compose.AsyncImage
import com.example.ui.auth.onGoogleSignInClicked
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.PrimaryActionButton
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SkyPrimary
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    onComplete: (
        name: String,
        stage: String,
        grade: String,
        school: String,
        governorate: String,
        goal: String,
        avatarUri: String,
        subjects: List<String>,
        enableCloudSync: Boolean
    ) -> Unit,
    onRestoreCloudAfterLogin: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var name by remember { mutableStateOf("") }
    var stage by remember { mutableStateOf("الثانوية") }
    var grade by remember { mutableStateOf("") }
    var school by remember { mutableStateOf("") }
    var governorate by remember { mutableStateOf("") }
    var studyGoal by remember { mutableStateOf("") }
    var avatarUri by remember { mutableStateOf("") }

    var customSubject by remember { mutableStateOf("") }
    val selectedSubjects = remember { mutableStateListOf<String>() }
    val suggestedSubjects = listOf(
        "اللغة العربية", "الرياضيات", "اللغة الإنجليزية",
        "الفيزياء", "الكيمياء", "الأحياء", "التاريخ", "الجغرافيا"
    )

    var enableCloudSync by remember { mutableStateOf(false) }
    var googleUserEmail by remember { mutableStateOf(Firebase.auth.currentUser?.email) }
    var authLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            avatarUri = uri.toString()
        }
    }

    val stages = listOf("الابتدائية", "الإعدادية", "الثانوية", "الجامعية")

    GlassBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Branded Header Banner
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                contentPadding = PaddingValues(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(72.dp)) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(SkyPrimary.copy(alpha = 0.45f), Color.Transparent)
                            ),
                            radius = size.minDimension * 0.65f,
                            center = Offset(size.width / 2f, size.height / 2f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = "شعار منظم",
                        tint = SkyPrimary,
                        modifier = Modifier.size(38.dp)
                    )
                }
                Text(
                    text = "MUNAZZEM STUDY PRO",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "مُنظِّم — مساعد الطالب الذكي",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "«وقتك منظم، ومستقبلك أقرب.»",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Student Profile Info
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "بيانات الطالب الأساسية",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(12.dp))

                // Optional Avatar Picker
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                            .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .clickable {
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("onboarding_avatar_picker"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarUri.isNotBlank()) {
                            AsyncImage(
                                model = avatarUri,
                                contentDescription = "الصورة الشخصية",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.AddAPhoto,
                                contentDescription = "اختيار صورة شخصية",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "صورة شخصية (اختياري)",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "اضغط لاختيار صورة من جهازك عبر منتقي الصور الآمن",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("الاسم الكامل *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("onboarding_name_input")
                )
                Spacer(Modifier.height(10.dp))

                Text("المرحلة الدراسية", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    stages.forEach { st ->
                        FilterChip(
                            selected = stage == st,
                            onClick = { stage = st },
                            label = { Text(st) }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = grade,
                    onValueChange = { grade = it },
                    label = { Text("الصف الدراسي (مثال: الثالث الثانوي)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("onboarding_grade_input")
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = school,
                    onValueChange = { school = it },
                    label = { Text("المدرسة / الجامعة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = governorate,
                    onValueChange = { governorate = it },
                    label = { Text("المحافظة (اختياري)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = studyGoal,
                    onValueChange = { studyGoal = it },
                    label = { Text("الهدف الدراسي (مثال: التفوق وتحقيق 98%)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Subjects Selection
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "موادك الدراسية",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "اختر موادك أو أضف أي مادة مخصصة (يمكنك تعديلها لاحقاً)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    suggestedSubjects.forEach { subj ->
                        val isSelected = selectedSubjects.contains(subj)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) selectedSubjects.remove(subj)
                                else selectedSubjects.add(subj)
                            },
                            label = { Text(subj) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                    selectedSubjects.filter { it !in suggestedSubjects }.forEach { custom ->
                        FilterChip(
                            selected = true,
                            onClick = { selectedSubjects.remove(custom) },
                            label = { Text(custom) },
                            trailingIcon = { Icon(Icons.Filled.Close, null, Modifier.size(16.dp)) }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customSubject,
                        onValueChange = { customSubject = it },
                        label = { Text("أضف مادة أخرى") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalButton(
                        onClick = {
                            val clean = customSubject.trim()
                            if (clean.isNotBlank() && !selectedSubjects.contains(clean)) {
                                selectedSubjects.add(clean)
                                customSubject = ""
                            }
                        },
                        modifier = Modifier.height(52.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "إضافة")
                    }
                }
            }

            // Optional Cloud Sync & Google Sign-In
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Filled.CloudSync, contentDescription = null, tint = SkyPrimary)
                    Text(
                        text = "المزامنة السحابية والنسخ الاحتياطي (اختياري)",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "يعمل التطبيق بالكامل بدون إنترنت على جهازك. يمكنك اختيارياً ربط حساب Google لحفظ نسخة مشفرة ومزامنة بياناتك بموافقتك.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))

                if (googleUserEmail != null) {
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
                                Text("متصل بحساب Google", style = MaterialTheme.typography.labelLarge, color = EmeraldSuccess)
                                Text(googleUserEmail ?: "", style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = { onRestoreCloudAfterLogin() }) {
                                Text("استعادة")
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            authLoading = true
                            errorMessage = null
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    authLoading = false
                                    googleUserEmail = Firebase.auth.currentUser?.email
                                    enableCloudSync = true
                                },
                                onAuthError = { msg ->
                                    authLoading = false
                                    errorMessage = msg
                                },
                                scope = scope,
                                onAuthCancelled = { authLoading = false }
                            )
                        },
                        enabled = !authLoading,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("onboarding_google_signin_button")
                    ) {
                        if (authLoading) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.AccountCircle, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("تسجيل الدخول بحساب Google للمزامنة")
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = enableCloudSync,
                        onCheckedChange = { checked ->
                            if (checked && Firebase.auth.currentUser == null) {
                                errorMessage = "يرجى تسجيل الدخول بحساب Google أولاً لتفعيل المزامنة السحابية."
                            } else {
                                enableCloudSync = checked
                            }
                        }
                    )
                    Text(
                        text = "أوافق على حفظ ومزامنة بياناتي الدراسية سحابياً بأمان",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            PrimaryActionButton(
                text = "ابدأ رحلتك المنظمة الآن",
                icon = Icons.Filled.RocketLaunch,
                testTag = "onboarding_submit_button",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "يرجى كتابة اسمك أولاً للبدء."
                    } else {
                        errorMessage = null
                        onComplete(
                            name,
                            stage,
                            grade,
                            school,
                            governorate,
                            studyGoal.ifBlank { "الالتزام والتفوق الدراسي" },
                            avatarUri,
                            selectedSubjects.toList(),
                            enableCloudSync
                        )
                    }
                }
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
