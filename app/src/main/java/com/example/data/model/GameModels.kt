package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val photoUrl: String? = null,
    val wins: Long = 0L,
    val losses: Long = 0L,
    val gamesPlayed: Long = 0L,
    val trophies: Long = 100L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MatchRoom(
    @DocumentId val id: String = "",
    val roomId: String = "",
    val hostId: String = "",
    val hostName: String = "",
    val status: String = "waiting", // "waiting", "playing", "finished"
    val playerCount: Long = 2L,
    val playerUids: List<String> = emptyList(),
    val playerNames: List<String> = emptyList(),
    val playerColors: List<String> = emptyList(), // "RED", "BLUE", "GREEN", "YELLOW"
    val playerTokens: List<Long> = emptyList(), // 2 tokens per player: [p1_t1, p1_t2, p2_t1, p2_t2, ...]
    val turnIndex: Long = 0L,
    val turnUid: String = "",
    val diceValue: Long = 1L,
    val diceRolled: Boolean = false,
    val lastAction: String = "Game created",
    val winnerId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val messageId: String = "",
    val roomId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val wins: Long = 0L,
    val gamesPlayed: Long = 0L,
    val trophies: Long = 100L,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

object BoardConstants {
    const val BOARD_SIZE = 100
    const val TOKENS_PER_PLAYER = 2

    // Ladders: bottom tile -> top tile
    val LADDERS = mapOf(
        4 to 14,
        9 to 31,
        20 to 38,
        28 to 84,
        40 to 59,
        51 to 67,
        63 to 81,
        71 to 91
    )

    // Snakes: head tile -> tail tile
    val SNAKES = mapOf(
        17 to 7,
        54 to 34,
        62 to 19,
        64 to 60,
        87 to 24,
        93 to 73,
        95 to 75,
        99 to 78
    )

    // Safe / Star tiles (immune to Ludo knockouts)
    val SAFE_TILES = setOf(1, 9, 14, 21, 28, 38, 51, 67, 71, 84, 91)

    val PLAYER_COLORS = listOf("RED", "BLUE", "GREEN", "YELLOW")

    fun getTileCoordinates(tileNumber: Int): Pair<Int, Int> {
        if (tileNumber <= 0) return Pair(-1, -1)
        val clamped = tileNumber.coerceIn(1, 100)
        val zeroIndexed = clamped - 1
        val row = zeroIndexed / 10 // 0 to 9 from bottom
        val col = zeroIndexed % 10
        // Alternating serpentine pattern
        val actualCol = if (row % 2 == 0) col else 9 - col
        // row 0 is at bottom (y = 9), row 9 is at top (y = 0)
        val gridY = 9 - row
        val gridX = actualCol
        return Pair(gridX, gridY)
    }
}
