package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.AiChatMessageEntity
import com.example.ui.components.GlassCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.CrimsonUrgent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SkyPrimary
import com.example.ui.viewmodel.AiChatState

@Composable
fun AssistantScreen(
    messages: List<AiChatMessageEntity>,
    chatState: AiChatState,
    aiDataContextConsent: Boolean,
    onToggleDataConsent: (Boolean) -> Unit,
    onSendMessage: (String) -> Unit,
    onClearChat: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val quickPrompts = listOf(
        "نظم يومي",
        "اعمل لي جدول أسبوعي",
        "وزع المتراكم",
        "اعمل خطة لامتحان قريب",
        "إيه أهم حاجة أذاكرها النهارده؟",
        "حلل مستوايا",
        "اقترح طريقة مراجعة",
        "ساعدني ألتزم بجدولي"
    )

    LaunchedEffect(messages.size, chatState) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Header + Data Consent Switch
        GlassCard(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            contentPadding = PaddingValues(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.SmartToy, contentDescription = null, tint = SkyPrimary)
                    Column {
                        Text("MUNAZZEM AI", style = MaterialTheme.typography.titleMedium, color = SkyPrimary)
                        Text("مساعدك الدراسي الذكي", style = MaterialTheme.typography.labelSmall)
                    }
                }
                IconButton(onClick = onClearChat) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = "مسح المحادثة")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "السماح للمساعد بقراءة بياناتي (المواد، المهام، الامتحانات)",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = aiDataContextConsent,
                    onCheckedChange = onToggleDataConsent
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(quickPrompts) { prompt ->
                AssistChip(
                    onClick = { onSendMessage(prompt) },
                    label = { Text(prompt) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (messages.isEmpty() && chatState is AiChatState.Idle) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        StatusPill("جاهز لمساعدتك", EmeraldSuccess)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "أهلاً بك في MUNAZZEM AI! اسألني عن أي شيء يخص جدولك، المتراكم، خطط الامتحانات، أو تحليل مستواك، أو اختر أحد الاختصارات بالأعلى.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                val isUser = msg.role == "user"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.Start else Arrangement.End
                ) {
                    GlassCard(
                        modifier = Modifier.widthIn(max = 320.dp),
                        cornerRadius = 18.dp,
                        borderColor = if (isUser) SkyPrimary.copy(alpha = 0.5f) else EmeraldSuccess.copy(alpha = 0.4f)
                    ) {
                        Text(
                            text = if (isUser) "أنت" else "MUNAZZEM AI",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isUser) SkyPrimary else EmeraldSuccess
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(msg.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            when (chatState) {
                is AiChatState.Loading -> {
                    item {
                        GlassCard(modifier = Modifier.widthIn(max = 280.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                Text("يفكر MUNAZZEM AI...", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                is AiChatState.Streaming -> {
                    item {
                        GlassCard(
                            modifier = Modifier.widthIn(max = 320.dp),
                            borderColor = EmeraldSuccess.copy(alpha = 0.5f)
                        ) {
                            Text("MUNAZZEM AI", style = MaterialTheme.typography.labelSmall, color = EmeraldSuccess)
                            Spacer(Modifier.height(4.dp))
                            Text(chatState.partialText, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                is AiChatState.Error -> {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = CrimsonUrgent.copy(alpha = 0.6f)) {
                            Text(chatState.message, color = CrimsonUrgent, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                AiChatState.Idle -> Unit
            }
        }

        // Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("اسأل MUNAZZEM AI...") },
                maxLines = 3,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.weight(1f).testTag("ai_chat_input")
            )
            FilledIconButton(
                onClick = {
                    val toSend = inputText
                    inputText = ""
                    onSendMessage(toSend)
                },
                enabled = inputText.isNotBlank() && chatState !is AiChatState.Loading && chatState !is AiChatState.Streaming,
                modifier = Modifier.size(50.dp).testTag("ai_chat_send_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "إرسال")
            }
        }
    }
}
