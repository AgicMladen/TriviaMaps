package com.triviamaps.app.ui.screens.map

import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.triviamaps.app.data.Constants
import com.triviamaps.app.data.model.TriviaMarker
import com.triviamaps.app.data.model.User
import com.triviamaps.app.data.repository.MarkerRepository
import kotlinx.coroutines.launch

const val ANSWER_PROXIMITY_METERS = 100f

@Composable
fun TriviaDialog(
    marker: TriviaMarker,
    currentUser: User?,
    markerRepository: MarkerRepository,
    userLocation: LatLng?,
    onDismiss: () -> Unit,
    onAnswered: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedOption by remember { mutableStateOf(-1) }
    var hasAnswered by remember { mutableStateOf(false) }
    var isCorrect by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var alreadyAnswered by remember { mutableStateOf(false) }
    var isOwn by remember { mutableStateOf(false) }

    // Check if user is in proximity
    val inProximity = remember(userLocation) {
        if (userLocation == null) false
        else {
            val userAndroid = Location("").apply {
                latitude = userLocation.latitude
                longitude = userLocation.longitude
            }
            val markerAndroid = Location("").apply {
                latitude = marker.latitude
                longitude = marker.longitude
            }
            userAndroid.distanceTo(markerAndroid) <= ANSWER_PROXIMITY_METERS
        }
    }

    val successRate = if (marker.timesAnswered > 0)
        (marker.timesAnsweredCorrectly * 100 / marker.timesAnswered)
    else null

    LaunchedEffect(marker) {
        currentUser?.let { user ->
            isOwn = marker.authorUid == user.uid
            if (!isOwn) {
                alreadyAnswered = markerRepository.hasUserAnsweredMarker(marker.id, user.uid)
            }
        }
    }

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
                            .height(160.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                // Category badge + name
                val categoryColor = Color(
                    Constants.CATEGORY_COLORS[marker.category] ?: 0xFF546E7A
                )
                val emoji = Constants.CATEGORY_EMOJIS[marker.category] ?: "📌"

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = categoryColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "$emoji ${marker.category}",
                            fontSize = 11.sp,
                            color = categoryColor,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Text(
                    text = marker.locationName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "by ${marker.authorUsername}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                // Stats always visible
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${marker.timesAnswered}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("Answered", fontSize = 11.sp, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${marker.timesAnsweredCorrectly}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF4CAF50)
                        )
                        Text("Correct", fontSize = 11.sp, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (successRate != null) "$successRate%" else "N/A",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text("Success rate", fontSize = 11.sp, color = Color.Gray)
                    }
                }

                HorizontalDivider()

                when {
                    isOwn -> {
                        // Own marker — show message
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                "📍 This is your marker!",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    alreadyAnswered -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF4CAF50).copy(alpha = 0.15f)
                        ) {
                            Text(
                                "✅ You already answered this question!",
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    !inProximity -> {
                        // Out of range — show info only, no question
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "📍 You are too far away!",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    "Get within ${ANSWER_PROXIMITY_METERS.toInt()}m of this location to answer the question.",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    hasAnswered -> {
                        // Show result
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCorrect)
                                Color(0xFF4CAF50).copy(alpha = 0.15f)
                            else
                                MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = if (isCorrect)
                                    "🎉 Correct! +${Constants.POINTS_ANSWER_QUESTION + Constants.POINTS_CORRECT_ANSWER} points"
                                else
                                    "❌ Wrong! +${Constants.POINTS_ANSWER_QUESTION} points",
                                color = if (isCorrect) Color(0xFF4CAF50)
                                else MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        // Show correct answer
                        marker.options.forEachIndexed { index, option ->
                            val isCorrectAnswer = index == marker.correctAnswerIndex
                            val wasSelected = index == selectedOption
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when {
                                        isCorrectAnswer -> "✅"
                                        wasSelected && !isCorrect -> "❌"
                                        else -> "○"
                                    },
                                    fontSize = 14.sp
                                )
                                Text(
                                    option,
                                    color = when {
                                        isCorrectAnswer -> Color(0xFF4CAF50)
                                        wasSelected && !isCorrect ->
                                            MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                    fontWeight = if (isCorrectAnswer) FontWeight.Bold
                                    else FontWeight.Normal
                                )
                            }
                        }

                        Button(
                            onClick = onAnswered,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Continue") }
                    }

                    else -> {
                        // In range — show question and options
                        Text(
                            text = marker.question,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )

                        marker.options.forEachIndexed { index, option ->
                            OutlinedButton(
                                onClick = { selectedOption = index },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selectedOption == index)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else Color.Transparent
                                )
                            ) {
                                Text(option)
                            }
                        }

                        Button(
                            onClick = {
                                if (selectedOption == -1 || currentUser == null) return@Button
                                scope.launch {
                                    isLoading = true
                                    val result = markerRepository.submitAnswer(
                                        marker, selectedOption, currentUser
                                    )
                                    result.onSuccess {
                                        isCorrect = it
                                        hasAnswered = true
                                    }
                                    isLoading = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            enabled = selectedOption != -1 && !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Text("Submit Answer")
                            }
                        }
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) { Text("Close") }
            }
        }
    }
}