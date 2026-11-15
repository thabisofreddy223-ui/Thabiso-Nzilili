package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.LocalService
import com.example.data.model.ServiceReview
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

class ServicesRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun observePublishedServices(): Flow<List<LocalService>> {
        val path = "services"
        return db.collection("services")
            .whereEqualTo("isPublished", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(LocalService::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(
                        compareByDescending<LocalService> { it.isVerified }
                            .thenByDescending { it.averageRating }
                            .thenBy { it.businessName }
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

    fun observeReviews(serviceId: String): Flow<List<ServiceReview>> {
        val path = "services/$serviceId/reviews"
        return db.collection("services").document(serviceId).collection("reviews")
            .whereEqualTo("serviceId", serviceId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ServiceReview::class.java, ServerTimestampBehavior.ESTIMATE)
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

    suspend fun createService(
        ownerId: String,
        ownerName: String,
        businessName: String,
        category: String,
        city: String,
        suburb: String,
        businessPhone: String,
        servicesOffered: String
    ): Result<String> {
        require(ownerId.isNotBlank() && businessName.trim().length >= 2)
        val docRef = db.collection("services").document()
        val payload = mapOf(
            "ownerId" to ownerId,
            "ownerName" to ownerName.trim().ifBlank { "Business Owner" }.take(80),
            "businessName" to businessName.trim().take(100),
            "category" to category.trim().take(50),
            "city" to city.trim().take(60),
            "suburb" to suburb.trim().take(60),
            "businessPhone" to businessPhone.trim().take(30),
            "servicesOffered" to servicesOffered.trim().take(400),
            "isVerified" to false,
            "isPublished" to true,
            "ratingSum" to 0,
            "reviewCount" to 0,
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

    suspend fun submitReview(
        serviceId: String,
        authorId: String,
        authorName: String,
        rating: Int,
        comment: String
    ): Result<String> {
        require(serviceId.isNotBlank() && authorId.isNotBlank() && comment.isNotBlank())
        val safeRating = rating.coerceIn(1, 5)
        val serviceRef = db.collection("services").document(serviceId)
        val reviewRef = serviceRef.collection("reviews").document()
        return try {
            val batch = db.batch()
            batch.set(
                reviewRef,
                mapOf(
                    "serviceId" to serviceId,
                    "authorId" to authorId,
                    "authorName" to authorName.trim().ifBlank { "Community Member" }.take(80),
                    "rating" to safeRating,
                    "comment" to comment.trim().take(500),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                serviceRef,
                mapOf(
                    "ratingSum" to FieldValue.increment(safeRating.toLong()),
                    "reviewCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(reviewRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, reviewRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteService(serviceId: String): Result<Unit> {
        require(serviceId.isNotBlank())
        val docRef = db.collection("services").document(serviceId)
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
