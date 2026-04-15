package com.triviamaps.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.triviamaps.app.R
import com.triviamaps.app.data.repository.AuthRepository
import com.triviamaps.app.data.repository.MarkerRepository

@Composable
fun SplashScreen(onFinished: (isLoggedIn: Boolean) -> Unit) {
    val authRepository = remember { AuthRepository() }
    val markerRepository = remember { MarkerRepository() }

    val infiniteTransition = rememberInfiniteTransition(label = "splash")

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    var statusText by remember { mutableStateOf("Starting up...") }

    LaunchedEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            statusText = "Loading your profile..."
            authRepository.getCurrentUserData()
            statusText = "Loading map data..."
            markerRepository.getAllMarkers()
            statusText = "Ready!"
            onFinished(true)
        } else {
            onFinished(false)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A0A0A),
                        Color(0xFF2D1010),
                        Color(0xFF1A0A0A)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Logo — no circle clip, same as login screen
            Image(
                painter = painterResource(id = R.drawable.triviamaps_logo),
                contentDescription = "TriviaMaps Logo",
                modifier = Modifier
                    .size(160.dp)
                    .scale(pulse),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Animated dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val dotScale by infiniteTransition.animateFloat(
                        initialValue = 0.6f,
                        targetValue = 1.2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(500, easing = EaseInOutCubic),
                            repeatMode = RepeatMode.Reverse,
                            initialStartOffset = StartOffset(index * 160)
                        ),
                        label = "dot$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .scale(dotScale)
                            .background(
                                Color(0xFFC4392B).copy(alpha = 0.4f + (dotScale - 0.6f)),
                                CircleShape
                            )
                    )
                }
            }

            Text(
                text = statusText,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.4f)
            )
        }

        Text(
            text = "v1.0",
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.3f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}