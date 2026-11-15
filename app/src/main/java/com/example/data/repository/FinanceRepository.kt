package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.BudgetLimit
import com.example.data.model.MoneyTransaction
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

class FinanceRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun observeTransactions(userId: String): Flow<List<MoneyTransaction>> {
        val path = "users/$userId/transactions"
        return db.collection("users").document(userId).collection("transactions")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(MoneyTransaction::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(
                        compareByDescending<MoneyTransaction> { it.dateIso }
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

    suspend fun addTransaction(
        userId: String,
        title: String,
        amount: Double,
        type: String,
        category: String,
        dateIso: String,
        notes: String = ""
    ): Result<String> {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        require(title.isNotBlank()) { "Transaction title is required" }
        require(amount > 0.0) { "Amount must be greater than R 0.00" }
        val docRef = db.collection("users").document(userId).collection("transactions").document()
        val payload = mapOf(
            "userId" to userId,
            "title" to title.trim().take(100),
            "amount" to amount,
            "type" to type,
            "category" to category.trim().take(50),
            "dateIso" to dateIso.trim().take(20),
            "notes" to notes.trim().take(300),
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

    suspend fun deleteTransaction(userId: String, transactionId: String): Result<Unit> {
        require(userId.isNotBlank() && transactionId.isNotBlank())
        val docRef = db.collection("users").document(userId)
            .collection("transactions").document(transactionId)
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

    fun observeBudgets(userId: String): Flow<List<BudgetLimit>> {
        val path = "users/$userId/budgets"
        return db.collection("users").document(userId).collection("budgets")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(BudgetLimit::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.category }
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

    suspend fun upsertBudget(
        userId: String,
        category: String,
        limitAmount: Double,
        period: String,
        existingId: String? = null
    ): Result<String> {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        require(limitAmount > 0.0) { "Budget limit must be greater than R 0" }
        val col = db.collection("users").document(userId).collection("budgets")
        val docRef = if (existingId.isNullOrBlank()) col.document() else col.document(existingId)
        return try {
            if (existingId.isNullOrBlank()) {
                val payload = mapOf(
                    "userId" to userId,
                    "category" to category.trim().take(50),
                    "limitAmount" to limitAmount,
                    "period" to period,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val payload = mapOf(
                    "category" to category.trim().take(50),
                    "limitAmount" to limitAmount,
                    "period" to period,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(payload).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteBudget(userId: String, budgetId: String): Result<Unit> {
        require(userId.isNotBlank() && budgetId.isNotBlank())
        val docRef = db.collection("users").document(userId).collection("budgets").document(budgetId)
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
}
