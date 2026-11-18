package com.example.domain

import com.example.data.local.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class SubjectAnalytics(
    val subject: SubjectEntity,
    val totalLessons: Int,
    val completedLessons: Int,
    val backlogCount: Int,
    val completionPercent: Int,
    val studyMinutes: Int
)

data class DailyStudyPoint(
    val dayLabel: String,
    val hours: Float
)

data class WeeklyReport(
    val completedSummary: String,
    val pendingSummary: String,
    val attentionSubjects: List<String>,
    val bestStudyDay: String,
    val nextWeekSuggestion: String
)

data class DashboardAnalytics(
    val todayStudyHours: Float,
    val weeklyStudyHours: Float,
    val previousWeekStudyHours: Float,
    val completedTasksCount: Int,
    val totalTasksCount: Int,
    val completionRatePercent: Int,
    val delayRatePercent: Int,
    val backlogCount: Int,
    val consistencyPercent: Int,
    val subjectAnalytics: List<SubjectAnalytics>,
    val topTimeSubjectName: String,
    val dailyChartPoints: List<DailyStudyPoint>,
    val weeklyReport: WeeklyReport
)

object AnalyticsEngine {

    fun compute(
        subjects: List<SubjectEntity>,
        lessons: List<LessonEntity>,
        focusSessions: List<FocusSessionEntity>,
        exams: List<ExamEntity>,
        today: LocalDate = LocalDate.now()
    ): DashboardAnalytics {
        val todayEpoch = today.toEpochDay()
        val zone = ZoneId.systemDefault()

        fun epochDayOf(ms: Long): Long =
            Instant.ofEpochMilli(ms).atZone(zone).toLocalDate().toEpochDay()

        val todayMinutes = focusSessions
            .filter { epochDayOf(it.completedAtEpochMs) == todayEpoch }
            .sumOf { it.durationMinutes } +
            lessons.filter { it.completedAtEpochMs > 0 && epochDayOf(it.completedAtEpochMs) == todayEpoch }
                .sumOf { it.actualMinutes }

        val weekDays = (6 downTo 0).map { today.minusDays(it.toLong()) }
        val dailyPoints = weekDays.map { date ->
            val dEpoch = date.toEpochDay()
            val mins = focusSessions
                .filter { epochDayOf(it.completedAtEpochMs) == dEpoch }
                .sumOf { it.durationMinutes } +
                lessons.filter { it.completedAtEpochMs > 0 && epochDayOf(it.completedAtEpochMs) == dEpoch }
                    .sumOf { it.actualMinutes }
            val dayAr = when ((date.dayOfWeek.value % 7) + 1) {
                1 -> "أحد"
                2 -> "إثنين"
                3 -> "ثلاثاء"
                4 -> "أربعاء"
                5 -> "خميس"
                6 -> "جمعة"
                else -> "سبت"
            }
            DailyStudyPoint(dayAr, mins / 60f)
        }

        val weeklyHours = dailyPoints.sumOf { it.hours.toDouble() }.toFloat()

        val prevWeekStart = todayEpoch - 13
        val prevWeekEnd = todayEpoch - 7
        val prevWeekMins = focusSessions
            .filter { epochDayOf(it.completedAtEpochMs) in prevWeekStart..prevWeekEnd }
            .sumOf { it.durationMinutes }
        val prevWeekHours = prevWeekMins / 60f

        val completedCount = lessons.count { LessonStatus.fromRaw(it.status) == LessonStatus.COMPLETED }
        val totalCount = lessons.size
        val completionPct = if (totalCount > 0) (completedCount * 100) / totalCount else 0

        val backlogLessons = lessons.filter { isBacklogLesson(it, todayEpoch) }
        val delayPct = if (totalCount > 0) (backlogLessons.size * 100) / totalCount else 0

        val activeDaysThisWeek = dailyPoints.count { it.hours > 0f }
        val consistencyPct = (activeDaysThisWeek * 100) / 7

        val subjStats = subjects.map { subj ->
            val sLessons = lessons.filter { it.subjectId == subj.id }
            val sDone = sLessons.count { LessonStatus.fromRaw(it.status) == LessonStatus.COMPLETED }
            val sBacklog = sLessons.count { isBacklogLesson(it, todayEpoch) }
            val pct = if (sLessons.isNotEmpty()) (sDone * 100) / sLessons.size else 0
            val mins = subj.studyMinutesLogged +
                focusSessions.filter { it.subjectId == subj.id }.sumOf { it.durationMinutes }
            SubjectAnalytics(
                subject = subj,
                totalLessons = sLessons.size,
                completedLessons = sDone,
                backlogCount = sBacklog,
                completionPercent = pct,
                studyMinutes = mins
            )
        }.sortedByDescending { it.completionPercent }

        val topTimeSubj = subjStats.maxByOrNull { it.studyMinutes }?.subject?.name ?: "لا توجد بيانات بعد"
        val attentionList = subjStats
            .filter { it.backlogCount > 0 || it.completionPercent < 50 }
            .sortedByDescending { it.backlogCount }
            .take(3)
            .map { "${it.subject.name} (متراكم: ${it.backlogCount})" }

        val bestDay = dailyPoints.maxByOrNull { it.hours }?.let {
            if (it.hours > 0f) "${it.dayLabel} (${String.format("%.1f", it.hours)} س)" else "لم يُسجل نشاط بعد"
        } ?: "لم يُسجل نشاط بعد"

        val upcomingExam = exams
            .filter { it.examDateEpochDay >= todayEpoch }
            .minByOrNull { it.examDateEpochDay }

        val nextWeekTip = when {
            upcomingExam != null ->
                "التركيز على مراجعة مادة الامتحان القادم (${upcomingExam.title}) مع تقليل المتراكم تدريجياً."
            backlogLessons.isNotEmpty() ->
                "تخصيص 45 دقيقة يومياً لتصفية ${backlogLessons.size} مهمة متراكمة عبر تقسيمها لأجزاء صغيرة."
            subjects.isEmpty() ->
                "أضف موادك الدراسية ووصف يومك ليصنع لك المنظم خطة أسبوعية متوازنة."
            else ->
                "استمر على نفس الإيقاع المتوازن مع الالتزام بمواعيد المراجعة المتباعدة."
        }

        val report = WeeklyReport(
            completedSummary = if (completedCount > 0) {
                "أنجزت $completedCount من أصل $totalCount مهمة بمعدل ${String.format("%.1f", weeklyHours)} ساعة هذا الأسبوع."
            } else {
                "لم تكتمل مهام هذا الأسبوع بعد — ابدأ بأول جلسة تركيز الآن."
            },
            pendingSummary = if (backlogLessons.isNotEmpty()) {
                "يوجد ${backlogLessons.size} دروس/مهام متراكمة أو تحتاج مراجعة."
            } else {
                "ممتاز! لا توجد مهام متأخرة أو متراكمة حالياً."
            },
            attentionSubjects = attentionList,
            bestStudyDay = bestDay,
            nextWeekSuggestion = nextWeekTip
        )

        return DashboardAnalytics(
            todayStudyHours = todayMinutes / 60f,
            weeklyStudyHours = weeklyHours,
            previousWeekStudyHours = prevWeekHours,
            completedTasksCount = completedCount,
            totalTasksCount = totalCount,
            completionRatePercent = completionPct,
            delayRatePercent = delayPct,
            backlogCount = backlogLessons.size,
            consistencyPercent = consistencyPct,
            subjectAnalytics = subjStats,
            topTimeSubjectName = topTimeSubj,
            dailyChartPoints = dailyPoints,
            weeklyReport = report
        )
    }

    fun isBacklogLesson(lesson: LessonEntity, todayEpochDay: Long): Boolean {
        val status = LessonStatus.fromRaw(lesson.status)
        if (status == LessonStatus.COMPLETED) return false
        if (status == LessonStatus.NEEDS_REVIEW || status == LessonStatus.POSTPONED) return true
        if (lesson.dueDateEpochDay in 1 until todayEpochDay) return true
        return false
    }
}
