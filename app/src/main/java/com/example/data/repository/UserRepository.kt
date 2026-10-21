package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.UserProfile
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class UserRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be authenticated before accessing profile")
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val docRef = db.collection("users").document(userId)
        return docRef.snapshots()
            .map { it.toObject(UserProfile::class.java) }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.GET, docRef.path)
                throw e
            }
    }

    suspend fun getOrCreateUserProfile(userId: String, displayName: String, photoUrl: String?): UserProfile {
        val docRef = db.collection("users").document(userId)
        val snapshot = docRef.get().await()
        if (snapshot.exists()) {
            val profile = snapshot.toObject(UserProfile::class.java)
            if (profile != null) return profile
        }

        val initialProfile = mapOf(
            "userId" to userId,
            "username" to displayName.ifBlank { "Player_${userId.take(5)}" },
            "photoUrl" to photoUrl,
            "wins" to 0L,
            "losses" to 0L,
            "gamesPlayed" to 0L,
            "trophies" to 100L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        try {
            docRef.set(initialProfile).await()
            val created = docRef.get().await().toObject(UserProfile::class.java)
            return created ?: UserProfile(userId = userId, username = displayName)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun recordMatchResult(won: Boolean) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val leaderRef = db.collection("leaderboards").document(uid)

        try {
            val userSnap = docRef.get().await()
            val user = userSnap.toObject(UserProfile::class.java) ?: return

            val newWins = if (won) user.wins + 1 else user.wins
            val newLosses = if (!won) user.losses + 1 else user.losses
            val newPlayed = user.gamesPlayed + 1
            val deltaTrophies = if (won) 25L else -10L
            val newTrophies = (user.trophies + deltaTrophies).coerceAtLeast(0L)

            val updatePayload = mapOf(
                "wins" to newWins,
                "losses" to newLosses,
                "gamesPlayed" to newPlayed,
                "trophies" to newTrophies,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updatePayload).await()

            // Update public leaderboard entry
            val leaderboardPayload = mapOf(
                "userId" to uid,
                "username" to user.username,
                "wins" to newWins,
                "gamesPlayed" to newPlayed,
                "trophies" to newTrophies,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            leaderRef.set(leaderboardPayload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }
}
