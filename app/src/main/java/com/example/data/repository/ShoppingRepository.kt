package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ShoppingItem
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

class ShoppingRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun observeShoppingItems(userId: String): Flow<List<ShoppingItem>> {
        val path = "users/$userId/shopping_items"
        return db.collection("users").document(userId).collection("shopping_items")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ShoppingItem::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(
                        compareBy<ShoppingItem> { it.purchased }
                            .thenBy { it.shopName }
                            .thenBy { it.name }
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

    suspend fun addShoppingItem(
        userId: String,
        name: String,
        quantity: Double,
        unit: String,
        estimatedPrice: Double,
        shopName: String,
        category: String,
        isFrequent: Boolean
    ): Result<String> {
        require(userId.isNotBlank() && name.isNotBlank())
        require(quantity > 0.0)
        val docRef = db.collection("users").document(userId).collection("shopping_items").document()
        val payload = mapOf(
            "userId" to userId,
            "name" to name.trim().take(100),
            "quantity" to quantity,
            "unit" to unit.trim().ifBlank { "x" }.take(20),
            "estimatedPrice" to estimatedPrice.coerceAtLeast(0.0),
            "shopName" to shopName.trim().ifBlank { "General" }.take(60),
            "category" to category.trim().ifBlank { "Groceries" }.take(50),
            "purchased" to false,
            "isFrequent" to isFrequent,
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

    suspend fun togglePurchased(userId: String, item: ShoppingItem): Result<Unit> {
        require(userId.isNotBlank() && item.id.isNotBlank())
        val docRef = db.collection("users").document(userId)
            .collection("shopping_items").document(item.id)
        return try {
            docRef.update(
                mapOf(
                    "purchased" to !item.purchased,
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

    suspend fun toggleFrequent(userId: String, item: ShoppingItem): Result<Unit> {
        require(userId.isNotBlank() && item.id.isNotBlank())
        val docRef = db.collection("users").document(userId)
            .collection("shopping_items").document(item.id)
        return try {
            docRef.update(
                mapOf(
                    "isFrequent" to !item.isFrequent,
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

    suspend fun resetPurchasedForFrequent(userId: String, item: ShoppingItem): Result<Unit> {
        require(userId.isNotBlank() && item.id.isNotBlank())
        val docRef = db.collection("users").document(userId)
            .collection("shopping_items").document(item.id)
        return try {
            docRef.update(
                mapOf(
                    "purchased" to false,
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

    suspend fun deleteShoppingItem(userId: String, itemId: String): Result<Unit> {
        require(userId.isNotBlank() && itemId.isNotBlank())
        val docRef = db.collection("users").document(userId)
            .collection("shopping_items").document(itemId)
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
