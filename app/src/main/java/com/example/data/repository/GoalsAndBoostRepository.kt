package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.GameScoreRecord
import com.example.data.model.LeaderboardEntry
import com.example.data.model.PersonalGoal
import com.example.data.model.SavedQuote
import com.example.data.model.UserProfile
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class GoalsAndBoostRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                handleFirestoreError(firestoreError ?: error, OperationType.GET, path)
                throw error
            }
    }

    suspend fun ensureUserProfile(
        userId: String,
        displayName: String,
        email: String
    ): Result<Unit> {
        require(userId.isNotBlank())
        val docRef = db.collection("users").document(userId)
        return try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val safeName = displayName.trim().ifBlank { "LIFEHUB Member" }.take(80)
                val payload = buildMap<String, Any> {
                    put("userId", userId)
                    put("displayName", safeName)
                    if (email.isNotBlank()) put("email", email.trim().take(160))
                    put("currencyCode", "ZAR")
                    put("savingsTargetZar", 5000.0)
                    put("currentStreak", 0)
                    put("bestScore", 0)
                    put("totalGamePoints", 0)
                    put("isPremium", false)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateProfilePreferences(
        userId: String,
        displayName: String,
        savingsTargetZar: Double
    ): Result<Unit> {
        require(userId.isNotBlank() && displayName.isNotBlank())
        val docRef = db.collection("users").document(userId)
        return try {
            docRef.update(
                mapOf(
                    "displayName" to displayName.trim().take(80),
                    "savingsTargetZar" to savingsTargetZar.coerceAtLeast(0.0),
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

    fun observeGoals(userId: String): Flow<List<PersonalGoal>> {
        val path = "users/$userId/financial_goals"
        return db.collection("users").document(userId).collection("financial_goals")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PersonalGoal::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(
                        compareBy<PersonalGoal> { it.completed }
                            .thenByDescending { it.createdAt?.seconds ?: 0L }
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

    suspend fun addGoal(
        userId: String,
        title: String,
        category: String,
        targetValue: Double,
        currentValue: Double,
        unitLabel: String,
        deadlineIso: String
    ): Result<String> {
        require(userId.isNotBlank() && title.isNotBlank() && targetValue > 0.0)
        val docRef = db.collection("users").document(userId).collection("financial_goals").document()
        val safeCurrent = currentValue.coerceAtLeast(0.0)
        val payload = mapOf(
            "userId" to userId,
            "title" to title.trim().take(100),
            "category" to category.trim().take(50),
            "targetValue" to targetValue,
            "currentValue" to safeCurrent,
            "unitLabel" to unitLabel.trim().ifBlank { "ZAR" }.take(20),
            "deadlineIso" to deadlineIso.trim().take(20),
            "completed" to (safeCurrent >= targetValue),
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

    suspend fun updateGoalProgress(
        userId: String,
        goal: PersonalGoal,
        newCurrentValue: Double
    ): Result<Unit> {
        require(userId.isNotBlank() && goal.id.isNotBlank())
        val docRef = db.collection("users").document(userId)
            .collection("financial_goals").document(goal.id)
        val clamped = newCurrentValue.coerceAtLeast(0.0)
        return try {
            docRef.update(
                mapOf(
                    "currentValue" to clamped,
                    "completed" to (clamped >= goal.targetValue),
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

    suspend fun deleteGoal(userId: String, goalId: String): Result<Unit> {
        require(userId.isNotBlank() && goalId.isNotBlank())
        val docRef = db.collection("users").document(userId)
            .collection("financial_goals").document(goalId)
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

    fun observeGameScores(userId: String): Flow<List<GameScoreRecord>> {
        val path = "users/$userId/game_scores"
        return db.collection("users").document(userId).collection("game_scores")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(GameScoreRecord::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
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

    suspend fun recordGameCompletion(
        userId: String,
        displayName: String,
        gameId: String,
        difficulty: String,
        score: Int,
        durationSeconds: Int,
        shareOnLeaderboard: Boolean
    ): Result<Unit> {
        require(userId.isNotBlank())
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(yesterdayCal.time)

        val scoreRef = db.collection("users").document(userId).collection("game_scores").document()
        val profileRef = db.collection("users").document(userId)
        val leaderboardRef = db.collection("leaderboard").document(userId)

        return try {
            val scorePayload = mapOf(
                "userId" to userId,
                "gameId" to gameId,
                "difficulty" to difficulty,
                "score" to score.coerceIn(0, 10000),
                "dateIso" to today,
                "durationSeconds" to durationSeconds.coerceIn(0, 3600),
                "createdAt" to FieldValue.serverTimestamp()
            )
            scoreRef.set(scorePayload).await()

            val profileSnap = profileRef.get().await()
            val prevStreak = (profileSnap.getLong("currentStreak") ?: 0L).toInt()
            val prevBest = (profileSnap.getLong("bestScore") ?: 0L).toInt()
            val prevTotal = (profileSnap.getLong("totalGamePoints") ?: 0L).toInt()
            val lastPlayed = profileSnap.getString("lastPlayedDate") ?: ""

            val nextStreak = when (lastPlayed) {
                today -> prevStreak.coerceAtLeast(1)
                yesterday -> prevStreak + 1
                else -> 1
            }
            val nextBest = maxOf(prevBest, score)
            val nextTotal = prevTotal + score

            if (profileSnap.exists()) {
                profileRef.update(
                    mapOf(
                        "currentStreak" to nextStreak,
                        "bestScore" to nextBest,
                        "totalGamePoints" to nextTotal,
                        "lastPlayedDate" to today,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val safeName = displayName.trim().ifBlank { "LIFEHUB Player" }.take(80)
            leaderboardRef.set(
                mapOf(
                    "userId" to userId,
                    "displayName" to safeName,
                    "totalPoints" to nextTotal,
                    "currentStreak" to nextStreak,
                    "bestScore" to nextBest,
                    "isPublic" to shareOnLeaderboard,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, scoreRef.path)
            Result.failure(e)
        }
    }

    fun observeLeaderboard(): Flow<List<LeaderboardEntry>> {
        val path = "leaderboard"
        return db.collection("leaderboard")
            .whereEqualTo("isPublic", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(LeaderboardEntry::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.totalPoints }
                    .take(25)
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

    fun observeSavedQuotes(userId: String): Flow<List<SavedQuote>> {
        val path = "users/$userId/saved_quotes"
        return db.collection("users").document(userId).collection("saved_quotes")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(SavedQuote::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
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

    suspend fun toggleSaveQuote(
        userId: String,
        quoteId: String,
        text: String,
        author: String,
        category: String,
        currentlySaved: Boolean
    ): Result<Unit> {
        require(userId.isNotBlank() && quoteId.isNotBlank())
        val docRef = db.collection("users").document(userId)
            .collection("saved_quotes").document(quoteId)
        return try {
            if (currentlySaved) {
                docRef.delete().await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to userId,
                        "quoteId" to quoteId,
                        "text" to text.take(400),
                        "author" to author.take(80),
                        "category" to category.take(40),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }
}
