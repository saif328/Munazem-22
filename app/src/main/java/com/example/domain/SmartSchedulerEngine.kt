package com.example.domain

import com.example.data.local.*
import java.time.LocalDate
import java.util.UUID

data class ScheduleGenerationResult(
    val generatedBlocks: List<ScheduleBlockEntity>,
    val overflowLessons: List<LessonEntity>,
    val warnings: List<String>
)

object SmartSchedulerEngine {

    /**
     * Generates a conflict-free weekly schedule (days 1=Sun .. 7=Sat) starting from today.
     * Respects wake/sleep, school, commute around lessons, meal breaks, fixed blocks,
     * registered holidays/no-lesson days, rest buffers, exam urgency, difficulty, and priority.
     * Never schedules two blocks at the same time and places unscheduled tasks into overflowLessons.
     */
    fun generateWeeklySchedule(
        profile: StudentProfileEntity,
        subjects: List<SubjectEntity>,
        lessons: List<LessonEntity>,
        existingBlocks: List<ScheduleBlockEntity>,
        exams: List<ExamEntity>,
        holidays: List<HolidayOrBreakEntity>,
        todayEpochDay: Long = LocalDate.now().toEpochDay()
    ): ScheduleGenerationResult {
        val warnings = mutableListOf<String>()
        val subjectMap = subjects.associateBy { it.id }

        // Fixed blocks kept untouched (user-pinned or private lessons/school/personal)
        val fixedBlocks = existingBlocks.filter { it.fixed }

        // Build candidate tasks prioritized by:
        // 1. Overdue / backlog or needs review
        // 2. Nearest upcoming exam for subject
        // 3. Subject priority + difficulty + lesson importance
        val pendingLessons = lessons.filter {
            val st = LessonStatus.fromRaw(it.status)
            st != LessonStatus.COMPLETED
        }.sortedByDescending { lesson ->
            val subj = subjectMap[lesson.subjectId]
            val st = LessonStatus.fromRaw(lesson.status)
            val isOverdue = lesson.dueDateEpochDay in 1 until todayEpochDay
            val nearestExamDays = exams
                .filter { it.subjectId == lesson.subjectId && it.examDateEpochDay >= todayEpochDay }
                .minOfOrNull { (it.examDateEpochDay - todayEpochDay).toInt() } ?: 60

            val urgencyScore = when {
                isOverdue -> 45
                st == LessonStatus.NEEDS_REVIEW -> 38
                st == LessonStatus.POSTPONED -> 30
                st == LessonStatus.IN_PROGRESS -> 25
                else -> 15
            }
            val examBoost = (30 - nearestExamDays.coerceAtMost(30)) * 2
            val subjBoost = ((subj?.priority ?: 3) * 6) + ((subj?.difficulty ?: 3) * 4)
            urgencyScore + examBoost + subjBoost + (lesson.difficultyOrImportance * 5)
        }.toMutableList()

        val allGenerated = mutableListOf<ScheduleBlockEntity>()
        val dateStart = LocalDate.ofEpochDay(todayEpochDay)

        for (offset in 0 until 7) {
            val date = dateStart.plusDays(offset.toLong())
            val epochDay = date.toEpochDay()
            // Convert Java DayOfWeek (1=Mon..7=Sun) to our 1=Sun..7=Sat
            val dayOfWeek = (date.dayOfWeek.value % 7) + 1

            val dayHolidays = holidays.filter { it.dateEpochDay == epochDay }
            val fullDayBlocked = dayHolidays.any { it.blockAllStudy }
            if (fullDayBlocked) {
                val holTitle = dayHolidays.firstOrNull { it.blockAllStudy }?.title ?: "يوم إجازة"
                allGenerated.add(
                    ScheduleBlockEntity(
                        id = "blk_${UUID.randomUUID().toString().replace("-", "").take(10)}",
                        dayOfWeek = dayOfWeek,
                        specificDateEpochDay = epochDay,
                        title = "إجازة: $holTitle (راحة واستجمام)",
                        category = BlockCategory.HOLIDAY.name,
                        startMinutes = profile.wakeTimeMinutes,
                        endMinutes = (profile.wakeTimeMinutes + 60).coerceAtMost(1439),
                        fixed = false,
                        completed = false,
                        orderIndex = 0
                    )
                )
                continue
            }

            // Collect busy intervals for this day
            val busy = mutableListOf<IntRange>()

            // Sleep guard: before wakeTime and after sleepTime
            if (profile.wakeTimeMinutes > 0) {
                busy.add(0 until profile.wakeTimeMinutes)
            }
            if (profile.sleepTimeMinutes < 1440) {
                busy.add(profile.sleepTimeMinutes until 1440)
            }

            // School routine (Sun..Thu = 1..5)
            if (profile.hasSchoolRoutine && dayOfWeek in 1..5 &&
                profile.schoolEndMinutes > profile.schoolStartMinutes
            ) {
                val noLessonsHoliday = dayHolidays.any { HolidayKind.fromRaw(it.kind) == HolidayKind.NO_LESSONS }
                if (!noLessonsHoliday) {
                    busy.add(profile.schoolStartMinutes until profile.schoolEndMinutes)
                    // Lunch/meal + recovery right after school
                    val mealStart = profile.schoolEndMinutes
                    val mealEnd = (mealStart + profile.mealBreakMinutes.coerceAtLeast(30)).coerceAtMost(1439)
                    busy.add(mealStart until mealEnd)
                    allGenerated.add(
                        ScheduleBlockEntity(
                            dayOfWeek = dayOfWeek,
                            specificDateEpochDay = epochDay,
                            title = "المدرسة",
                            category = BlockCategory.SCHOOL.name,
                            startMinutes = profile.schoolStartMinutes,
                            endMinutes = profile.schoolEndMinutes,
                            fixed = false,
                            completed = false,
                            orderIndex = 1
                        )
                    )
                    allGenerated.add(
                        ScheduleBlockEntity(
                            dayOfWeek = dayOfWeek,
                            specificDateEpochDay = epochDay,
                            title = "غداء وراحة بعد المدرسة",
                            category = BlockCategory.MEAL.name,
                            startMinutes = mealStart,
                            endMinutes = mealEnd,
                            fixed = false,
                            completed = false,
                            orderIndex = 2
                        )
                    )
                }
            }

            // Fixed blocks for this day (or daily template dayOfWeek==0)
            val dayFixed = fixedBlocks.filter {
                it.dayOfWeek == dayOfWeek || it.dayOfWeek == 0 || it.specificDateEpochDay == epochDay
            }
            for (fb in dayFixed) {
                val isPrivateLesson = BlockCategory.fromRaw(fb.category) == BlockCategory.PRIVATE_LESSON
                val commute = if (isPrivateLesson) profile.commuteMinutes else 0
                val s = (fb.startMinutes - commute).coerceAtLeast(0)
                val e = (fb.endMinutes + commute).coerceAtMost(1440)
                busy.add(s until e)
            }

            // Special event partial blocks from holidays table
            for (h in dayHolidays.filter { !it.blockAllStudy && it.startMinutes != null && it.endMinutes != null }) {
                busy.add(h.startMinutes!! until h.endMinutes!!)
            }

            // Detect conflicts among fixed blocks themselves
            for (i in dayFixed.indices) {
                for (j in i + 1 until dayFixed.size) {
                    val a = dayFixed[i]
                    val b = dayFixed[j]
                    if (a.startMinutes < b.endMinutes && b.startMinutes < a.endMinutes) {
                        warnings.add("تعارض في يوم ${arabicDayName(dayOfWeek)}: '${a.title}' مع '${b.title}'")
                    }
                }
            }

            // Compute free intervals inside [wake, sleep]
            val freeWindows = computeFreeWindows(
                startMin = profile.wakeTimeMinutes,
                endMin = profile.sleepTimeMinutes,
                busy = busy
            )

            // Leave ~25% of free time untouched so we never cram 100% of student's free time
            val totalFree = freeWindows.sumOf { it.last - it.first + 1 }
            val healthyStudyCapMinutes = (totalFree * 0.72).toInt().coerceAtLeast(0)
            var scheduledStudyMinutes = 0
            var orderIdx = 10

            for (window in freeWindows) {
                var cursor = window.first
                val windowEnd = window.last + 1

                while (cursor + 30 <= windowEnd &&
                    scheduledStudyMinutes < healthyStudyCapMinutes &&
                    pendingLessons.isNotEmpty()
                ) {
                    val availableInWindow = windowEnd - cursor
                    val nextTask = pendingLessons.firstOrNull { it.estimatedMinutes <= availableInWindow }
                        ?: pendingLessons.firstOrNull()
                        ?: break

                    val sessionLen = nextTask.estimatedMinutes.coerceIn(25, 60)
                        .coerceAtMost(availableInWindow)
                    if (sessionLen < 25) break

                    pendingLessons.remove(nextTask)
                    val subjName = subjectMap[nextTask.subjectId]?.name ?: "مذاكرة"

                    allGenerated.add(
                        ScheduleBlockEntity(
                            dayOfWeek = dayOfWeek,
                            specificDateEpochDay = epochDay,
                            title = "$subjName: ${nextTask.title}",
                            category = BlockCategory.STUDY.name,
                            subjectId = nextTask.subjectId,
                            lessonId = nextTask.id,
                            startMinutes = cursor,
                            endMinutes = cursor + sessionLen,
                            fixed = false,
                            completed = false,
                            orderIndex = orderIdx++
                        )
                    )
                    scheduledStudyMinutes += sessionLen
                    cursor += sessionLen

                    // Insert mandatory 10-15 min rest break after study block if space allows
                    val breakLen = 15
                    if (cursor + breakLen <= windowEnd) {
                        allGenerated.add(
                            ScheduleBlockEntity(
                                dayOfWeek = dayOfWeek,
                                specificDateEpochDay = epochDay,
                                title = "استراحة قصيرة وتجديد تركيز",
                                category = BlockCategory.BREAK.name,
                                startMinutes = cursor,
                                endMinutes = cursor + breakLen,
                                fixed = false,
                                completed = false,
                                orderIndex = orderIdx++
                            )
                        )
                        cursor += breakLen
                    }
                }
            }
        }

        return ScheduleGenerationResult(
            generatedBlocks = allGenerated,
            overflowLessons = pendingLessons,
            warnings = warnings.distinct()
        )
    }

