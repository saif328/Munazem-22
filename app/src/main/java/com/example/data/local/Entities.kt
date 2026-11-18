package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class LessonKind(val arabicLabel: String) {
    LESSON("درس"),
    ASSIGNMENT("واجب"),
    REVISION("مراجعة");

    companion object {
        fun fromRaw(raw: String?): LessonKind =
            entries.firstOrNull { it.name == raw } ?: LESSON
    }
}

enum class LessonStatus(val arabicLabel: String) {
    NOT_STARTED("لم تبدأ"),
    IN_PROGRESS("قيد المذاكرة"),
    COMPLETED("تمت المذاكرة"),
    NEEDS_REVIEW("تحتاج مراجعة"),
    POSTPONED("مؤجلة");

    companion object {
        fun fromRaw(raw: String?): LessonStatus =
            entries.firstOrNull { it.name == raw } ?: NOT_STARTED
    }
}

enum class BlockCategory(val arabicLabel: String) {
    SCHOOL("المدرسة"),
    PRIVATE_LESSON("درس / حصة"),
    STUDY("مذاكرة"),
    BREAK("راحة"),
    MEAL("طعام"),
    SLEEP("نوم"),
    HOLIDAY("إجازة / مناسبة"),
    PERSONAL("التزام شخصي");

    companion object {
        fun fromRaw(raw: String?): BlockCategory =
            entries.firstOrNull { it.name == raw } ?: STUDY
    }
}

enum class FocusModeType(val arabicLabel: String) {
    POMODORO("بومودورو"),
    CUSTOM("مخصص");

    companion object {
        fun fromRaw(raw: String?): FocusModeType =
            entries.firstOrNull { it.name == raw } ?: POMODORO
    }
}

enum class AttachmentType(val arabicLabel: String) {
    NONE("بدون مرفق"),
    PDF("ملف PDF"),
    IMAGE("صورة"),
    LINK("رابط تعليمي");

    companion object {
        fun fromRaw(raw: String?): AttachmentType =
            entries.firstOrNull { it.name == raw } ?: NONE
    }
}

enum class HolidayKind(val arabicLabel: String) {
    HOLIDAY("يوم إجازة"),
    NO_LESSONS("يوم بدون دروس"),
    SPECIAL_EVENT("مناسبة / التزام خاص");

    companion object {
        fun fromRaw(raw: String?): HolidayKind =
            entries.firstOrNull { it.name == raw } ?: HOLIDAY
    }
}

enum class NotificationCategory(val arabicLabel: String) {
    LESSON("موعد درس"),
    TASK("موعد مهمة"),
    REVIEW("مراجعة متباعدة"),
    EXAM("امتحان قريب"),
    BACKLOG("تنبيه متراكم"),
    GOAL("هدف المذاكرة");

    companion object {
        fun fromRaw(raw: String?): NotificationCategory =
            entries.firstOrNull { it.name == raw } ?: TASK
    }
}

@Entity(tableName = "student_profile")
data class StudentProfileEntity(
    @PrimaryKey val id: String = "main",
    val name: String = "",
    val educationalStage: String = "",
    val grade: String = "",
    val school: String = "",
    val governorate: String = "",
    val studyGoal: String = "",
    val avatarUri: String = "",
    val wakeTimeMinutes: Int = 360,       // 6:00 AM default
    val sleepTimeMinutes: Int = 1380,     // 11:00 PM default
    val schoolStartMinutes: Int = 450,    // 7:30 AM
    val schoolEndMinutes: Int = 840,      // 2:00 PM
    val hasSchoolRoutine: Boolean = false,
    val commuteMinutes: Int = 20,
    val mealBreakMinutes: Int = 30,
    val xp: Int = 0,
    val level: Int = 1,
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val lastActiveEpochDay: Long = 0L,
    val unlockedBadgesCsv: String = "",
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey val id: String = "subj_${UUID.randomUUID().toString().replace("-", "").take(12)}",
    val name: String,
    val teacherName: String = "",
    val colorHex: String = "#38BDF8",
    val difficulty: Int = 3,
    val priority: Int = 3,
    val studyMinutesLogged: Int = 0,
    val notesSummary: String = "",
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey val id: String = "les_${UUID.randomUUID().toString().replace("-", "").take(12)}",
    val subjectId: String,
    val unitName: String = "",
    val chapterName: String = "",
    val title: String,
    val partLabel: String = "",
    val kind: String = LessonKind.LESSON.name,
    val status: String = LessonStatus.NOT_STARTED.name,
    val estimatedMinutes: Int = 45,
    val actualMinutes: Int = 0,
    val pagesCount: Int = 0,
    val questionsSolved: Int = 0,
    val comprehensionPercent: Int = 0,
    val difficultyOrImportance: Int = 3,
    val delayReason: String = "",
    val completionNotes: String = "",
    val startDateEpochDay: Long = 0L,
    val dueDateEpochDay: Long = 0L,
    val completedAtEpochMs: Long = 0L,
    val nextReviewEpochDay: Long = 0L,
    val reviewStage: Int = 0
)

