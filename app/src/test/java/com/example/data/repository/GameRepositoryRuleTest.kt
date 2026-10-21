package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class GameRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createRoom_authenticatedHost_createsRoomSuccessfully() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repo = GameRepository(firestore)

        val roomId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createRoom("AliceHost", 2)
        }
        assertTrue(roomId.isNotEmpty())

        val room = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeRoom(roomId).first { it != null }
        }
        assertNotNull(room)
        assertEquals("AliceHost", room?.hostName)
        assertEquals(2L, room?.playerCount)
    }

    @Test
    fun joinRoom_authenticatedGuest_joinsSuccessfully() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val hostRepo = GameRepository(firestore)
        val roomId = withTimeout(DEFAULT_TIMEOUT_MS) {
            hostRepo.createRoom("AliceHost", 2)
        }

        val bobUid = signInTestUser(BOB_EMAIL)
        val guestRepo = GameRepository(firestore)
        val joined = withTimeout(DEFAULT_TIMEOUT_MS) {
            guestRepo.joinRoom(roomId, "BobGuest")
        }
        assertTrue(joined)

        val room = withTimeout(FLOW_TIMEOUT_MS) {
            guestRepo.observeRoom(roomId).first { it?.playerUids?.contains(bobUid) == true }
        }
        assertEquals("playing", room?.status)
    }

    @Test
    fun sendMessage_authenticatedPlayer_sendsMessage() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repo = GameRepository(firestore)
        val roomId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createRoom("AliceHost", 2)
        }

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.sendMessage(roomId, "Hello World!", "AliceHost")
        }

        val messages = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeMessages(roomId).first { it.isNotEmpty() }
        }
        assertEquals(1, messages.size)
        assertEquals("Hello World!", messages.first().text)
    }

    @Test
    fun createRoom_unauthenticated_fails() = runBlocking {
        auth.signOut()
        val repo = GameRepository(firestore)
        try {
            repo.createRoom("Anonymous", 2)
            fail("Expected IllegalStateException when unauthenticated")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message?.contains("authenticated") == true)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@ludoladders.test"
        const val BOB_EMAIL = "bob@ludoladders.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
