package com.example.domain

import java.time.LocalDate

object SpacedRepetitionHelper {
    // Stage 0 -> +1 day (Stage 1)
    // Stage 1 -> +3 days (Stage 2)
    // Stage 2 -> +7 days (Stage 3)
    // Stage 3 -> +14 days (Stage 4)
    // Stage 4+ -> Mastered (0L)
    fun nextReviewOffsetDays(currentStage: Int): Long? = when (currentStage) {
        0 -> 1L
        1 -> 3L
        2 -> 7L
        3 -> 14L
        else -> null
    }

    fun computeNextReviewEpochDay(
        currentStage: Int,
        todayEpochDay: Long = LocalDate.now().toEpochDay()
    ): Pair<Int, Long> {
        val offset = nextReviewOffsetDays(currentStage) ?: return 4 to 0L
        return (currentStage + 1) to (todayEpochDay + offset)
    }

    fun stageLabelArabic(stage: Int): String = when (stage) {
        0 -> "لم تبدأ المراجعة"
        1 -> "المراجعة الأولى (اليوم التالي)"
        2 -> "المراجعة الثانية (بعد 3 أيام)"
        3 -> "المراجعة الثالثة (بعد أسبوع)"
        4 -> "المراجعة الرابعة (بعد أسبوعين)"
        else -> "متقن تماماً"
    }
}
