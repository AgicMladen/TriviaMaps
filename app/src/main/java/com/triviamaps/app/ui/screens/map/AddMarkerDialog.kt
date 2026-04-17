package com.triviamaps.app.ui.screens.map

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.model.LatLng
import com.triviamaps.app.data.Constants
import com.triviamaps.app.data.model.TriviaMarker
import com.triviamaps.app.data.model.User
import com.triviamaps.app.data.repository.CloudinaryRepository
import com.triviamaps.app.data.repository.MarkerRepository
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.runtime.saveable.rememberSaveable

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AddMarkerDialog(
    location: LatLng,
    currentUser: User?,
    markerRepository: MarkerRepository,
    context: Context,
    onDismiss: () -> Unit,
    onMarkerAdded: (TriviaMarker) -> Unit
) {
    val scope = rememberCoroutineScope()
    val cloudinaryRepository = remember { CloudinaryRepository() }

    var locationName by rememberSaveable { mutableStateOf("") }
    var question by rememberSaveable { mutableStateOf("") }
    var options by rememberSaveable { mutableStateOf(listOf("", "", "", "")) }
    var correctAnswerIndex by rememberSaveable { mutableStateOf(0) }
    var imageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var selectedCategory by rememberSaveable { mutableStateOf(Constants.CATEGORIES[0]) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf("") }
    //var waitingForCameraPermission by remember { mutableStateOf(false) }

    val cameraPermissionState = rememberPermissionState(
        android.Manifest.permission.CAMERA
    )

    // Auto-launch camera after permission is granted
    var waitingForCameraPermission by remember { mutableStateOf(false) }

    val cameraImageUri = remember {
        val tempFile = File.createTempFile("trivia_photo_", ".jpg", context.cacheDir)
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            tempFile
        )
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { imageUri = it } }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) imageUri = cameraImageUri
    }

    LaunchedEffect(cameraPermissionState.status.isGranted) {
        if (cameraPermissionState.status.isGranted && waitingForCameraPermission) {
            cameraLauncher.launch(cameraImageUri)
            waitingForCameraPermission = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Add Trivia Marker",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    "📍 ${String.format("%.4f", location.latitude)}, " +
                            "${String.format("%.4f", location.longitude)}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                // Image preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .border(
                            1.dp,
                            if (imageUri == null) MaterialTheme.colorScheme.primary
                            else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    "Tap below to change",
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(
                                        horizontal = 8.dp,
                                        vertical = 4.dp
                                    )
                                )
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("📸", fontSize = 32.sp)
                            Text(
                                "Location photo required *",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Use camera or choose from gallery",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Camera / Gallery buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (cameraPermissionState.status.isGranted) {
                                cameraLauncher.launch(cameraImageUri)
                            } else {
                                waitingForCameraPermission = true
                                cameraPermissionState.launchPermissionRequest()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Camera", fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gallery", fontSize = 13.sp)
                    }
                }

                // Location name
                OutlinedTextField(
                    value = locationName,
                    onValueChange = { locationName = it },
                    label = {
                        Row {
                            Text("Location name")
                            Text(" *", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Category selector
                Text(
                    "Category *",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Constants.CATEGORIES.chunked(2).forEach { rowCategories ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowCategories.forEach { category ->
                                val isSelected = selectedCategory == category
                                val categoryColor = Color(
                                    Constants.CATEGORY_COLORS[category] ?: 0xFF546E7A
                                )
                                val emoji = Constants.CATEGORY_EMOJIS[category] ?: "📌"
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedCategory = category },
                                    shape = RoundedCornerShape(12.dp),
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
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(emoji, fontSize = 22.sp)
                                        Text(
                                            text = category,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected)
                                                FontWeight.Bold
                                            else FontWeight.Normal,
                                            color = if (isSelected) categoryColor
                                            else MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                            if (rowCategories.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // Question
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = {
                        Row {
                            Text("Trivia question")
                            Text(" *", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )

                // Answer options
                Text(
                    "Answer options — tap ○ to mark correct *",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                options.forEachIndexed { index, option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = correctAnswerIndex == index,
                            onClick = { correctAnswerIndex = index },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        OutlinedTextField(
                            value = option,
                            onValueChange = { newVal ->
                                options = options.toMutableList().also { it[index] = newVal }
                            },
                            label = {
                                Row {
                                    Text("Option ${index + 1}")
                                    Text(" *", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }

                if (errorMessage.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⚠️", fontSize = 16.sp)
                            Text(
                                errorMessage,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        when {
                            imageUri == null ->
                                errorMessage = "Please add a photo of the location"
                            locationName.isBlank() ->
                                errorMessage = "Enter a location name"
                            question.isBlank() ->
                                errorMessage = "Enter a question"
                            options.any { it.isBlank() } ->
                                errorMessage = "Fill in all answer options"
                            currentUser == null ->
                                errorMessage = "Not logged in"
                            else -> {
                                scope.launch {
                                    isLoading = true
                                    errorMessage = ""
                                    val uploadResult = cloudinaryRepository.uploadImage(
                                        context, imageUri!!
                                    )
                                    uploadResult.fold(
                                        onSuccess = { imageUrl ->
                                            val marker = TriviaMarker(
                                                authorUid = currentUser.uid,
                                                authorUsername = currentUser.username,
                                                latitude = location.latitude,
                                                longitude = location.longitude,
                                                locationName = locationName.trim(),
                                                imageUrl = imageUrl,
                                                question = question.trim(),
                                                options = options.map { it.trim() },
                                                correctAnswerIndex = correctAnswerIndex,
                                                category = selectedCategory
                                            )
                                            markerRepository.addMarker(marker).fold(
                                                onSuccess = { onMarkerAdded(marker) },
                                                onFailure = {
                                                    errorMessage = it.message
                                                        ?: "Failed to save marker"
                                                }
                                            )
                                        },
                                        onFailure = {
                                            errorMessage = "Image upload failed: ${it.message}"
                                        }
                                    )
                                    isLoading = false
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text("Add Marker", fontWeight = FontWeight.Bold)
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) { Text("Cancel") }
            }
        }
    }
}