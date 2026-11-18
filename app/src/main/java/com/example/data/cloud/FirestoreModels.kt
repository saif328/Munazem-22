package com.example.data.cloud

import androidx.annotation.Keep
import com.example.data.local.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class FsStudentProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val educationalStage: String = "",
    val grade: String = "",
    val school: String = "",
    val governorate: String = "",
    val studyGoal: String = "",
    val wakeTimeMinutes: Long = 360L,
    val sleepTimeMinutes: Long = 1380L,
    val schoolStartMinutes: Long = 450L,
    val schoolEndMinutes: Long = 840L,
    val commuteMinutes: Long = 20L,
    val mealBreakMinutes: Long = 30L,
    val xp: Long = 0L,
    val level: Long = 1L,
    val currentStreakDays: Long = 0L,
    val longestStreakDays: Long = 0L,
    val unlockedBadgesCsv: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FsSubject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val teacherName: String = "",
    val colorHex: String = "#38BDF8",
    val difficulty: Long = 3L,
    val priority: Long = 3L,
    val studyMinutesLogged: Long = 0L,
    val notesSummary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FsLesson(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subjectId: String = "",
    val unitName: String = "",
    val chapterName: String = "",
    val title: String = "",
    val partLabel: String = "",
    val kind: String = LessonKind.LESSON.name,
    val status: String = LessonStatus.NOT_STARTED.name,
    val estimatedMinutes: Long = 45L,
    val actualMinutes: Long = 0L,
    val pagesCount: Long = 0L,
    val questionsSolved: Long = 0L,
    val comprehensionPercent: Long = 0L,
    val difficultyOrImportance: Long = 3L,
    val delayReason: String = "",
    val completionNotes: String = "",
    val startDateEpochDay: Long = 0L,
    val dueDateEpochDay: Long = 0L,
    val nextReviewEpochDay: Long = 0L,
    val reviewStage: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FsScheduleBlock(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dayOfWeek: Long = 0L,
    val specificDateEpochDay: Long? = null,
    val title: String = "",
    val category: String = BlockCategory.STUDY.name,
    val subjectId: String? = null,
    val lessonId: String? = null,
    val startMinutes: Long = 0L,
    val endMinutes: Long = 0L,
    val fixed: Boolean = false,
    val completed: Boolean = false,
    val orderIndex: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FsExam(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val subjectId: String = "",
    val examDateEpochDay: Long = 0L,
    val timeMinutes: Long = 540L,
    val requiredParts: String = "",
    val readinessPercent: Long = 0L,
    val achievedScore: Double? = null,
    val maxScore: Double = 100.0,
    val revisionPlanSummary: String = "",
    val reminderEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FsFocusSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subjectId: String? = null,
    val lessonId: String? = null,
    val durationMinutes: Long = 25L,
    val breakMinutes: Long = 5L,
    val mode: String = FocusModeType.POMODORO.name,
    val completedAtEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FsStudyNote(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subjectId: String? = null,
    val title: String = "",
    val content: String = "",
    val linkUrl: String = "",
    val attachmentType: String = AttachmentType.NONE.name,
    val important: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FsHoliday(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateEpochDay: Long = 0L,
    val title: String = "",
    val kind: String = HolidayKind.HOLIDAY.name,
    val blockAllStudy: Boolean = true,
    val startMinutes: Long? = null,
    val endMinutes: Long? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Suppress("UNCHECKED_CAST")
fun StudentProfileEntity.toFirestoreMap(uid: String, isCreate: Boolean): Map<String, Any> {
    val base = mutableMapOf<String, Any?>(
        "name" to name.ifBlank { "طالب" }.take(100),
        "educationalStage" to educationalStage.take(80),
        "grade" to grade.take(80),
        "school" to school.take(120),
        "governorate" to governorate.take(80),
        "studyGoal" to studyGoal.take(300),
        "wakeTimeMinutes" to wakeTimeMinutes.coerceIn(0, 1439),
        "sleepTimeMinutes" to sleepTimeMinutes.coerceIn(0, 1439),
        "schoolStartMinutes" to schoolStartMinutes.coerceIn(0, 1439),
        "schoolEndMinutes" to schoolEndMinutes.coerceIn(0, 1439),
        "commuteMinutes" to commuteMinutes.coerceIn(0, 240),
        "mealBreakMinutes" to mealBreakMinutes.coerceIn(0, 240),
        "xp" to xp.coerceAtLeast(0),
        "level" to level.coerceIn(1, 200),
        "currentStreakDays" to currentStreakDays.coerceAtLeast(0),
        "longestStreakDays" to longestStreakDays.coerceAtLeast(0),
        "unlockedBadgesCsv" to unlockedBadgesCsv.take(1000),
        "updatedAt" to FieldValue.serverTimestamp()
    )
    if (isCreate) {
        base["userId"] = uid
        base["createdAt"] = FieldValue.serverTimestamp()
    }
    return base.filterValues { it != null } as Map<String, Any>
}

@Suppress("UNCHECKED_CAST")
fun SubjectEntity.toFirestoreMap(uid: String, isCreate: Boolean): Map<String, Any> {
    val base = mutableMapOf<String, Any?>(
        "name" to name.ifBlank { "مادة" }.take(100),
        "teacherName" to teacherName.take(100),
        "colorHex" to colorHex.ifBlank { "#38BDF8" }.take(16),
        "difficulty" to difficulty.coerceIn(1, 5),
        "priority" to priority.coerceIn(1, 5),
        "studyMinutesLogged" to studyMinutesLogged.coerceAtLeast(0),
        "notesSummary" to notesSummary.take(2000),
        "updatedAt" to FieldValue.serverTimestamp()
    )
    if (isCreate) {
        base["userId"] = uid
        base["createdAt"] = FieldValue.serverTimestamp()
    }
    return base.filterValues { it != null } as Map<String, Any>
}

@Suppress("UNCHECKED_CAST")
fun LessonEntity.toFirestoreMap(uid: String, isCreate: Boolean): Map<String, Any> {
    val base = mutableMapOf<String, Any?>(
        "subjectId" to subjectId.take(128),
        "unitName" to unitName.take(120),
        "chapterName" to chapterName.take(120),
        "title" to title.ifBlank { "درس" }.take(200),
        "partLabel" to partLabel.take(120),
        "kind" to LessonKind.fromRaw(kind).name,
        "status" to LessonStatus.fromRaw(status).name,
        "estimatedMinutes" to estimatedMinutes.coerceIn(1, 1440),
        "actualMinutes" to actualMinutes.coerceAtLeast(0),
        "pagesCount" to pagesCount.coerceIn(0, 10000),
        "questionsSolved" to questionsSolved.coerceIn(0, 10000),
        "comprehensionPercent" to comprehensionPercent.coerceIn(0, 100),
        "difficultyOrImportance" to difficultyOrImportance.coerceIn(1, 5),
        "delayReason" to delayReason.take(400),
        "completionNotes" to completionNotes.take(2000),
        "startDateEpochDay" to startDateEpochDay,
        "dueDateEpochDay" to dueDateEpochDay,
        "nextReviewEpochDay" to nextReviewEpochDay,
        "reviewStage" to reviewStage.coerceIn(0, 10),
        "updatedAt" to FieldValue.serverTimestamp()
    )
    if (isCreate) {
        base["userId"] = uid
        base["createdAt"] = FieldValue.serverTimestamp()
    }
    return base.filterValues { it != null } as Map<String, Any>
}

@Suppress("UNCHECKED_CAST")
fun ScheduleBlockEntity.toFirestoreMap(uid: String, isCreate: Boolean): Map<String, Any> {
    val base = mutableMapOf<String, Any?>(
        "dayOfWeek" to dayOfWeek.coerceIn(0, 7),
        "specificDateEpochDay" to specificDateEpochDay,
        "title" to title.ifBlank { "جلسة" }.take(200),
        "category" to BlockCategory.fromRaw(category).name,
        "subjectId" to subjectId?.take(128),
        "lessonId" to lessonId?.take(128),
        "startMinutes" to startMinutes.coerceIn(0, 1439),
        "endMinutes" to endMinutes.coerceIn(0, 1440),
        "fixed" to fixed,
        "completed" to completed,
        "orderIndex" to orderIndex.coerceIn(0, 1000),
        "updatedAt" to FieldValue.serverTimestamp()
    )
    if (isCreate) {
        base["userId"] = uid
        base["createdAt"] = FieldValue.serverTimestamp()
    }
    return base.filterValues { it != null } as Map<String, Any>
}

@Suppress("UNCHECKED_CAST")
fun ExamEntity.toFirestoreMap(uid: String, isCreate: Boolean): Map<String, Any> {
    val base = mutableMapOf<String, Any?>(
        "title" to title.ifBlank { "امتحان" }.take(200),
        "subjectId" to subjectId.take(128),
        "examDateEpochDay" to examDateEpochDay,
        "timeMinutes" to timeMinutes.coerceIn(0, 1439),
        "requiredParts" to requiredParts.take(1000),
        "readinessPercent" to readinessPercent.coerceIn(0, 100),
        "achievedScore" to achievedScore?.coerceIn(0.0, 10000.0),
        "maxScore" to maxScore.coerceIn(1.0, 10000.0),
        "revisionPlanSummary" to revisionPlanSummary.take(2000),
        "reminderEnabled" to reminderEnabled,
        "updatedAt" to FieldValue.serverTimestamp()
    )
    if (isCreate) {
        base["userId"] = uid
        base["createdAt"] = FieldValue.serverTimestamp()
    }
    return base.filterValues { it != null } as Map<String, Any>
}

@Suppress("UNCHECKED_CAST")
fun FocusSessionEntity.toFirestoreMap(uid: String): Map<String, Any> {
    return mutableMapOf<String, Any?>(
        "userId" to uid,
        "subjectId" to subjectId?.take(128),
        "lessonId" to lessonId?.take(128),
        "durationMinutes" to durationMinutes.coerceIn(1, 600),
        "breakMinutes" to breakMinutes.coerceIn(0, 120),
        "mode" to FocusModeType.fromRaw(mode).name,
        "completedAtEpochMs" to completedAtEpochMs.coerceAtLeast(0L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null } as Map<String, Any>
}

@Suppress("UNCHECKED_CAST")
fun StudyNoteEntity.toFirestoreMap(uid: String, isCreate: Boolean): Map<String, Any> {
    val base = mutableMapOf<String, Any?>(
        "subjectId" to subjectId?.take(128),
        "title" to title.ifBlank { "ملاحظة" }.take(200),
        "content" to content.take(10000),
        "linkUrl" to linkUrl.take(1000),
        "attachmentType" to AttachmentType.fromRaw(attachmentType).name,
        "important" to important,
        "updatedAt" to FieldValue.serverTimestamp()
    )
    if (isCreate) {
        base["userId"] = uid
        base["createdAt"] = FieldValue.serverTimestamp()
    }
    return base.filterValues { it != null } as Map<String, Any>
}

@Suppress("UNCHECKED_CAST")
fun HolidayOrBreakEntity.toFirestoreMap(uid: String, isCreate: Boolean): Map<String, Any> {
    val base = mutableMapOf<String, Any?>(
        "dateEpochDay" to dateEpochDay,
        "title" to title.ifBlank { "إجازة" }.take(200),
        "kind" to HolidayKind.fromRaw(kind).name,
        "blockAllStudy" to blockAllStudy,
        "startMinutes" to startMinutes?.coerceIn(0, 1439),
        "endMinutes" to endMinutes?.coerceIn(0, 1440),
        "updatedAt" to FieldValue.serverTimestamp()
    )
    if (isCreate) {
        base["userId"] = uid
        base["createdAt"] = FieldValue.serverTimestamp()
    }
    return base.filterValues { it != null } as Map<String, Any>
}