@Entity(tableName = "schedule_blocks")
data class ScheduleBlockEntity(
    @PrimaryKey val id: String = "blk_${UUID.randomUUID().toString().replace("-", "").take(12)}",
    val dayOfWeek: Int, // 1=Sun..7=Sat, 0=Daily template
    val specificDateEpochDay: Long? = null,
    val title: String,
    val category: String = BlockCategory.STUDY.name,
    val subjectId: String? = null,
    val lessonId: String? = null,
    val startMinutes: Int,
    val endMinutes: Int,
    val fixed: Boolean = false,
    val completed: Boolean = false,
    val orderIndex: Int = 0
)

@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey val id: String = "exm_${UUID.randomUUID().toString().replace("-", "").take(12)}",
    val title: String,
    val subjectId: String,
    val examDateEpochDay: Long,
    val timeMinutes: Int = 540,
    val requiredParts: String = "",
    val readinessPercent: Int = 0,
    val achievedScore: Double? = null,
    val maxScore: Double = 100.0,
    val revisionPlanSummary: String = "",
    val reminderEnabled: Boolean = true
)

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey val id: String = "foc_${UUID.randomUUID().toString().replace("-", "").take(12)}",
    val subjectId: String? = null,
    val lessonId: String? = null,
    val durationMinutes: Int,
    val breakMinutes: Int,
    val mode: String = FocusModeType.POMODORO.name,
    val completedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_notes")
data class StudyNoteEntity(
    @PrimaryKey val id: String = "not_${UUID.randomUUID().toString().replace("-", "").take(12)}",
    val subjectId: String? = null,
    val title: String,
    val content: String,
    val linkUrl: String = "",
    val attachmentUri: String = "",
    val attachmentType: String = AttachmentType.NONE.name,
    val important: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "holidays")
data class HolidayOrBreakEntity(
    @PrimaryKey val id: String = "hol_${UUID.randomUUID().toString().replace("-", "").take(12)}",
    val dateEpochDay: Long,
    val title: String,
    val kind: String = HolidayKind.HOLIDAY.name,
    val blockAllStudy: Boolean = true,
    val startMinutes: Int? = null,
    val endMinutes: Int? = null
)

@Entity(tableName = "app_notifications")
data class AppNotificationEntity(
    @PrimaryKey val id: String = "ntf_${UUID.randomUUID().toString().replace("-", "").take(12)}",
    val title: String,
    val body: String,
    val category: String = NotificationCategory.TASK.name,
    val triggerAtEpochMs: Long = System.currentTimeMillis(),
    val read: Boolean = false,
    val relatedRoute: String = "dashboard"
)

@Entity(tableName = "ai_chat_messages")
data class AiChatMessageEntity(
    @PrimaryKey val id: String = "msg_${UUID.randomUUID().toString().replace("-", "").take(12)}",
    val role: String, // "user" or "model"
    val text: String,
    val timestampMs: Long = System.currentTimeMillis()
)
