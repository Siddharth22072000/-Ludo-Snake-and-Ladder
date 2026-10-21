package com.example.ui.leaderboard

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LeaderboardEntry
import com.example.data.repository.GameRepository
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
fun LeaderboardScreen(
    currentUserId: String,
    gameRepository: GameRepository?,
    onBack: () -> Unit
) {
    val entries by (gameRepository?.observeLeaderboard()?.collectAsState(initial = null)
        ?: remember { androidx.compose.runtime.mutableStateOf(null) })

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = StarGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Global Leaderboard", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("leaderboard_back_button")) {
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
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            if (entries == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonPurple)
                }
            } else if (entries!!.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No players ranked yet. Play an online match to climb the ranks!",
                        fontSize = 14.sp,
                        color = Color(0xFF9E99C2),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                val list = entries!!

                // Top 3 Podium Cards (if at least 3 players exist, or whatever subset)
                if (list.size >= 2) {
                    PodiumSection(list.take(3))
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Full Rankings List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(list) { index, entry ->
                        val isMe = (entry.userId == currentUserId)
                        LeaderboardRow(rank = index + 1, entry = entry, isMe = isMe)
                    }
                }
            }
        }
    }
}

@Composable
private fun PodiumSection(topThree: List<LeaderboardEntry>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        // 2nd Place (Silver)
        if (topThree.size >= 2) {
            PodiumItem(entry = topThree[1], rank = 2, color = Color(0xFFC0C0C0), height = 110.dp)
        }
        // 1st Place (Gold)
        if (topThree.isNotEmpty()) {
            PodiumItem(entry = topThree[0], rank = 1, color = StarGold, height = 135.dp)
        }
        // 3rd Place (Bronze)
        if (topThree.size >= 3) {
            PodiumItem(entry = topThree[2], rank = 3, color = Color(0xFFCD7F32), height = 95.dp)
        }
    }
}

@Composable
private fun PodiumItem(entry: LeaderboardEntry, rank: Int, color: Color, height: androidx.compose.ui.unit.Dp) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Icon(
            imageVector = if (rank == 1) Icons.Default.EmojiEvents else Icons.Default.MilitaryTech,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = entry.username.take(9),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "${entry.trophies} 🏆",
            fontSize = 11.sp,
            color = StarGold,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(84.dp)
                .height(height)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(color.copy(alpha = 0.5f), GameCard)
                    )
                )
                .border(1.dp, color, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "#$rank",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}

@Composable
private fun LeaderboardRow(rank: Int, entry: LeaderboardEntry, isMe: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isMe) 1.5.dp else 1.dp,
                color = if (isMe) NeonPurple else GameCardBorder,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isMe) Color(0xFF28204D) else GameCard
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Number
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        when (rank) {
                            1 -> StarGold
                            2 -> Color(0xFFC0C0C0)
                            3 -> Color(0xFFCD7F32)
                            else -> Color(0xFF332C59)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$rank",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = if (rank <= 3) Color.Black else Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Player Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isMe) "${entry.username} (You)" else entry.username,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isMe) NeonPurple else Color.White
                    )
                }
                Text(
                    text = "${entry.wins} Wins | ${entry.gamesPlayed} Matches",
                    fontSize = 11.5.sp,
                    color = Color(0xFF9E99C2)
                )
            }

            // Trophies Rating
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = StarGold,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${entry.trophies}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = StarGold
                )
            }
        }
    }
}
