package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.PlannerTask
import com.example.data.model.SmartReminder
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class PlannerRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun observeTasks(userId: String): Flow<List<PlannerTask>> {
        val path = "users/$userId/tasks"
        return db.collection("users").document(userId).collection("tasks")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PlannerTask::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(
                        compareBy<PlannerTask> { it.completed }
                            .thenBy { it.dueDateIso }
                            .thenBy { priorityRank(it.priority) }
                    )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun addTask(
        userId: String,
        title: String,
        priority: String,
        dueDateIso: String,
        dueTime: String,
        category: String
    ): Result<String> {
        require(userId.isNotBlank() && title.isNotBlank())
        val docRef = db.collection("users").document(userId).collection("tasks").document()
        val payload = mapOf(
            "userId" to userId,
            "title" to title.trim().take(120),
            "priority" to priority,
            "dueDateIso" to dueDateIso.trim().take(20),
            "dueTime" to dueTime.trim().take(10),
            "category" to category.trim().take(40),
            "completed" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleTaskCompleted(userId: String, task: PlannerTask): Result<Unit> {
        require(userId.isNotBlank() && task.id.isNotBlank())
        val docRef = db.collection("users").document(userId).collection("tasks").document(task.id)
        return try {
            docRef.update(
                mapOf(
                    "completed" to !task.completed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteTask(userId: String, taskId: String): Result<Unit> {
        require(userId.isNotBlank() && taskId.isNotBlank())
        val docRef = db.collection("users").document(userId).collection("tasks").document(taskId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeReminders(userId: String): Flow<List<SmartReminder>> {
        val path = "users/$userId/reminders"
        return db.collection("users").document(userId).collection("reminders")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(SmartReminder::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(
                        compareBy<SmartReminder> { it.completed }
                            .thenBy { it.dueDateTimeIso }
                    )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun addReminder(
        userId: String,
        title: String,
        category: String,
        dueDateTimeIso: String,
        recurrence: String,
        amountZar: Double,
        notificationsEnabled: Boolean
    ): Result<String> {
        require(userId.isNotBlank() && title.isNotBlank())
        val docRef = db.collection("users").document(userId).collection("reminders").document()
        val payload = mapOf(
            "userId" to userId,
            "title" to title.trim().take(120),
            "category" to category.trim().take(40),
            "dueDateTimeIso" to dueDateTimeIso.trim().take(30),
            "recurrence" to recurrence,
            "amountZar" to amountZar.coerceAtLeast(0.0),
            "completed" to false,
            "notificationsEnabled" to notificationsEnabled,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleReminderCompleted(userId: String, reminder: SmartReminder): Result<Unit> {
        require(userId.isNotBlank() && reminder.id.isNotBlank())
        val docRef = db.collection("users").document(userId).collection("reminders").document(reminder.id)
        return try {
            docRef.update(
                mapOf(
                    "completed" to !reminder.completed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteReminder(userId: String, reminderId: String): Result<Unit> {
        require(userId.isNotBlank() && reminderId.isNotBlank())
        val docRef = db.collection("users").document(userId).collection("reminders").document(reminderId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    private fun priorityRank(priority: String): Int = when (priority) {
        "HIGH" -> 0
        "MEDIUM" -> 1
        else -> 2
    }
}
