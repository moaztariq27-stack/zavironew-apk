package com.example.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FirebaseRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun createRepo(): FirebaseRepository {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return FirebaseRepository(firestore, context)
    }

    @Test
    fun createOrder_authenticatedCustomer_createsDocumentAndReturnsId() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = createRepo()

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createOrderRemote(
                customerName = "Alice Khan",
                phone = "0300-1234567",
                address = "Gulberg III, Lahore"
            )
        }
        assertTrue("Expected order creation to succeed: ${createResult.exceptionOrNull()}", createResult.isSuccess)
        val orderId = createResult.getOrThrow()
        assertTrue(orderId.isNotEmpty())
    }

    @Test
    fun getUserOrders_authenticatedOwner_returnsMatchingOrders() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = createRepo()
        val orderId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createOrderRemote(
                customerName = "Alice Khan",
                phone = "0300-1234567",
                address = "Gulberg III, Lahore"
            ).getOrThrow()
        }

        val ordersResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.getUserOrdersRemote() }
        assertTrue(ordersResult.isSuccess)
        assertTrue(ordersResult.getOrThrow().contains(orderId))
    }

    @Test
    fun observeCustomerOrders_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = createRepo()
        val orderId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createOrderRemote(
                customerName = "Alice Khan",
                phone = "0300-1234567",
                address = "Gulberg III, Lahore"
            ).getOrThrow()
        }

        val emittedOrderIds = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCustomerOrdersRemote().first { list -> list.contains(orderId) }
        }
        assertTrue(emittedOrderIds.contains(orderId))
    }

    @Test
    fun getOrderById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = createRepo()
        val orderId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createOrderRemote(
                customerName = "Alice Khan",
                phone = "0300-1234567",
                address = "Gulberg III, Lahore"
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = createRepo()
        val bobResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getOrderByIdRemote(orderId)
        }
        assertTrue("Cross-user order access must fail", bobResult.isFailure)
        val exception = bobResult.exceptionOrNull() as? FirebaseFirestoreException
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, exception?.code)
    }

    @Test
    fun observeCustomerOrders_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = createRepo()

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeCustomerOrdersRemote().first()
            }
            fail("Unauthenticated observation must fail with PERMISSION_DENIED")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@test.com"
        const val BOB_EMAIL = "bob@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
