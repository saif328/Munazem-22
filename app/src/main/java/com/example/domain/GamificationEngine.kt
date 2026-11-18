package com.example.domain

import com.example.data.local.*

data class BadgeInfo(
    val id: String,
    val title: String,
    val description: String,
    val unlocked: Boolean
)

data class ChallengeInfo(
    val title: String,
    val progressText: String,
    val progressFraction: Float,
    val rewardXp: Int,
    val completed: Boolean
)

data class GamificationSnapshot(
    val xp: Int,
    val level: Int,
    val levelTitle: String,
    val xpIntoCurrentLevel: Int,
    val xpForNextLevel: Int,
    val streakDays: Int,
    val gentleStreakMessage: String,
    val badges: List<BadgeInfo>,
    val dailyChallenge: ChallengeInfo,
    val weeklyChallenge: ChallengeInfo
)

object GamificationEngine {

    fun evaluate(
        profile: StudentProfileEntity?,
        lessons: List<LessonEntity>,
        focusSessions: List<FocusSessionEntity>,
        todayEpochDay: Long
    ): GamificationSnapshot {
        val xp = profile?.xp ?: 0
        val level = (xp / 250) + 1
        val xpIntoLevel = xp % 250
        val levelTitle = when {
            level >= 15 -> "عبقري مُنظِّم"
            level >= 10 -> "بطل التفوق"
            level >= 6 -> "باحث متميز"
            level >= 3 -> "طالب مثابر"
            else -> "مبتدئ منظم"
        }

        val completedLessons = lessons.count { LessonStatus.fromRaw(it.status) == LessonStatus.COMPLETED }
        val reviewedCount = lessons.count { it.reviewStage >= 2 }
        val clearedBacklogCount = lessons.count {
            LessonStatus.fromRaw(it.status) == LessonStatus.COMPLETED && it.delayReason.isNotBlank()
        }
        val focusCount = focusSessions.size
        val streak = profile?.currentStreakDays ?: 0

        val badges = listOf(
            BadgeInfo(
                id = "first_step",
                title = "أول خطوة",
                description = "إتمام أول درس أو مهمة بنجاح",
                unlocked = completedLessons >= 1
            ),
            BadgeInfo(
                id = "week_streak",
                title = "أسبوع من الالتزام",
                description = "الالتزام بالمذاكرة لمدة 7 أيام",
                unlocked = streak >= 7 || (profile?.longestStreakDays ?: 0) >= 7
            ),
            BadgeInfo(
                id = "backlog_slayer",
                title = "عدو المتراكم",
                description = "إنهاء 5 دروس متراكمة أو متأخرة",
                unlocked = clearedBacklogCount >= 5 || completedLessons >= 10
            ),
            BadgeInfo(
                id = "review_hero",
                title = "بطل المراجعة",
                description = "إتمام 5 مراحل مراجعة متباعدة",
                unlocked = reviewedCount >= 5
            ),
            BadgeInfo(
                id = "focus_king",
                title = "ملك التركيز",
                description = "إكمال 5 جلسات بومودورو مركزة",
                unlocked = focusCount >= 5
            ),
            BadgeInfo(
                id = "hundred_tasks",
                title = "إنجاز 100 مهمة",
                description = "إتمام 100 مهمة دراسية",
                unlocked = completedLessons >= 100
            )
        )

        // Daily challenge: complete 2 lessons or 2 focus sessions today
        val todayMsStart = todayEpochDay * 86_400_000L
        val todayFocus = focusSessions.count { it.completedAtEpochMs >= todayMsStart }
        val todayLessons = lessons.count { it.completedAtEpochMs >= todayMsStart }
        val dailyDone = (todayFocus + todayLessons).coerceAtMost(2)

        // Weekly challenge: complete 10 study sessions/tasks
        val weekMsStart = (todayEpochDay - 6) * 86_400_000L
        val weeklyDone = (focusSessions.count { it.completedAtEpochMs >= weekMsStart } +
            lessons.count { it.completedAtEpochMs >= weekMsStart }).coerceAtMost(10)

        val gentleMsg = when {
            streak >= 7 -> "رائع! سلسلتك المستمرة ($streak أيام) تعكس انضباطاً مذهلاً."
            streak >= 1 -> "بداية ممتازة ($streak يوم). استمر بخطوات ثابتة ومريحة."
            else -> "كل يوم هو فرصة جديدة للانطلاق — ابدأ بمهمة واحدة خفيفة اليوم دون قلق."
        }

        return GamificationSnapshot(
            xp = xp,
            level = level,
            levelTitle = levelTitle,
            xpIntoCurrentLevel = xpIntoLevel,
            xpForNextLevel = 250,
            streakDays = streak,
            gentleStreakMessage = gentleMsg,
            badges = badges,
            dailyChallenge = ChallengeInfo(
                title = "التحدي اليومي: إنجاز جلستي مذاكرة أو درسين",
                progressText = "$dailyDone / 2",
                progressFraction = dailyDone / 2f,
                rewardXp = 50,
                completed = dailyDone >= 2
            ),
            weeklyChallenge = ChallengeInfo(
                title = "التحدي الأسبوعي: 10 إنجازات دراسية",
                progressText = "$weeklyDone / 10",
                progressFraction = weeklyDone / 10f,
                rewardXp = 200,
                completed = weeklyDone >= 10
            )
        )
    }
}
