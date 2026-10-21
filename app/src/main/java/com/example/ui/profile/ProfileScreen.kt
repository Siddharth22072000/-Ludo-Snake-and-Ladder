package com.example.ui.profile

import android.content.Context
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.data.model.UserProfile
import com.example.data.repository.UserRepository
import com.example.ui.auth.signOutUser
import com.example.ui.theme.GameBackground
import com.example.ui.theme.GameCard
import com.example.ui.theme.GameCardBorder
import com.example.ui.theme.GameSurface
import com.example.ui.theme.MintGreen
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.StarGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    currentUserId: String,
    userRepository: UserRepository?,
    onSignOut: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val userProfile by (userRepository?.observeUserProfile(currentUserId)?.collectAsState(initial = null)
        ?: remember { androidx.compose.runtime.mutableStateOf(null) })

    val username = userProfile?.username ?: "Guest Player"
    val trophies = userProfile?.trophies ?: 100L
    val wins = userProfile?.wins ?: 0L
    val played = userProfile?.gamesPlayed ?: 0L
    val winRate = if (played > 0) ((wins.toDouble() / played) * 100).toInt() else 0

    val tierTitle = when {
        trophies >= 1000 -> "👑 Grandmaster"
        trophies >= 500 -> "💎 Diamond Champion"
        trophies >= 250 -> "🥇 Gold Climber"
        trophies >= 100 -> "🥈 Silver Runner"
        else -> "🥉 Bronze Roller"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Player Profile", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("profile_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GameSurface)
            )
        },
        containerColor = GameBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Avatar Ring
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(NeonPurple, NeonCoral)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = username,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Text(
                text = tierTitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = StarGold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Stats Cards Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Trophies",
                    value = "$trophies",
                    icon = Icons.Default.Star,
                    color = StarGold
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Wins",
                    value = "$wins",
                    icon = Icons.Default.EmojiEvents,
                    color = MintGreen
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Matches Played",
                    value = "$played",
                    icon = Icons.Default.Person,
                    color = NeonPurple
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Win Rate",
                    value = "$winRate%",
                    icon = Icons.Default.EmojiEvents,
                    color = NeonCoral
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Sign Out Button
            Button(
                onClick = {
                    signOutUser(
                        context = context,
                        credentialManager = credentialManager,
                        onSignOutComplete = onSignOut,
                        scope = scope
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("sign_out_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381A2A))
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = NeonCoral)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out", fontWeight = FontWeight.Bold, color = NeonCoral)
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = modifier.border(1.dp, GameCardBorder, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = GameCard)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text(text = title, fontSize = 11.5.sp, color = Color(0xFF9E99C2))
        }
    }
}
