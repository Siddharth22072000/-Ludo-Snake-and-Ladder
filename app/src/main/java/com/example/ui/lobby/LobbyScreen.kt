package com.example.ui.lobby

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BoardConstants
import com.example.data.repository.GameRepository
import com.example.data.repository.UserRepository
import com.example.ui.game.OfflineGameState
import com.example.ui.theme.GameBackground
import com.example.ui.theme.GameCard
import com.example.ui.theme.GameCardBorder
import com.example.ui.theme.GameSurface
import com.example.ui.theme.MintGreen
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.StarGold
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(
    currentUserId: String,
    currentUserName: String,
    gameRepository: GameRepository?,
    userRepository: UserRepository?,
    onStartOnlineGame: (roomId: String) -> Unit,
    onStartOfflineGame: (state: OfflineGameState) -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isMatchmaking by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showOfflineDialog by remember { mutableStateOf(false) }
    var showRulesDialog by remember { mutableStateOf(false) }
    var joinRoomCode by remember { mutableStateOf("") }
    var createPlayerCount by remember { mutableIntStateOf(2) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val userProfile by (userRepository?.observeUserProfile(currentUserId)?.collectAsState(initial = null)
        ?: remember { mutableStateOf(null) })

    val username = userProfile?.username ?: currentUserName
    val trophies = userProfile?.trophies ?: 100L

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NeonPurple),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Casino, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Ludo Ladders", fontSize = 17.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Text("Roll, Climb & Conquer", fontSize = 11.5.sp, color = StarGold)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showRulesDialog = true }, modifier = Modifier.testTag("rules_button")) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "Rules", tint = Color.White)
                    }
                    IconButton(onClick = onOpenProfile, modifier = Modifier.testTag("open_profile_button")) {
                        Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GameSurface)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = GameSurface) {
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.Default.Casino, contentDescription = "Play") },
                    label = { Text("Play") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        indicatorColor = NeonPurple
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onOpenLeaderboard,
                    icon = { Icon(Icons.Default.EmojiEvents, contentDescription = "Ranks") },
                    label = { Text("Leaderboard") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = StarGold,
                        indicatorColor = NeonPurple
                    ),
                    modifier = Modifier.testTag("nav_leaderboard_button")
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onOpenProfile,
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonPurple,
                        indicatorColor = NeonPurple
                    ),
                    modifier = Modifier.testTag("nav_profile_button")
                )
            }
        },
        containerColor = GameBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Player Summary Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenProfile() },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = GameCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, GameCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(NeonPurple, NeonCoral))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = username.take(1).uppercase(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = username,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = StarGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$trophies Trophies",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = StarGold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF352B5C))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Stats", color = NeonPurple, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            if (statusMessage != null) {
                Text(
                    text = statusMessage!!,
                    color = NeonCoral,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text(
                text = "ONLINE MULTIPLAYER",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                color = Color(0xFF9E99C2)
            )

            // Quick Online Matchmaking
            GameModeCard(
                title = "Quick Match (1v1)",
                subtitle = "Automatch with players around the world in real-time",
                icon = Icons.Default.Casino,
                accentColor = NeonPurple,
                isLoading = isMatchmaking,
                onClick = {
                    if (gameRepository == null) {
                        statusMessage = "Please sign in with Google to play online multiplayer"
                        return@GameModeCard
                    }
                    isMatchmaking = true
                    statusMessage = null
                    coroutineScope.launch {
                        try {
                            val roomId = gameRepository.findOrCreateQuickMatch(username)
                            isMatchmaking = false
                            onStartOnlineGame(roomId)
                        } catch (e: Exception) {
                            isMatchmaking = false
                            statusMessage = "Matchmaking failed: ${e.localizedMessage}"
                        }
                    }
                },
                testTag = "quick_match_card"
            )

            // Custom Room (Create or Join)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (gameRepository == null) {
                                statusMessage = "Please sign in with Google to create rooms"
                            } else {
                                showCreateDialog = true
                            }
                        }
                        .testTag("create_room_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GameCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GameCardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Groups, contentDescription = null, tint = StarGold, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Create Room", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        Text("Host with Code", fontSize = 11.sp, color = Color(0xFF9E99C2))
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (gameRepository == null) {
                                statusMessage = "Please sign in with Google to join rooms"
                            } else {
                                showJoinDialog = true
                            }
                        }
                        .testTag("join_room_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GameCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GameCardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Casino, contentDescription = null, tint = MintGreen, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Join Room", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        Text("Enter Code", fontSize = 11.sp, color = Color(0xFF9E99C2))
                    }
                }
            }

            Text(
                text = "OFFLINE MODES",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                color = Color(0xFF9E99C2)
            )

            // Play vs AI
            GameModeCard(
                title = "Play vs AI Bots",
                subtitle = "Practice against smart AI opponents offline anytime",
                icon = Icons.Default.SmartToy,
                accentColor = MintGreen,
                isLoading = false,
                onClick = {
                    showOfflineDialog = true
                },
                testTag = "play_vs_ai_card"
            )

            // Pass & Play (Local Multiplayer)
            GameModeCard(
                title = "Pass & Play (Same Device)",
                subtitle = "Play with friends and family taking turns on one phone",
                icon = Icons.Default.Groups,
                accentColor = NeonCoral,
                isLoading = false,
                onClick = {
                    val passPlayState = OfflineGameState(
                        playerCount = 2,
                        playerNames = listOf("Player 1", "Player 2"),
                        playerColors = listOf("RED", "BLUE"),
                        isAiPlayer = listOf(false, false),
                        playerTokens = List(4) { 0L }
                    )
                    onStartOfflineGame(passPlayState)
                },
                testTag = "pass_and_play_card"
            )
        }
    }

    // Create Room Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Multiplayer Room", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column {
                    Text("Select number of players:", color = Color(0xFFD4D0E8), fontSize = 13.5.sp)
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (count in 2..4) {
                            val isSelected = (createPlayerCount == count)
                            Button(
                                onClick = { createPlayerCount = count },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) NeonPurple else GameCard
                                )
                            ) {
                                Text("$count Players", color = if (isSelected) Color.White else Color(0xFF9E99C2))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCreateDialog = false
                        coroutineScope.launch {
                            try {
                                val roomId = gameRepository?.createRoom(username, createPlayerCount)
                                if (roomId != null) onStartOnlineGame(roomId)
                            } catch (e: Exception) {
                                statusMessage = "Failed to create room: ${e.localizedMessage}"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    modifier = Modifier.testTag("confirm_create_room_button")
                ) {
                    Text("Create Room")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = Color(0xFFD4D0E8))
                }
            },
            containerColor = GameSurface
        )
    }

    // Join Room Dialog
    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = { Text("Join Room", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column {
                    Text("Enter room code:", color = Color(0xFFD4D0E8), fontSize = 13.5.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = joinRoomCode,
                        onValueChange = { joinRoomCode = it.trim() },
                        placeholder = { Text("e.g. room_a1b2c3", color = Color(0xFF9E99C2)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = GameBackground,
                            unfocusedContainerColor = GameBackground,
                            focusedBorderColor = NeonPurple,
                            unfocusedBorderColor = GameCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("room_code_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (joinRoomCode.isNotBlank()) {
                            showJoinDialog = false
                            coroutineScope.launch {
                                try {
                                    val joined = gameRepository?.joinRoom(joinRoomCode, username) ?: false
                                    if (joined) {
                                        onStartOnlineGame(joinRoomCode)
                                    } else {
                                        statusMessage = "Room not found or already full"
                                    }
                                } catch (e: Exception) {
                                    statusMessage = "Failed to join: ${e.localizedMessage}"
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    modifier = Modifier.testTag("confirm_join_room_button")
                ) {
                    Text("Join")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showJoinDialog = false }) {
                    Text("Cancel", color = Color(0xFFD4D0E8))
                }
            },
            containerColor = GameSurface
        )
    }

    // Offline AI Setup Dialog
    if (showOfflineDialog) {
        var botCount by remember { mutableIntStateOf(1) }
        AlertDialog(
            onDismissRequest = { showOfflineDialog = false },
            title = { Text("Play vs AI", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column {
                    Text("Choose number of AI opponents:", color = Color(0xFFD4D0E8), fontSize = 13.5.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (count in 1..3) {
                            val isSelected = (botCount == count)
                            Button(
                                onClick = { botCount = count },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MintGreen else GameCard
                                )
                            ) {
                                Text("$count ${if (count == 1) "Bot" else "Bots"}", color = if (isSelected) Color.Black else Color(0xFF9E99C2))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showOfflineDialog = false
                        val names = mutableListOf(username)
                        val colors = mutableListOf("RED")
                        val isAi = mutableListOf(false)
                        val botNames = listOf("Bot Alex", "Bot Maya", "Bot Leo")

                        for (i in 0 until botCount) {
                            names.add(botNames[i])
                            colors.add(BoardConstants.PLAYER_COLORS[(i + 1) % BoardConstants.PLAYER_COLORS.size])
                            isAi.add(true)
                        }

                        val offlineState = OfflineGameState(
                            playerCount = botCount + 1,
                            playerNames = names,
                            playerColors = colors,
                            isAiPlayer = isAi,
                            playerTokens = List((botCount + 1) * BoardConstants.TOKENS_PER_PLAYER) { 0L }
                        )
                        onStartOfflineGame(offlineState)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MintGreen),
                    modifier = Modifier.testTag("start_ai_game_button")
                ) {
                    Text("Start Match", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showOfflineDialog = false }) {
                    Text("Cancel", color = Color(0xFFD4D0E8))
                }
            },
            containerColor = GameSurface
        )
    }

    // Rules Dialog
    if (showRulesDialog) {
        AlertDialog(
            onDismissRequest = { showRulesDialog = false },
            title = { Text("Game Rules & Mechanics", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RulePoint(emoji = "🎲", title = "Spawning & Rolling 6", desc = "Tokens start in Base (Home). Roll a 6 to bring a token to tile 1, and get an extra roll!")
                    RulePoint(emoji = "🪜", title = "Ladders Climb", desc = "Landing on the foot of a ladder vaults your token up to the top!")
                    RulePoint(emoji = "🐍", title = "Snakes Slide", desc = "Landing on a venomous snake's head slides your token down to the tail.")
                    RulePoint(emoji = "💥", title = "Ludo Knockouts", desc = "Landing on an opponent's token knocks it back to their Base! Plus you earn an extra roll!")
                    RulePoint(emoji = "⭐", title = "Safe Star Tiles", desc = "Tiles with stars (1, 9, 14, 21, 28, 38, etc.) are safe zones immune to knockouts.")
                    RulePoint(emoji = "🏆", title = "Winning the Game", desc = "The first player to guide all their tokens to Tile 100 wins the match and claims trophies!")
                }
            },
            confirmButton = {
                Button(
                    onClick = { showRulesDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                ) {
                    Text("Got It!")
                }
            },
            containerColor = GameSurface
        )
    }
}

@Composable
private fun RulePoint(emoji: String, title: String, desc: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(emoji, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = StarGold)
            Text(desc, fontSize = 12.sp, color = Color(0xFFD4D0E8))
        }
    }
}

@Composable
private fun GameModeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    isLoading: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading) { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GameCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, GameCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = accentColor, modifier = Modifier.size(24.dp))
                } else {
                    Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(26.dp))
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF9E99C2)
                )
            }
        }
    }
}
