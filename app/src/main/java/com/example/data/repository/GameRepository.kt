package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.BoardConstants
import com.example.data.model.ChatMessage
import com.example.data.model.LeaderboardEntry
import com.example.data.model.MatchRoom
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.random.Random

class GameRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be authenticated before performing game action")
    }

    fun observeRoom(roomId: String): Flow<MatchRoom?> {
        val docRef = db.collection("match_rooms").document(roomId)
        return docRef.snapshots()
            .map { it.toObject(MatchRoom::class.java) }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.GET, docRef.path)
                throw e
            }
    }

    fun observeMessages(roomId: String): Flow<List<ChatMessage>> {
        val collRef = db.collection("match_rooms").document(roomId).collection("messages")
        return collRef.orderBy("createdAt", Query.Direction.ASCENDING).limit(50)
            .snapshots()
            .map { it.toObjects(ChatMessage::class.java) }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, collRef.path)
                throw e
            }
    }

    fun observeLeaderboard(): Flow<List<LeaderboardEntry>> {
        val collRef = db.collection("leaderboards")
        return collRef.orderBy("trophies", Query.Direction.DESCENDING).limit(50)
            .snapshots()
            .map { it.toObjects(LeaderboardEntry::class.java) }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, collRef.path)
                throw e
            }
    }

    suspend fun createRoom(hostName: String, playerCount: Int = 2): String {
        val uid = requireUserId()
        val roomId = "room_${UUID.randomUUID().toString().take(6)}"
        val docRef = db.collection("match_rooms").document(roomId)

        val initialTokens = List(playerCount * BoardConstants.TOKENS_PER_PLAYER) { 0L }

        val roomData = mapOf(
            "roomId" to roomId,
            "hostId" to uid,
            "hostName" to hostName,
            "status" to "waiting",
            "playerCount" to playerCount.toLong(),
            "playerUids" to listOf(uid),
            "playerNames" to listOf(hostName),
            "playerColors" to listOf(BoardConstants.PLAYER_COLORS[0]),
            "playerTokens" to initialTokens,
            "turnIndex" to 0L,
            "turnUid" to uid,
            "diceValue" to 1L,
            "diceRolled" to false,
            "lastAction" to "Waiting for players to join...",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(roomData).await()
            return roomId
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun joinRoom(roomId: String, playerName: String): Boolean {
        val uid = requireUserId()
        val docRef = db.collection("match_rooms").document(roomId)
        try {
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) return false
            val room = snapshot.toObject(MatchRoom::class.java) ?: return false

            if (room.playerUids.contains(uid)) return true // already in room
            if (room.status != "waiting" || room.playerUids.size >= room.playerCount) return false

            val newIndex = room.playerUids.size
            val updatedUids = room.playerUids + uid
            val updatedNames = room.playerNames + playerName
            val updatedColors = room.playerColors + BoardConstants.PLAYER_COLORS[newIndex % BoardConstants.PLAYER_COLORS.size]

            val isNowFull = updatedUids.size.toLong() == room.playerCount
            val newStatus = if (isNowFull) "playing" else "waiting"
            val lastAction = if (isNowFull) "Match started! ${room.playerNames.first()}'s turn" else "$playerName joined the lobby"

            val updatePayload = mapOf(
                "playerUids" to updatedUids,
                "playerNames" to updatedNames,
                "playerColors" to updatedColors,
                "status" to newStatus,
                "lastAction" to lastAction,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            docRef.update(updatePayload).await()
            return true
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun findOrCreateQuickMatch(playerName: String): String {
        val uid = requireUserId()
        val collRef = db.collection("match_rooms")
        try {
            val querySnapshot = collRef.whereEqualTo("status", "waiting")
                .limit(10)
                .get()
                .await()

            for (doc in querySnapshot.documents) {
                val room = doc.toObject(MatchRoom::class.java)
                if (room != null && !room.playerUids.contains(uid) && room.playerUids.size < room.playerCount) {
                    val joined = joinRoom(room.roomId, playerName)
                    if (joined) return room.roomId
                }
            }
            // None found or join failed, create a new 2-player room
            return createRoom(playerName, playerCount = 2)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, collRef.path)
            throw e
        }
    }

    suspend fun rollDice(roomId: String) {
        val uid = requireUserId()
        val docRef = db.collection("match_rooms").document(roomId)
        try {
            val snapshot = docRef.get().await()
            val room = snapshot.toObject(MatchRoom::class.java) ?: return

            if (room.status != "playing" || room.turnUid != uid || room.diceRolled) return

            val dice = Random.nextInt(1, 7).toLong()
            val currentTurn = room.turnIndex.toInt()
            val startTokenIdx = currentTurn * BoardConstants.TOKENS_PER_PLAYER
            val endTokenIdx = startTokenIdx + BoardConstants.TOKENS_PER_PLAYER
            val tokens = room.playerTokens.toMutableList()

            // Check if player has any valid move with this dice value
            var hasValidMove = false
            for (idx in startTokenIdx until endTokenIdx) {
                val pos = tokens.getOrElse(idx) { 0L }
                if (pos == 0L && dice == 6L) hasValidMove = true
                if (pos > 0L && pos + dice <= 100L) hasValidMove = true
            }

            if (!hasValidMove) {
                // Pass turn immediately to next player
                val nextTurnIndex = ((currentTurn + 1) % room.playerUids.size).toLong()
                val nextTurnUid = room.playerUids[nextTurnIndex.toInt()]
                docRef.update(
                    mapOf(
                        "diceValue" to dice,
                        "diceRolled" to false,
                        "turnIndex" to nextTurnIndex,
                        "turnUid" to nextTurnUid,
                        "lastAction" to "${room.playerNames[currentTurn]} rolled $dice (No valid moves, turn skipped)",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "diceValue" to dice,
                        "diceRolled" to true,
                        "lastAction" to "${room.playerNames[currentTurn]} rolled a $dice! Select a token to move.",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun moveToken(roomId: String, relativeTokenIndex: Int) {
        val uid = requireUserId()
        val docRef = db.collection("match_rooms").document(roomId)
        try {
            val snapshot = docRef.get().await()
            val room = snapshot.toObject(MatchRoom::class.java) ?: return

            if (room.status != "playing" || room.turnUid != uid || !room.diceRolled) return

            val currentTurn = room.turnIndex.toInt()
            val dice = room.diceValue
            val tokens = room.playerTokens.toMutableList()
            val globalTokenIndex = currentTurn * BoardConstants.TOKENS_PER_PLAYER + relativeTokenIndex

            if (globalTokenIndex >= tokens.size) return
            val currentPos = tokens[globalTokenIndex]

            // Validate move
            if (currentPos == 0L && dice != 6L) return // Must roll 6 to leave base
            if (currentPos > 0L && currentPos + dice > 100L) return // Exact or bounce

            var newPos = if (currentPos == 0L) 1L else currentPos + dice
            var actionDescription = "${room.playerNames[currentTurn]} moved to tile $newPos"
            var extraTurn = (dice == 6L)

            // Check Ladder
            val ladderTarget = BoardConstants.LADDERS[newPos.toInt()]
            if (ladderTarget != null) {
                newPos = ladderTarget.toLong()
                actionDescription = "${room.playerNames[currentTurn]} climbed a 🪜 ladder up to $newPos!"
            }

            // Check Snake
            val snakeTarget = BoardConstants.SNAKES[newPos.toInt()]
            if (snakeTarget != null) {
                newPos = snakeTarget.toLong()
                actionDescription = "🐍 Oh no! Snake bit ${room.playerNames[currentTurn]} down to $newPos!"
            }

            // Check Ludo Knockout on opponent tokens
            if (!BoardConstants.SAFE_TILES.contains(newPos.toInt())) {
                for (otherIdx in tokens.indices) {
                    val otherPlayerIndex = otherIdx / BoardConstants.TOKENS_PER_PLAYER
                    if (otherPlayerIndex != currentTurn && tokens[otherIdx] == newPos) {
                        tokens[otherIdx] = 0L // Knocked back to base!
                        extraTurn = true // Knocking an opponent grants an extra roll in Ludo!
                        actionDescription = "💥 ${room.playerNames[currentTurn]} knocked ${room.playerNames[otherPlayerIndex]}'s token back to base!"
                        break
                    }
                }
            }

            tokens[globalTokenIndex] = newPos

            // Check Win Condition: Did this player bring all their tokens to 100?
            val startTokenIdx = currentTurn * BoardConstants.TOKENS_PER_PLAYER
            val endTokenIdx = startTokenIdx + BoardConstants.TOKENS_PER_PLAYER
            val hasWon = (startTokenIdx until endTokenIdx).all { tokens[it] == 100L }

            val updatePayload = mutableMapOf<String, Any>(
                "playerTokens" to tokens,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            if (hasWon) {
                updatePayload["status"] = "finished"
                updatePayload["winnerId"] = uid
                updatePayload["lastAction"] = "🏆 ${room.playerNames[currentTurn]} won the match!"
                updatePayload["diceRolled"] = false
            } else if (extraTurn) {
                // Same player rolls again
                updatePayload["diceRolled"] = false
                updatePayload["lastAction"] = "$actionDescription (Roll again!)"
            } else {
                // Next player's turn
                val nextTurnIndex = ((currentTurn + 1) % room.playerUids.size).toLong()
                val nextTurnUid = room.playerUids[nextTurnIndex.toInt()]
                updatePayload["turnIndex"] = nextTurnIndex
                updatePayload["turnUid"] = nextTurnUid
                updatePayload["diceRolled"] = false
                updatePayload["lastAction"] = actionDescription
            }

            docRef.update(updatePayload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun sendMessage(roomId: String, text: String, senderName: String) {
        val uid = requireUserId()
        val collRef = db.collection("match_rooms").document(roomId).collection("messages")
        val messageId = "msg_${UUID.randomUUID().toString().take(8)}"
        val messageData = mapOf(
            "messageId" to messageId,
            "roomId" to roomId,
            "senderId" to uid,
            "senderName" to senderName,
            "text" to text.take(250),
            "createdAt" to FieldValue.serverTimestamp()
        )

        try {
            collRef.document(messageId).set(messageData).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, collRef.path)
            throw e
        }
    }
}
