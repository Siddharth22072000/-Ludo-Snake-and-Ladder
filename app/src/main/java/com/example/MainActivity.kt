package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.credentials.CredentialManager
import com.example.data.repository.GameRepository
import com.example.data.repository.UserRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.game.GameScreen
import com.example.ui.game.OfflineGameState
import com.example.ui.leaderboard.LeaderboardScreen
import com.example.ui.lobby.LobbyScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.theme.GameBackground
import com.example.ui.theme.LudoLaddersTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

enum class AppDestination {
    LOBBY,
    GAME,
    LEADERBOARD,
    PROFILE
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val databaseId = getString(R.string.firestore_database_id)
        val firestore = FirebaseFirestore.getInstance(databaseId)
        val gameRepository = GameRepository(firestore)
        val userRepository = UserRepository(firestore)

        setContent {
            LudoLaddersTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = GameBackground
                ) {
                    AppRoot(
                        gameRepository = gameRepository,
                        userRepository = userRepository
                    )
                }
            }
        }
    }
}

@Composable
fun AppRoot(
    gameRepository: GameRepository,
    userRepository: UserRepository
) {
    var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }
    var isGuestMode by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
            if (auth.currentUser != null) {
                isGuestMode = false
            }
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    // Auto-create/sync user profile on authenticated login
    LaunchedEffect(currentUser?.uid) {
        currentUser?.let { user ->
            try {
                userRepository.getOrCreateUserProfile(
                    userId = user.uid,
                    displayName = user.displayName ?: "Player_${user.uid.take(4)}",
                    photoUrl = user.photoUrl?.toString()
                )
            } catch (_: Exception) {}
        }
    }

    if (currentUser == null && !isGuestMode) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
                isGuestMode = false
            },
            onGuestMode = {
                isGuestMode = true
            }
        )
    } else {
        val uid = currentUser?.uid ?: "guest_${remember { java.util.UUID.randomUUID().toString().take(6) }}"
        val name = currentUser?.displayName ?: "Guest Roller"

        MainNavigation(
            currentUserId = uid,
            currentUserName = name,
            isGuest = (currentUser == null),
            gameRepository = if (currentUser != null) gameRepository else null,
            userRepository = if (currentUser != null) userRepository else null,
            onSignOut = {
                currentUser = null
                isGuestMode = false
            }
        )
    }
}

@Composable
fun MainNavigation(
    currentUserId: String,
    currentUserName: String,
    isGuest: Boolean,
    gameRepository: GameRepository?,
    userRepository: UserRepository?,
    onSignOut: () -> Unit
) {
    var currentDestination by remember { mutableStateOf(AppDestination.LOBBY) }
    var activeRoomId by remember { mutableStateOf<String?>(null) }
    var activeOfflineState by remember { mutableStateOf<OfflineGameState?>(null) }

    when (currentDestination) {
        AppDestination.LOBBY -> {
            LobbyScreen(
                currentUserId = currentUserId,
                currentUserName = currentUserName,
                gameRepository = gameRepository,
                userRepository = userRepository,
                onStartOnlineGame = { roomId ->
                    activeRoomId = roomId
                    activeOfflineState = null
                    currentDestination = AppDestination.GAME
                },
                onStartOfflineGame = { offlineState ->
                    activeRoomId = null
                    activeOfflineState = offlineState
                    currentDestination = AppDestination.GAME
                },
                onOpenLeaderboard = {
                    currentDestination = AppDestination.LEADERBOARD
                },
                onOpenProfile = {
                    currentDestination = AppDestination.PROFILE
                }
            )
        }

        AppDestination.GAME -> {
            BackHandler {
                currentDestination = AppDestination.LOBBY
            }
            GameScreen(
                roomId = activeRoomId,
                offlineState = activeOfflineState,
                currentUserId = currentUserId,
                currentUserName = currentUserName,
                gameRepository = gameRepository,
                userRepository = userRepository,
                onExitGame = {
                    activeRoomId = null
                    activeOfflineState = null
                    currentDestination = AppDestination.LOBBY
                }
            )
        }

        AppDestination.LEADERBOARD -> {
            BackHandler {
                currentDestination = AppDestination.LOBBY
            }
            LeaderboardScreen(
                currentUserId = currentUserId,
                gameRepository = gameRepository,
                onBack = { currentDestination = AppDestination.LOBBY }
            )
        }

        AppDestination.PROFILE -> {
            BackHandler {
                currentDestination = AppDestination.LOBBY
            }
            ProfileScreen(
                currentUserId = currentUserId,
                userRepository = userRepository,
                onSignOut = onSignOut,
                onBack = { currentDestination = AppDestination.LOBBY }
            )
        }
    }
}
