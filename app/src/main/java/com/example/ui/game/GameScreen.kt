package com.example.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BoardConstants
import com.example.data.model.ChatMessage
import com.example.data.model.MatchRoom
import com.example.data.repository.GameRepository
import com.example.data.repository.UserRepository
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GameBackground
import com.example.ui.theme.GameCard
import com.example.ui.theme.GameCardBorder
import com.example.ui.theme.GameSurface
import com.example.ui.theme.MintGreen
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.StarGold
import com.example.ui.theme.TokenBlue
import com.example.ui.theme.TokenGreen
import com.example.ui.theme.TokenRed
import com.example.ui.theme.TokenYellow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    roomId: String?, // null if offline game
    offlineState: OfflineGameState?,
    currentUserId: String,
    currentUserName: String,
    gameRepository: GameRepository?,
    userRepository: UserRepository?,
    onExitGame: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var showChatSheet by remember { mutableStateOf(false) }
    var soundEnabled by remember { mutableStateOf(true) }

    // State holders for Online vs Offline
    val isOnline = (roomId != null && gameRepository != null)

    var offlineGameEngine by remember(offlineState) {
        mutableStateOf(
            if (!isOnline && offlineState != null) {
                OfflineGameEngine(offlineState) { /* triggers recomposition through state changes below */ }
            } else null
        )
    }

    var localOfflineState by remember(offlineState) {
        mutableStateOf(offlineState ?: OfflineGameState())
    }

    LaunchedEffect(offlineGameEngine) {
        offlineGameEngine?.let { engine ->
            engine.rollDice() // start game flow if AI first
        }
    }

    val onlineRoomState = if (isOnline) {
        gameRepository?.observeRoom(roomId!!)?.collectAsState(initial = null)?.value
    } else null

    val onlineMessages = if (isOnline) {
        gameRepository?.observeMessages(roomId!!)?.collectAsState(initial = emptyList())?.value ?: emptyList()
    } else emptyList()

    // Normalized Game Values
    val status = if (isOnline) onlineRoomState?.status ?: "waiting" else (if (localOfflineState.isGameOver) "finished" else "playing")
    val playerTokens = if (isOnline) onlineRoomState?.playerTokens ?: emptyList() else localOfflineState.playerTokens
    val playerColors = if (isOnline) onlineRoomState?.playerColors ?: listOf("RED", "BLUE") else localOfflineState.playerColors
    val playerNames = if (isOnline) onlineRoomState?.playerNames ?: listOf("Player 1", "Player 2") else localOfflineState.playerNames
    val currentTurnIndex = if (isOnline) onlineRoomState?.turnIndex?.toInt() ?: 0 else localOfflineState.currentTurnIndex
    val diceValue = if (isOnline) onlineRoomState?.diceValue?.toInt() ?: 1 else localOfflineState.diceValue
    val diceRolled = if (isOnline) onlineRoomState?.diceRolled ?: false else localOfflineState.diceRolled
    val lastAction = if (isOnline) onlineRoomState?.lastAction ?: "Waiting for match..." else localOfflineState.lastAction
    val winnerId = if (isOnline) onlineRoomState?.winnerId else (if (localOfflineState.isGameOver) localOfflineState.winnerName else null)

    val isMyTurn = if (isOnline) {
        onlineRoomState?.turnUid == currentUserId
    } else {
        !localOfflineState.isAiPlayer.getOrElse(currentTurnIndex) { false }
    }

    // Compute Movable Tokens for current turn
    val movableTokenIndices = remember(playerTokens, currentTurnIndex, diceValue, diceRolled, isMyTurn) {
        if (!diceRolled || !isMyTurn) emptyList()
        else {
            val startTokenIdx = currentTurnIndex * BoardConstants.TOKENS_PER_PLAYER
            val list = mutableListOf<Int>()
            for (relIdx in 0 until BoardConstants.TOKENS_PER_PLAYER) {
                val pos = playerTokens.getOrElse(startTokenIdx + relIdx) { 0L }
                if (pos == 0L && diceValue == 6) list.add(relIdx)
                else if (pos > 0L && pos + diceValue <= 100L) list.add(relIdx)
            }
            list
        }
    }

    // Record stats on match completion
    var hasRecordedResult by remember { mutableStateOf(false) }
    LaunchedEffect(status, winnerId) {
        if (status == "finished" && !hasRecordedResult && userRepository != null) {
            hasRecordedResult = true
            val won = if (isOnline) winnerId == currentUserId else winnerId == localOfflineState.playerNames.firstOrNull()
            coroutineScope.launch {
                try {
                    userRepository.recordMatchResult(won)
                } catch (_: Exception) {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isOnline) "Match Room: ${onlineRoomState?.roomId ?: roomId}" else "Offline Match",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isOnline && status == "waiting") "Waiting for opponent to join..." else "Turn: ${playerNames.getOrElse(currentTurnIndex) { "Player" }}",
                            fontSize = 12.sp,
                            color = StarGold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onExitGame, modifier = Modifier.testTag("exit_game_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { soundEnabled = !soundEnabled }) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Sound Toggle",
                            tint = if (soundEnabled) StarGold else Color(0xFF9E99C2)
                        )
                    }
                    if (isOnline) {
                        IconButton(onClick = { showChatSheet = true }, modifier = Modifier.testTag("open_chat_button")) {
                            Icon(Icons.Default.Chat, contentDescription = "Chat", tint = NeonPurple)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GameSurface)
            )
        },
        containerColor = GameBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Action & Events Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = GameCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, GameCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isMyTurn) MintGreen else StarGold)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = lastAction,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            // Player HUD Cards Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (pIdx in playerNames.indices) {
                    val isCurrent = (pIdx == currentTurnIndex)
                    val pColor = resolveColor(playerColors.getOrElse(pIdx) { "RED" })
                    val pName = playerNames[pIdx]
                    val startToken = pIdx * BoardConstants.TOKENS_PER_PLAYER
                    val t1Pos = playerTokens.getOrElse(startToken) { 0L }
                    val t2Pos = playerTokens.getOrElse(startToken + 1) { 0L }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = if (isCurrent) 2.dp else 1.dp,
                                color = if (isCurrent) pColor else GameCardBorder,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = GameCard)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(pColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = pName.take(8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrent) Color.White else Color(0xFFD4D0E8)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "T1: ${if (t1Pos == 0L) "Home" else "$t1Pos"} | T2: ${if (t2Pos == 0L) "Home" else "$t2Pos"}",
                                fontSize = 10.5.sp,
                                color = StarGold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Waiting for Opponent Overlay (if online waiting)
            if (isOnline && status == "waiting") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GameCard)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = NeonPurple, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Waiting for an opponent to join...",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Room Code: $roomId",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = StarGold
                        )
                    }
                }
            }

            // Interactive 10x10 Game Board
            BoardView(
                playerTokens = playerTokens,
                playerColors = playerColors,
                currentTurnIndex = currentTurnIndex,
                movableTokenIndices = movableTokenIndices,
                onTokenClicked = { relIdx ->
                    if (isMyTurn && diceRolled) {
                        if (isOnline) {
                            coroutineScope.launch {
                                gameRepository?.moveToken(roomId!!, relIdx)
                            }
                        } else {
                            offlineGameEngine?.moveToken(relIdx)
                            localOfflineState = offlineGameEngine?.state ?: localOfflineState
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Dice & Move Controls Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Token Selection Quick Buttons (especially helpful if token is at base tile 0)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Move Token:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFD4D0E8)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (relIdx in 0 until BoardConstants.TOKENS_PER_PLAYER) {
                            val isMovable = movableTokenIndices.contains(relIdx)
                            Button(
                                onClick = {
                                    if (isOnline) {
                                        coroutineScope.launch {
                                            gameRepository?.moveToken(roomId!!, relIdx)
                                        }
                                    } else {
                                        offlineGameEngine?.moveToken(relIdx)
                                        localOfflineState = offlineGameEngine?.state ?: localOfflineState
                                    }
                                },
                                enabled = isMovable,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isMovable) StarGold else GameCard,
                                    disabledContainerColor = GameCard
                                ),
                                modifier = Modifier
                                    .size(width = 54.dp, height = 44.dp)
                                    .testTag("select_token_${relIdx + 1}")
                            ) {
                                Text(
                                    text = "T${relIdx + 1}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isMovable) Color.Black else Color(0xFF756F97)
                                )
                            }
                        }
                    }
                }

                // 3D Animated Dice
                DiceView(
                    diceValue = diceValue,
                    isMyTurn = isMyTurn,
                    diceRolled = diceRolled,
                    onRollClicked = {
                        if (isMyTurn && !diceRolled) {
                            if (isOnline) {
                                coroutineScope.launch {
                                    gameRepository?.rollDice(roomId!!)
                                }
                            } else {
                                offlineGameEngine?.rollDice()
                                localOfflineState = offlineGameEngine?.state ?: localOfflineState
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Live Chat Bottom Sheet
        if (showChatSheet) {
            ChatBottomSheet(
                messages = onlineMessages,
                currentUserId = currentUserId,
                onSendMessage = { text ->
                    if (isOnline) {
                        coroutineScope.launch {
                            gameRepository?.sendMessage(roomId!!, text, currentUserName)
                        }
                    }
                },
                onDismiss = { showChatSheet = false }
            )
        }

        // Victory Dialog
        if (status == "finished") {
            val won = if (isOnline) winnerId == currentUserId else winnerId == playerNames.firstOrNull()
            AlertDialog(
                onDismissRequest = onExitGame,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = StarGold,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (won) "VICTORY!" else "GAME OVER",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = if (won) StarGold else NeonCoral
                        )
                    }
                },
                text = {
                    Column {
                        Text(
                            text = if (won) "Congratulations! You reached tile 100 and claimed the crown!"
                            else "$winnerId has reached tile 100 and won the match!",
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (won) "🏆 +25 Trophies awarded!" else "💔 -10 Trophies",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (won) MintGreen else NeonCoral
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = onExitGame,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        modifier = Modifier.testTag("victory_dialog_exit_button")
                    ) {
                        Text("Return to Lobby")
                    }
                },
                containerColor = GameSurface
            )
        }
    }
}

private fun resolveColor(name: String): Color {
    return when (name.uppercase()) {
        "RED" -> TokenRed
        "BLUE" -> TokenBlue
        "GREEN" -> TokenGreen
        "YELLOW" -> TokenYellow
        else -> NeonCoral
    }
}
