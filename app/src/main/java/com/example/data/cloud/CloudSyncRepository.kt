package com.example.data.cloud

import android.content.Context
import com.example.R
import com.example.data.local.*
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class CloudSyncRepository(
    private val db: FirebaseFirestore,
    private val dao: AppDao,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context, dao: AppDao) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        dao
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("يجب تسجيل الدخول بحساب Google أولاً للمزامنة السحابية.")
    }

    private fun userRoot(uid: String): DocumentReference = db.collection("users").document(uid)

    private suspend fun upsertDocument(
        docRef: DocumentReference,
        createMap: Map<String, Any>,
        updateMap: Map<String, Any>
    ) {
        val snap = docRef.get().await()
        if (snap.exists()) {
            docRef.update(updateMap).await()
        } else {
            docRef.set(createMap).await()
        }
    }

    suspend fun syncProfile(profile: StudentProfileEntity): Result<Unit> {
        var path = "users/unknown/profile/main"
        return try {
            val uid = requireUserId()
            val ref = userRoot(uid).collection("profile").document("main")
            path = ref.path
            upsertDocument(
                ref,
                profile.toFirestoreMap(uid, isCreate = true),
                profile.toFirestoreMap(uid, isCreate = false)
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun syncSubject(subject: SubjectEntity): Result<Unit> {
        var path = "users/unknown/subjects/${subject.id}"
        return try {
            val uid = requireUserId()
            val ref = userRoot(uid).collection("subjects").document(subject.id)
            path = ref.path
            upsertDocument(ref, subject.toFirestoreMap(uid, true), subject.toFirestoreMap(uid, false))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCloudSubject(subjectId: String): Result<Unit> = deleteDoc("subjects", subjectId)

    suspend fun syncLesson(lesson: LessonEntity): Result<Unit> {
        var path = "users/unknown/lessons/${lesson.id}"
        return try {
            val uid = requireUserId()
            val ref = userRoot(uid).collection("lessons").document(lesson.id)
            path = ref.path
            upsertDocument(ref, lesson.toFirestoreMap(uid, true), lesson.toFirestoreMap(uid, false))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCloudLesson(lessonId: String): Result<Unit> = deleteDoc("lessons", lessonId)

    suspend fun syncScheduleBlock(block: ScheduleBlockEntity): Result<Unit> {
        var path = "users/unknown/schedule_blocks/${block.id}"
        return try {
            val uid = requireUserId()
            val ref = userRoot(uid).collection("schedule_blocks").document(block.id)
            path = ref.path
            upsertDocument(ref, block.toFirestoreMap(uid, true), block.toFirestoreMap(uid, false))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCloudScheduleBlock(blockId: String): Result<Unit> = deleteDoc("schedule_blocks", blockId)

    suspend fun syncExam(exam: ExamEntity): Result<Unit> {
        var path = "users/unknown/exams/${exam.id}"
        return try {
            val uid = requireUserId()
            val ref = userRoot(uid).collection("exams").document(exam.id)
            path = ref.path
            upsertDocument(ref, exam.toFirestoreMap(uid, true), exam.toFirestoreMap(uid, false))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCloudExam(examId: String): Result<Unit> = deleteDoc("exams", examId)

    suspend fun syncFocusSession(session: FocusSessionEntity): Result<Unit> {
        var path = "users/unknown/focus_sessions/${session.id}"
        return try {
            val uid = requireUserId()
            val ref = userRoot(uid).collection("focus_sessions").document(session.id)
            path = ref.path
            if (!ref.get().await().exists()) {
                ref.set(session.toFirestoreMap(uid)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun syncNote(note: StudyNoteEntity): Result<Unit> {
        var path = "users/unknown/notes/${note.id}"
        return try {
            val uid = requireUserId()
            val ref = userRoot(uid).collection("notes").document(note.id)
            path = ref.path
            upsertDocument(ref, note.toFirestoreMap(uid, true), note.toFirestoreMap(uid, false))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCloudNote(noteId: String): Result<Unit> = deleteDoc("notes", noteId)

    suspend fun syncHoliday(holiday: HolidayOrBreakEntity): Result<Unit> {
        var path = "users/unknown/holidays/${holiday.id}"
        return try {
            val uid = requireUserId()
            val ref = userRoot(uid).collection("holidays").document(holiday.id)
            path = ref.path
            upsertDocument(ref, holiday.toFirestoreMap(uid, true), holiday.toFirestoreMap(uid, false))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCloudHoliday(holidayId: String): Result<Unit> = deleteDoc("holidays", holidayId)

    private suspend fun deleteDoc(collection: String, docId: String): Result<Unit> {
        var path = "users/unknown/$collection/$docId"
        return try {
            val uid = requireUserId()
            val ref = userRoot(uid).collection(collection).document(docId)
            path = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun backupAllToCloud(): Result<Int> {
        return try {
            val uid = requireUserId()
            var count = 0
            dao.getProfileOnce()?.let { profile ->
                syncProfile(profile).getOrThrow()
                count++
            }
            dao.getSubjectsOnce().forEach {
                syncSubject(it).getOrThrow()
                count++
            }
            dao.getLessonsOnce().forEach {
                syncLesson(it).getOrThrow()
                count++
            }
            dao.getScheduleBlocksOnce().forEach {
                syncScheduleBlock(it).getOrThrow()
                count++
            }
            dao.getExamsOnce().forEach {
                syncExam(it).getOrThrow()
                count++
            }
            dao.getFocusSessionsOnce().forEach {
                syncFocusSession(it).getOrThrow()
                count++
            }
            dao.getNotesOnce().forEach {
                syncNote(it).getOrThrow()
                count++
            }
            dao.getHolidaysOnce().forEach {
                syncHoliday(it).getOrThrow()
                count++
            }
            Result.success(count)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid ?: "unknown"}")
            Result.failure(e)
        }
    }

    suspend fun restoreFromCloud(): Result<Int> {
        var path = "users/unknown"
        return try {
            val uid = requireUserId()
            val root = userRoot(uid)
            path = root.path
            var count = 0

            val profileSnap = root.collection("profile").document("main").get().await()
            if (profileSnap.exists()) {
                profileSnap.toObject(FsStudentProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.let { p ->
                    val existingLocal = dao.getProfileOnce()
                    dao.upsertProfile(
                        StudentProfileEntity(
                            id = "main",
                            name = p.name,
                            educationalStage = p.educationalStage,
                            grade = p.grade,
                            school = p.school,
                            governorate = p.governorate,
                            studyGoal = p.studyGoal,
                            avatarUri = existingLocal?.avatarUri ?: "",
                            wakeTimeMinutes = p.wakeTimeMinutes.toInt(),
                            sleepTimeMinutes = p.sleepTimeMinutes.toInt(),
                            schoolStartMinutes = p.schoolStartMinutes.toInt(),
                            schoolEndMinutes = p.schoolEndMinutes.toInt(),
                            hasSchoolRoutine = p.schoolEndMinutes > p.schoolStartMinutes,
                            commuteMinutes = p.commuteMinutes.toInt(),
                            mealBreakMinutes = p.mealBreakMinutes.toInt(),
                            xp = p.xp.toInt(),
                            level = p.level.toInt(),
                            currentStreakDays = p.currentStreakDays.toInt(),
                            longestStreakDays = p.longestStreakDays.toInt(),
                            unlockedBadgesCsv = p.unlockedBadgesCsv
                        )
                    )
                    count++
                }
            }

            val subjects = root.collection("subjects").get().await()
                .toObjects(FsSubject::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .map { s ->
                    SubjectEntity(
                        id = s.id,
                        name = s.name,
                        teacherName = s.teacherName,
                        colorHex = s.colorHex,
                        difficulty = s.difficulty.toInt(),
                        priority = s.priority.toInt(),
                        studyMinutesLogged = s.studyMinutesLogged.toInt(),
                        notesSummary = s.notesSummary
                    )
                }
            if (subjects.isNotEmpty()) {
                dao.upsertSubjects(subjects)
                count += subjects.size
            }

            val lessons = root.collection("lessons").get().await()
                .toObjects(FsLesson::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .map { l ->
                    LessonEntity(
                        id = l.id,
                        subjectId = l.subjectId,
                        unitName = l.unitName,
                        chapterName = l.chapterName,
                        title = l.title,
                        partLabel = l.partLabel,
                        kind = LessonKind.fromRaw(l.kind).name,
                        status = LessonStatus.fromRaw(l.status).name,
                        estimatedMinutes = l.estimatedMinutes.toInt(),
                        actualMinutes = l.actualMinutes.toInt(),
                        pagesCount = l.pagesCount.toInt(),
                        questionsSolved = l.questionsSolved.toInt(),
                        comprehensionPercent = l.comprehensionPercent.toInt(),
                        difficultyOrImportance = l.difficultyOrImportance.toInt(),
                        delayReason = l.delayReason,
                        completionNotes = l.completionNotes,
                        startDateEpochDay = l.startDateEpochDay,
                        dueDateEpochDay = l.dueDateEpochDay,
                        nextReviewEpochDay = l.nextReviewEpochDay,
                        reviewStage = l.reviewStage.toInt()
                    )
                }
            if (lessons.isNotEmpty()) {
                dao.upsertLessons(lessons)
                count += lessons.size
            }

            val blocks = root.collection("schedule_blocks").get().await()
                .toObjects(FsScheduleBlock::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .map { b ->
                    ScheduleBlockEntity(
                        id = b.id,
                        dayOfWeek = b.dayOfWeek.toInt(),
                        specificDateEpochDay = b.specificDateEpochDay,
                        title = b.title,
                        category = BlockCategory.fromRaw(b.category).name,
                        subjectId = b.subjectId,
                        lessonId = b.lessonId,
                        startMinutes = b.startMinutes.toInt(),
                        endMinutes = b.endMinutes.toInt(),
                        fixed = b.fixed,
                        completed = b.completed,
                        orderIndex = b.orderIndex.toInt()
                    )
                }
            if (blocks.isNotEmpty()) {
                dao.upsertScheduleBlocks(blocks)
                count += blocks.size
            }

            val exams = root.collection("exams").get().await()
                .toObjects(FsExam::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .map { ex ->
                    ExamEntity(
                        id = ex.id,
                        title = ex.title,
                        subjectId = ex.subjectId,
                        examDateEpochDay = ex.examDateEpochDay,
                        timeMinutes = ex.timeMinutes.toInt(),
                        requiredParts = ex.requiredParts,
                        readinessPercent = ex.readinessPercent.toInt(),
                        achievedScore = ex.achievedScore,
                        maxScore = ex.maxScore,
                        revisionPlanSummary = ex.revisionPlanSummary,
                        reminderEnabled = ex.reminderEnabled
                    )
                }
            if (exams.isNotEmpty()) {
                dao.upsertExams(exams)
                count += exams.size
            }

            val sessions = root.collection("focus_sessions").get().await()
                .toObjects(FsFocusSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .map { fs ->
                    FocusSessionEntity(
                        id = fs.id,
                        subjectId = fs.subjectId,
                        lessonId = fs.lessonId,
                        durationMinutes = fs.durationMinutes.toInt(),
                        breakMinutes = fs.breakMinutes.toInt(),
                        mode = FocusModeType.fromRaw(fs.mode).name,
                        completedAtEpochMs = fs.completedAtEpochMs
                    )
                }
            if (sessions.isNotEmpty()) {
                dao.insertFocusSessions(sessions)
                count += sessions.size
            }

            val notes = root.collection("notes").get().await()
                .toObjects(FsStudyNote::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .map { n ->
                    StudyNoteEntity(
                        id = n.id,
                        subjectId = n.subjectId,
                        title = n.title,
                        content = n.content,
                        linkUrl = n.linkUrl,
                        attachmentType = AttachmentType.fromRaw(n.attachmentType).name,
                        important = n.important
                    )
                }
            if (notes.isNotEmpty()) {
                dao.upsertNotes(notes)
                count += notes.size
            }

            val holidays = root.collection("holidays").get().await()
                .toObjects(FsHoliday::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .map { h ->
                    HolidayOrBreakEntity(
                        id = h.id,
                        dateEpochDay = h.dateEpochDay,
                        title = h.title,
                        kind = HolidayKind.fromRaw(h.kind).name,
                        blockAllStudy = h.blockAllStudy,
                        startMinutes = h.startMinutes?.toInt(),
                        endMinutes = h.endMinutes?.toInt()
                    )
                }
            if (holidays.isNotEmpty()) {
                dao.upsertHolidays(holidays)
                count += holidays.size
            }

            Result.success(count)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAllCloudData(): Result<Unit> {
        var path = "users/unknown"
        return try {
            val uid = requireUserId()
            val root = userRoot(uid)
            path = root.path
            val collections = listOf(
                "profile", "subjects", "lessons", "schedule_blocks",
                "exams", "focus_sessions", "notes", "holidays"
            )
            for (col in collections) {
                val snap = root.collection(col).get().await()
                for (doc in snap.documents) {
                    doc.reference.delete().await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
