package com.triviamaps.app.ui.screens.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.triviamaps.app.data.model.User
import com.triviamaps.app.data.repository.AuthRepository
import com.triviamaps.app.data.repository.MarkerRepository
import com.triviamaps.app.ui.screens.profile.AvatarImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(onBack: () -> Unit) {
    val markerRepository = remember { MarkerRepository() }
    val authRepository = remember { AuthRepository() }
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var currentUser by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // 0 = Combined world, 1 = Questions world, 2 = Answers world, 3 = Country
    val tabs = listOf("🌍\nWorld", "❓\nQuestions", "✅\nAnswers", "🏠\nCountry")

    LaunchedEffect(Unit) {
        authRepository.getCurrentUserData().onSuccess { currentUser = it }
        markerRepository.getLeaderboard().onSuccess { users = it }
        isLoading = false
    }

    val sortedUsers = when (selectedTab) {
        0 -> users.sortedByDescending { it.totalPoints }
        1 -> users.sortedByDescending { it.questionPoints }
        2 -> users.sortedByDescending { it.answerPoints }
        3 -> users
            .filter { it.country == currentUser?.country && it.country.isNotEmpty() }
            .sortedByDescending { it.totalPoints }
        else -> users
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🏆 Leaderboard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index)
                                    FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 14.sp
                            )
                        }
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (selectedTab == 3 && sortedUsers.isEmpty()) {
                // No country data
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🌍", fontSize = 48.sp)
                        Text(
                            if (currentUser?.country.isNullOrEmpty())
                                "You haven't set a country yet.\nUpdate your profile to see country rankings."
                            else
                                "No other players from ${currentUser?.country} yet.\nBe the first!",
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            } else {
                // Country tab header
                if (selectedTab == 3 && currentUser?.country?.isNotEmpty() == true) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "🏠 Rankings for ${currentUser?.country}",
                            modifier = Modifier.padding(
                                horizontal = 16.dp, vertical = 10.dp
                            ),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(sortedUsers) { index, user ->
                        LeaderboardItem(
                            user = user,
                            rank = index + 1,
                            selectedTab = selectedTab,
                            isCurrentUser = user.uid == currentUser?.uid
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardItem(
    user: User,
    rank: Int,
    selectedTab: Int,
    isCurrentUser: Boolean
) {
    val rankColor = when (rank) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }

    val rankEmoji = when (rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> "#$rank"
    }

    val points = when (selectedTab) {
        1 -> user.questionPoints
        2 -> user.answerPoints
        else -> user.totalPoints
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCurrentUser -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                rank <= 3 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCurrentUser) 4.dp else 2.dp
        ),
        border = if (isCurrentUser) androidx.compose.foundation.BorderStroke(
            1.5.dp, MaterialTheme.colorScheme.primary
        ) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = rankEmoji,
                fontSize = if (rank <= 3) 24.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                color = rankColor,
                modifier = Modifier.width(40.dp)
            )

            AvatarImage(
                imageUrl = user.profileImageUrl,
                username = user.username,
                size = 44
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = user.username,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    if (isCurrentUser) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                "you",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(
                                    horizontal = 6.dp, vertical = 2.dp
                                )
                            )
                        }
                    }
                }
                Text(
                    text = if (user.country.isNotEmpty()) user.country else "Unknown",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$points pts",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "❓ ${user.questionsCreated}  ✅ ${user.questionsAnswered}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }
    }
}