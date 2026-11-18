package com.example.data.cloud

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class DailyTasksRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            throw IllegalStateException("يجب تسجيل الدخول بحساب Google لعرض وحفظ مهام اليوم.")
        }
        return uid
    }

    private fun tasksCollection(uid: String) =
        db.collection("users").document(uid).collection("daily_tasks")

    fun observeTasksForDate(uid: String, dateEpochDay: Long): Flow<List<DailyTask>> = flow {
        if (uid.isBlank() || auth.currentUser?.uid != uid) {
            throw IllegalStateException("يجب تسجيل الدخول بحساب Google لعرض مهام اليوم.")
        }
        val path = "users/$uid/daily_tasks"
        emitAll(
            tasksCollection(uid)
                .whereEqualTo("dateEpochDay", dateEpochDay)
                .snapshots()
                .map { snapshot ->
                    snapshot
                        .toObjects(DailyTask::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(
                            compareBy<DailyTask> { it.completed }
                                .thenByDescending { it.priority }
                                .thenBy { it.scheduledStartMinutes ?: Long.MAX_VALUE }
                                .thenBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
                        )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addTask(task: DailyTask): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/daily_tasks"
        return try {
            val uid = requireUserId()
            val payload = task.toCreateMap(uid)
            val docRef = tasksCollection(uid).document()
            docPath = docRef.path
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateTask(task: DailyTask): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/daily_tasks/${task.id}"
        return try {
            val uid = requireUserId()
            require(task.id.isNotBlank()) { "معرف المهمة غير صالح للتحديث." }
            val payload = task.toUpdateMap()
            val docRef = tasksCollection(uid).document(task.id)
            docPath = docRef.path
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleTaskCompleted(task: DailyTask): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/daily_tasks/${task.id}"
        return try {
            val uid = requireUserId()
            require(task.id.isNotBlank()) { "معرف المهمة غير صالح للتحديث." }
            val newCompleted = !task.completed
            val docRef = tasksCollection(uid).document(task.id)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "completed" to newCompleted,
                    "completedAtEpochMs" to if (newCompleted) System.currentTimeMillis() else FieldValue.delete(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteTask(taskId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/daily_tasks/$taskId"
        return try {
            val uid = requireUserId()
            require(taskId.isNotBlank()) { "معرف المهمة غير صالح للحذف." }
            val docRef = tasksCollection(uid).document(taskId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
