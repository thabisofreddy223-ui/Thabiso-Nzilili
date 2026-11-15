package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LifeHubRepositoriesRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun financeRepository_ownerCanAddAndObserveTransaction() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = FinanceRepository(firestore)

        val addResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.addTransaction(
                userId = uid,
                title = "Prepaid Electricity",
                amount = 350.0,
                type = "EXPENSE",
                category = "Electricity",
                dateIso = "2026-11-15"
            )
        }
        assertTrue(addResult.isSuccess)
        val txId = addResult.getOrThrow()

        val list = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeTransactions(uid).first { items -> items.any { it.id == txId } }
        }
        assertTrue(list.any { it.id == txId && it.amount == 350.0 })
    }

    @Test
    fun financeRepository_crossUserWrite_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        signInTestUser(BOB_EMAIL)
        val repo = FinanceRepository(firestore)

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.addTransaction(
                userId = aliceUid,
                title = "Unauthorized",
                amount = 100.0,
                type = "EXPENSE",
                category = "Other",
                dateIso = "2026-11-15"
            )
        }
        assertTrue(result.isFailure)
        val firestoreException = result.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    @Test
    fun plannerRepository_unauthenticatedObserve_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = PlannerRepository(firestore)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeTasks("some_user_123").first()
            }
        } catch (e: Throwable) {
            caught = e
        }
        assertNotNull(caught)
        val fsEx = caught?.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException, got $caught", fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun servicesRepository_createAndSubmitGenuineReview_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = ServicesRepository(firestore)

        val srvResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createService(
                ownerId = aliceUid,
                ownerName = "Thabiso F.",
                businessName = "Soweto Spark Electricians",
                category = "Electricians",
                city = "Johannesburg",
                suburb = "Orlando West",
                businessPhone = "011-555-0199",
                servicesOffered = "COC certificates, DB board repairs, solar backup"
            )
        }
        assertTrue(srvResult.isSuccess)
        val serviceId = srvResult.getOrThrow()

        val bobUid = signInTestUser(BOB_EMAIL)
        val reviewResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.submitReview(
                serviceId = serviceId,
                authorId = bobUid,
                authorName = "Lerato M.",
                rating = 5,
                comment = "Arrived on time and solved our tripping breaker quickly."
            )
        }
        assertTrue(reviewResult.isSuccess)

        val services = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observePublishedServices().first { list -> list.any { it.id == serviceId && it.reviewCount == 1 } }
        }
        val found = services.first { it.id == serviceId }
        assertEquals(5, found.ratingSum)
        assertEquals(1, found.reviewCount)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_lifehub@test.com"
        const val BOB_EMAIL = "bob_lifehub@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
