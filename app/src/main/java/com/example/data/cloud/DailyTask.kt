package com.example.data.cloud

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class DailyTaskPriority(val level: Long, val arabicLabel: String) {
    LOW(1L, "منخفضة"),
    MEDIUM(2L, "متوسطة"),
    HIGH(3L, "عالية"),
    URGENT(4L, "عاجلة");

    companion object {
        fun fromLevel(level: Long): DailyTaskPriority =
            entries.firstOrNull { it.level == level } ?: MEDIUM
    }
}

@Keep
@IgnoreExtraProperties
data class DailyTask(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val subjectId: String? = null,
    val subjectName: String = "",
    val dateEpochDay: Long = 0L,
    val scheduledStartMinutes: Long? = null,
    val estimatedMinutes: Long = 30L,
    val priority: Long = DailyTaskPriority.MEDIUM.level,
    val completed: Boolean = false,
    val completedAtEpochMs: Long? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val priorityEnum: DailyTaskPriority
        get() = DailyTaskPriority.fromLevel(priority)

    @Suppress("UNCHECKED_CAST")
    fun toCreateMap(uid: String): Map<String, Any> {
        val cleanTitle = title.trim().take(200)
        require(cleanTitle.isNotEmpty()) { "عنوان المهمة مطلوب ولا يمكن أن يكون فارغاً." }

        return mutableMapOf<String, Any?>(
            "userId" to uid,
            "title" to cleanTitle,
            "description" to description.trim().take(1000),
            "subjectId" to subjectId?.trim()?.take(128)?.ifBlank { null },
            "subjectName" to subjectName.trim().take(100),
            "dateEpochDay" to dateEpochDay,
            "scheduledStartMinutes" to scheduledStartMinutes?.coerceIn(0L, 1439L),
            "estimatedMinutes" to estimatedMinutes.coerceIn(1L, 600L),
            "priority" to priority.coerceIn(1L, 4L),
            "completed" to completed,
            "completedAtEpochMs" to completedAtEpochMs?.coerceAtLeast(0L),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>
    }

    fun toUpdateMap(): Map<String, Any> {
        val cleanTitle = title.trim().take(200)
        require(cleanTitle.isNotEmpty()) { "عنوان المهمة مطلوب ولا يمكن أن يكون فارغاً." }

        val cleanSubjectId = subjectId?.trim()?.take(128)?.ifBlank { null }
        val cleanStartMinutes = scheduledStartMinutes?.coerceIn(0L, 1439L)
        val cleanCompletedAt = if (completed) {
            (completedAtEpochMs ?: System.currentTimeMillis()).coerceAtLeast(0L)
        } else {
            null
        }

        return mapOf(
            "title" to cleanTitle,
            "description" to description.trim().take(1000),
            "subjectId" to (cleanSubjectId ?: FieldValue.delete()),
            "subjectName" to subjectName.trim().take(100),
            "dateEpochDay" to dateEpochDay,
            "scheduledStartMinutes" to (cleanStartMinutes ?: FieldValue.delete()),
            "estimatedMinutes" to estimatedMinutes.coerceIn(1L, 600L),
            "priority" to priority.coerceIn(1L, 4L),
            "completed" to completed,
            "completedAtEpochMs" to (cleanCompletedAt ?: FieldValue.delete()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}