    fun hasConflict(
        candidateStart: Int,
        candidateEnd: Int,
        dayOfWeek: Int,
        excludeBlockId: String?,
        existingBlocks: List<ScheduleBlockEntity>
    ): ScheduleBlockEntity? {
        return existingBlocks.firstOrNull { blk ->
            blk.id != excludeBlockId &&
                (blk.dayOfWeek == dayOfWeek || blk.dayOfWeek == 0) &&
                candidateStart < blk.endMinutes &&
                blk.startMinutes < candidateEnd
        }
    }

    private fun computeFreeWindows(
        startMin: Int,
        endMin: Int,
        busy: List<IntRange>
    ): List<IntRange> {
        if (endMin <= startMin) return emptyList()
        val sorted = busy
            .map { (it.first.coerceAtLeast(startMin))..(it.last.coerceAtMost(endMin - 1)) }
            .filter { it.first <= it.last }
            .sortedBy { it.first }

        val merged = mutableListOf<IntRange>()
        for (r in sorted) {
            if (merged.isEmpty() || r.first > merged.last().last + 1) {
                merged.add(r)
            } else {
                val prev = merged.removeAt(merged.lastIndex)
                merged.add(prev.first..maxOf(prev.last, r.last))
            }
        }

        val free = mutableListOf<IntRange>()
        var cursor = startMin
        for (b in merged) {
            if (cursor < b.first) {
                free.add(cursor..(b.first - 1))
            }
            cursor = maxOf(cursor, b.last + 1)
        }
        if (cursor < endMin) {
            free.add(cursor..(endMin - 1))
        }
        return free
    }

    private fun arabicDayName(dayOfWeek: Int): String = when (dayOfWeek) {
        1 -> "الأحد"
        2 -> "الإثنين"
        3 -> "الثلاثاء"
        4 -> "الأربعاء"
        5 -> "الخميس"
        6 -> "الجمعة"
        7 -> "السبت"
        else -> "اليومي"
    }
}
