package com.triviamaps.app.ui.screens.map

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.triviamaps.app.R

@Composable
fun WelcomeDialog(
    isNewUser: Boolean,
    onDismiss: () -> Unit
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = screenHeight * 0.82f)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.triviamaps_logo),
                        contentDescription = "TriviaMaps Logo",
                        modifier = Modifier.size(90.dp),
                        contentScale = ContentScale.Fit
                    )

                    Text(
                        text = if (isNewUser) "Welcome to TriviaMaps!" else "Welcome back!",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Explore the world, learn about amazing locations, and compete with players globally through location-based trivia!",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    HorizontalDivider()

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "How it works",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        WelcomeFeatureRow(
                            emoji = "📍",
                            title = "Place Markers",
                            description = "Visit interesting locations and create trivia questions for others to discover."
                        )
                        WelcomeFeatureRow(
                            emoji = "❓",
                            title = "Answer Questions",
                            description = "Walk near a marker and answer the trivia question left by another player."
                        )
                        WelcomeFeatureRow(
                            emoji = "⭐",
                            title = "Earn Points",
                            description = "Get points for creating questions, answering them, and correct answers."
                        )
                        WelcomeFeatureRow(
                            emoji = "🏆",
                            title = "Compete Globally",
                            description = "Rank against players worldwide or just within your own country."
                        )
                        WelcomeFeatureRow(
                            emoji = "🔍",
                            title = "Explore & Filter",
                            description = "Browse markers by category, search by name or author, sort by distance and more."
                        )
                    }

                    HorizontalDivider()

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Coming Soon",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    "🚀 In development",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(
                                        horizontal = 8.dp, vertical = 3.dp
                                    )
                                )
                            }
                        }
                        ComingSoonRow(
                            emoji = "🎖️",
                            text = "Cosmetic rewards and badges for top-ranked players"
                        )
                        ComingSoonRow(
                            emoji = "🗺️",
                            text = "Themed map styles and custom marker icons"
                        )
                        ComingSoonRow(
                            emoji = "👥",
                            text = "Friends system and private challenges"
                        )
                        ComingSoonRow(
                            emoji = "📸",
                            text = "Photo verification for marker locations"
                        )
                    }
                }

                // Button always visible at bottom — not scrollable
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            if (isNewUser) "Let's Explore! 🗺️" else "Continue Exploring! 🗺️",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeFeatureRow(
    emoji: String,
    title: String,
    description: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(emoji, fontSize = 18.sp)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = Color.Gray,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun ComingSoonRow(emoji: String, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 16.sp)
        Text(
            text = text,
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.weight(1f)
        )
    }
}