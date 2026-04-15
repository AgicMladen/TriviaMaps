package com.triviamaps.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.triviamaps.app.data.Constants
import com.triviamaps.app.data.model.TriviaMarker
import com.triviamaps.app.data.model.User
import com.triviamaps.app.data.repository.AuthRepository
import com.triviamaps.app.data.repository.MarkerRepository
import androidx.compose.material.icons.filled.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onViewOnMap: (LatLng) -> Unit,
    onNavigateToSettings: () -> Unit = {}
) {
    val authRepository = remember { AuthRepository() }
    val markerRepository = remember { MarkerRepository() }

    var user by remember { mutableStateOf<User?>(null) }
    var userMarkers by remember { mutableStateOf<List<TriviaMarker>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedMarker by remember { mutableStateOf<TriviaMarker?>(null) }

    LaunchedEffect(Unit) {
        authRepository.getCurrentUserData().onSuccess {
            user = it
            markerRepository.getUserMarkers(it.uid).onSuccess { markers ->
                userMarkers = markers
            }
        }
        isLoading = false
    }

    val categories = listOf("All") + Constants.CATEGORIES

    val filteredMarkers = if (selectedCategory == "All")
        userMarkers
    else
        userMarkers.filter { it.category == selectedCategory }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("👤 Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile header card
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AvatarImage(
                                imageUrl = user?.profileImageUrl ?: "",
                                username = user?.username ?: "",
                                size = 90
                            )
                            Text(
                                text = user?.fullName ?: "",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Text(
                                text = "@${user?.username ?: ""}",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp
                            )
                            if (user?.country?.isNotEmpty() == true) {
                                Text(
                                    text = "🌍 ${user?.country}",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }

                // Points breakdown card
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Points Breakdown",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            StatRow("⭐ Total Points", "${user?.totalPoints ?: 0}")
                            HorizontalDivider()
                            StatRow("❓ Question Points", "${user?.questionPoints ?: 0}")
                            StatRow("✅ Answer Points", "${user?.answerPoints ?: 0}")
                            HorizontalDivider()
                            StatRow("📍 Questions Created", "${user?.questionsCreated ?: 0}")
                            StatRow("🎯 Questions Answered", "${user?.questionsAnswered ?: 0}")
                        }
                    }
                }

                // Markers header + count
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "My Markers",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            "${filteredMarkers.size} / ${userMarkers.size}",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Category filter chips
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(categories) { category ->
                            val isSelected = selectedCategory == category
                            val categoryColor = if (category == "All")
                                MaterialTheme.colorScheme.primary
                            else
                                Color(Constants.CATEGORY_COLORS[category] ?: 0xFF546E7A)
                            val emoji = if (category == "All") "🗺️"
                            else Constants.CATEGORY_EMOJIS[category] ?: "📌"

                            Surface(
                                modifier = Modifier.clickable { selectedCategory = category },
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected)
                                    categoryColor.copy(alpha = 0.15f)
                                else
                                    MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) categoryColor
                                    else Color.Gray.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 8.dp
                                    ),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(emoji, fontSize = 14.sp)
                                    Text(
                                        text = category,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold
                                        else FontWeight.Normal,
                                        color = if (isSelected) categoryColor
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Empty state
                if (filteredMarkers.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("🗺️", fontSize = 40.sp)
                                Text(
                                    if (selectedCategory == "All")
                                        "No markers yet!\nGo explore and add some."
                                    else
                                        "No markers in this category.",
                                    color = Color.Gray,
                                    fontSize = 14.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Marker cards
                items(filteredMarkers) { marker ->
                    MarkerCard(
                        marker = marker,
                        onClick = { selectedMarker = marker }
                    )
                }
            }
        }
    }

    // Logout dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(onClick = {
                    FirebaseAuth.getInstance().signOut()
                    onLogout()
                }) { Text("Logout", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Marker detail dialog
    selectedMarker?.let { marker ->
        MarkerDetailDialog(
            marker = marker,
            onDismiss = { selectedMarker = null },
            onViewOnMap = {
                selectedMarker = null
                onViewOnMap(LatLng(marker.latitude, marker.longitude))
            }
        )
    }
}

@Composable
fun MarkerDetailDialog(
    marker: TriviaMarker,
    onDismiss: () -> Unit,
    onViewOnMap: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Location image
                if (marker.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = marker.imageUrl,
                        contentDescription = "Location image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                // Category badge
                val categoryColor = Color(
                    Constants.CATEGORY_COLORS[marker.category] ?: 0xFF546E7A
                )
                val emoji = Constants.CATEGORY_EMOJIS[marker.category] ?: "📌"
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = categoryColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "$emoji ${marker.category}",
                        fontSize = 12.sp,
                        color = categoryColor,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = marker.locationName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Text(
                    text = marker.question,
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                HorizontalDivider()

                // Answer options
                marker.options.forEachIndexed { index, option ->
                    val isCorrect = index == marker.correctAnswerIndex
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isCorrect) "✅" else "○",
                            fontSize = 14.sp
                        )
                        Text(
                            text = option,
                            fontSize = 14.sp,
                            fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCorrect) Color(0xFF4CAF50)
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                HorizontalDivider()

                // Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${marker.timesAnswered}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("Answered", fontSize = 11.sp, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${marker.timesAnsweredCorrectly}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color(0xFF4CAF50)
                        )
                        Text("Correct", fontSize = 11.sp, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val rate = if (marker.timesAnswered > 0)
                            (marker.timesAnsweredCorrectly * 100 / marker.timesAnswered)
                        else 0
                        Text(
                            "$rate%",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text("Success rate", fontSize = 11.sp, color = Color.Gray)
                    }
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close")
                    }
                    Button(
                        onClick = onViewOnMap,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("🗺️ View on Map")
                    }
                }
            }
        }
    }
}

@Composable
fun AvatarImage(imageUrl: String, username: String, size: Int) {
    val isEmoji = imageUrl.length <= 2 && imageUrl.isNotEmpty()
    when {
        isEmoji -> {
            Box(
                modifier = Modifier
                    .size(size.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(imageUrl, fontSize = (size * 0.5f).sp)
            }
        }
        imageUrl.isNotEmpty() -> {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(size.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
        else -> {
            Box(
                modifier = Modifier
                    .size(size.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = username.firstOrNull()?.uppercase() ?: "?",
                    color = Color.White,
                    fontSize = (size * 0.4f).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = Color.Gray)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun MarkerCard(marker: TriviaMarker, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (marker.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = marker.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                // Category badge
                val categoryColor = Color(
                    Constants.CATEGORY_COLORS[marker.category] ?: 0xFF546E7A
                )
                val emoji = Constants.CATEGORY_EMOJIS[marker.category] ?: "📌"
                Text(
                    text = "$emoji ${marker.category}",
                    fontSize = 10.sp,
                    color = categoryColor,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = marker.locationName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = marker.question,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "👁️ ${marker.timesAnswered} answered",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "✅ ${marker.timesAnsweredCorrectly} correct",
                        fontSize = 11.sp,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}