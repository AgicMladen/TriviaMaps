package com.triviamaps.app.ui.screens.map

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.location.Location
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.triviamaps.app.data.Constants
import com.triviamaps.app.data.model.TriviaMarker
import com.triviamaps.app.data.model.User
import com.triviamaps.app.data.repository.AuthRepository
import com.triviamaps.app.data.repository.MarkerRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor

const val PLACEMENT_RADIUS_METERS = 25.0
const val FILTER_MY_MARKERS = "My Markers"

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToMarkersList: () -> Unit = {},
    focusLocation: LatLng? = null,
    onFocusConsumed: () -> Unit = {},
    onUserLocationUpdated: (LatLng) -> Unit = {},
    showWelcome: Int = 0,
    onWelcomeDismissed: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("triviamaps_prefs", android.content.Context.MODE_PRIVATE)
    }
    val notchMode = remember { prefs.getBoolean("notch_mode", false) }
    val topBarPadding = if (notchMode) 48.dp else 12.dp
    val scope = rememberCoroutineScope()
    val markerRepository = remember { MarkerRepository() }
    val authRepository = remember { AuthRepository() }

    var currentUser by remember { mutableStateOf<User?>(null) }
    var markers by remember { mutableStateOf<List<TriviaMarker>>(emptyList()) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var selectedMarker by remember { mutableStateOf<TriviaMarker?>(null) }
    var showAddMarkerDialog by remember { mutableStateOf(false) }
    var notifiedMarkerIds by remember { mutableStateOf(setOf<String>()) }
    var hasInitializedCamera by remember { mutableStateOf(false) }
    var isPlacingMarker by remember { mutableStateOf(false) }
    var placementOutOfRange by remember { mutableStateOf(false) }
    var locationUnavailable by remember { mutableStateOf(false) }
    var showFilterBar by remember { mutableStateOf(false) }
    var selectedFilters by remember { mutableStateOf(setOf<String>()) }

    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    val fineGranted = locationPermissions.permissions
        .first { it.permission == Manifest.permission.ACCESS_FINE_LOCATION }
        .status.isGranted

    val coarseGranted = locationPermissions.permissions
        .first { it.permission == Manifest.permission.ACCESS_COARSE_LOCATION }
        .status.isGranted

    val permissionsGranted = fineGranted || coarseGranted

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(44.0, 21.0), 6f)
    }

    val crosshairLocation by remember {
        derivedStateOf { cameraPositionState.position.target }
    }

    val filterCategories = listOf("All") + Constants.CATEGORIES + listOf(FILTER_MY_MARKERS)

    // Multi-select filter logic
    val filteredMarkers = when {
        selectedFilters.isEmpty() -> markers
        selectedFilters.contains(FILTER_MY_MARKERS) && selectedFilters.size == 1 ->
            markers.filter { it.authorUid == currentUser?.uid }
        selectedFilters.contains(FILTER_MY_MARKERS) ->
            markers.filter {
                it.authorUid == currentUser?.uid ||
                        selectedFilters.contains(it.category)
            }
        else -> markers.filter { selectedFilters.contains(it.category) }
    }

    val isFilterActive = selectedFilters.isNotEmpty()

    if (isPlacingMarker) {
        userLocation?.let { userLoc ->
            val userAndroid = Location("").apply {
                latitude = userLoc.latitude
                longitude = userLoc.longitude
            }
            val crosshairAndroid = Location("").apply {
                latitude = crosshairLocation.latitude
                longitude = crosshairLocation.longitude
            }
            placementOutOfRange =
                userAndroid.distanceTo(crosshairAndroid) > PLACEMENT_RADIUS_METERS
        }
    }

    LaunchedEffect(Unit) {
        authRepository.getCurrentUserData().onSuccess { currentUser = it }
        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        firestore.collection(com.triviamaps.app.data.Constants.MARKERS_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                markers = snapshot.documents.mapNotNull {
                    it.toObject(com.triviamaps.app.data.model.TriviaMarker::class.java)
                }
            }
    }

    LaunchedEffect(Unit) {
        if (!permissionsGranted) {
            locationPermissions.launchMultiplePermissionRequest()
        }
    }

    LaunchedEffect(focusLocation) {
        focusLocation?.let {
            cameraPositionState.animate(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.fromLatLngZoom(it, 19f)
                )
            )
            onFocusConsumed()
        }
    }

    LaunchedEffect(fineGranted, coarseGranted) {
        if (!fineGranted && !coarseGranted) return@LaunchedEffect
        locationUnavailable = false
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        while (true) {
            try {
                var location: Location? = fusedClient.getCurrentLocation(
                    if (fineGranted) Priority.PRIORITY_HIGH_ACCURACY
                    else Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    null
                ).await()

                if (location == null) {
                    location = fusedClient.lastLocation.await()
                }

                if (location != null) {
                    locationUnavailable = false
                    val latLng = LatLng(location.latitude, location.longitude)
                    userLocation = latLng
                    onUserLocationUpdated(latLng)
                    if (!hasInitializedCamera) {
                        cameraPositionState.position =
                            CameraPosition.fromLatLngZoom(latLng, 18f)
                        hasInitializedCamera = true
                    }

                    markers.forEach { marker ->
                        val markerLocation = Location("").apply {
                            latitude = marker.latitude
                            longitude = marker.longitude
                        }
                        val distance = location.distanceTo(markerLocation)
                        if (distance <= Constants.PROXIMITY_RADIUS_METERS &&
                            marker.id !in notifiedMarkerIds
                        ) {
                            val notificationsEnabled = context
                                .getSharedPreferences(
                                    "triviamaps_prefs",
                                    android.content.Context.MODE_PRIVATE
                                )
                                .getBoolean("notifications_enabled", true)

                            if (notificationsEnabled) {
                                scope.launch {
                                    val alreadyAnswered = currentUser?.let { user ->
                                        markerRepository.hasUserAnsweredMarker(marker.id, user.uid)
                                    } ?: false
                                    val isOwn = marker.authorUid == currentUser?.uid
                                    if (!alreadyAnswered && !isOwn) {
                                        sendProximityNotification(context, marker)
                                    }
                                }
                            }
                            notifiedMarkerIds = notifiedMarkerIds + marker.id
                        }
                    }
                } else {
                    locationUnavailable = true
                }
            } catch (e: Exception) {
                locationUnavailable = true
            }
            delay(10_000)
        }
    }

    if (!permissionsGranted && !locationPermissions.shouldShowRationale) {
        LocationUnavailableBanner(
            message = "Location permission is required for TriviaMaps to work.",
            buttonText = "Open Settings",
            onButtonClick = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.fromParts(
                            "package", context.packageName, null
                        )
                    }
                )
            }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = permissionsGranted),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = !isPlacingMarker,
                zoomControlsEnabled = false
            )
        ) {
            if (!isPlacingMarker) {
                filteredMarkers.forEach { marker ->
                    val isOwn = marker.authorUid == currentUser?.uid
                    Marker(
                        state = MarkerState(LatLng(marker.latitude, marker.longitude)),
                        title = marker.locationName,
                        snippet = "by ${marker.authorUsername}",
                        icon = BitmapDescriptorFactory.defaultMarker(
                            if (isOwn) BitmapDescriptorFactory.HUE_RED
                            else Constants.CATEGORY_HUES[marker.category]
                                ?: BitmapDescriptorFactory.HUE_AZURE
                        ),
                        onClick = {
                            selectedMarker = marker
                            false
                        }
                    )
                }
            }

            if (isPlacingMarker) {
                userLocation?.let { userLoc ->
                    Circle(
                        center = userLoc,
                        radius = PLACEMENT_RADIUS_METERS,
                        strokeColor = if (placementOutOfRange)
                            Color.Red.copy(alpha = 0.9f)
                        else Color(0xFF6C63FF).copy(alpha = 0.9f),
                        strokeWidth = 4f,
                        fillColor = if (placementOutOfRange)
                            Color.Red.copy(alpha = 0.08f)
                        else Color(0xFF6C63FF).copy(alpha = 0.08f)
                    )
                }
            }
        }

        if (isPlacingMarker) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📍",
                    fontSize = 40.sp,
                    modifier = Modifier.offset(y = 8.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = topBarPadding, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 4.dp
                ) {
                    Text(
                        text = "🗺️ TriviaMaps",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (!isPlacingMarker) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallFab(onClick = { showFilterBar = !showFilterBar }) {
                            Box {
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = "Filter",
                                    tint = if (isFilterActive)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                if (isFilterActive) {
                                    Badge(
                                        modifier = Modifier.align(Alignment.TopEnd),
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        SmallFab(onClick = onNavigateToMarkersList) {
                            Icon(
                                Icons.Default.List,
                                contentDescription = "Markers List",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        SmallFab(onClick = onNavigateToLeaderboard) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Leaderboard",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        SmallFab(onClick = onNavigateToProfile) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showFilterBar && !isPlacingMarker,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Filter Markers",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isFilterActive) {
                                TextButton(
                                    onClick = { selectedFilters = emptySet() },
                                    contentPadding = PaddingValues(
                                        horizontal = 8.dp,
                                        vertical = 0.dp
                                    )
                                ) {
                                    Text(
                                        "Clear",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            filterCategories.forEach { category ->
                                val isSelected = if (category == "All")
                                    selectedFilters.isEmpty()
                                else
                                    selectedFilters.contains(category)

                                val categoryColor = when (category) {
                                    "All" -> MaterialTheme.colorScheme.primary
                                    FILTER_MY_MARKERS -> Color(0xFF6C63FF)
                                    else -> Color(
                                        Constants.CATEGORY_COLORS[category] ?: 0xFF546E7A
                                    )
                                }
                                val emoji = when (category) {
                                    "All" -> "🗺️"
                                    FILTER_MY_MARKERS -> "👤"
                                    else -> Constants.CATEGORY_EMOJIS[category] ?: "📌"
                                }

                                Surface(
                                    modifier = Modifier.clickable {
                                        selectedFilters = when (category) {
                                            "All" -> emptySet()
                                            else -> {
                                                if (selectedFilters.contains(category))
                                                    selectedFilters - category
                                                else
                                                    selectedFilters + category
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected)
                                        categoryColor.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.background,
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
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = categoryColor
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Showing ${filteredMarkers.size} of ${markers.size} markers",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        if (locationUnavailable && permissionsGranted) {
            LocationUnavailableBanner(
                message = "Cannot get your location. Please enable GPS or Wi-Fi location.",
                buttonText = "Location Settings",
                onButtonClick = {
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 72.dp, start = 16.dp, end = 16.dp)
            )
        }

        if (isPlacingMarker) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 72.dp, start = 24.dp, end = 24.dp),
                shape = RoundedCornerShape(16.dp),
                color = if (placementOutOfRange)
                    MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 4.dp
            ) {
                Text(
                    text = if (placementOutOfRange)
                        "⚠️ Outside your 25m range!\nPan back towards your location."
                    else
                        "Pan the map to position the 📍\nthen confirm your location",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    color = if (placementOutOfRange)
                        MaterialTheme.colorScheme.onErrorContainer
                    else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        if (isPlacingMarker) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FloatingActionButton(
                    onClick = {
                        isPlacingMarker = false
                        placementOutOfRange = false
                        userLocation?.let {
                            scope.launch {
                                cameraPositionState.position =
                                    CameraPosition.fromLatLngZoom(it, 18f)
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }

                ExtendedFloatingActionButton(
                    onClick = {
                        if (!placementOutOfRange) showAddMarkerDialog = true
                    },
                    containerColor = if (placementOutOfRange) Color.Gray
                    else MaterialTheme.colorScheme.primary,
                    icon = {
                        Icon(Icons.Default.Check, contentDescription = "Confirm", tint = Color.White)
                    },
                    text = {
                        Text("Confirm Location", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                )
            }
        } else {
            currentUser?.let { user ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(24.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 4.dp
                ) {
                    Text(
                        text = "⭐ ${user.totalPoints} pts",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FloatingActionButton(
                    onClick = {
                        userLocation?.let {
                            scope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newCameraPosition(
                                        CameraPosition.fromLatLngZoom(it, 18f)
                                    )
                                )
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription = "Recenter",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                FloatingActionButton(
                    onClick = {
                        if (userLocation != null) {
                            isPlacingMarker = true
                            placementOutOfRange = false
                            showFilterBar = false
                            scope.launch {
                                userLocation?.let {
                                    cameraPositionState.position =
                                        CameraPosition.fromLatLngZoom(it, 19f)
                                }
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add marker", tint = Color.White)
                }
            }
        }
    }

    selectedMarker?.let { marker ->
        TriviaDialog(
            marker = marker,
            currentUser = currentUser,
            markerRepository = markerRepository,
            userLocation = userLocation,
            onDismiss = { selectedMarker = null },
            onAnswered = {
                selectedMarker = null
                scope.launch {
                    authRepository.getCurrentUserData().onSuccess { currentUser = it }
                }
            }
        )
    }

    if (showAddMarkerDialog) {
        AddMarkerDialog(
            location = crosshairLocation,
            currentUser = currentUser,
            markerRepository = markerRepository,
            context = context,
            onDismiss = {
                showAddMarkerDialog = false
                isPlacingMarker = false
                userLocation?.let {
                    scope.launch {
                        cameraPositionState.position =
                            CameraPosition.fromLatLngZoom(it, 18f)
                    }
                }
            },
            onMarkerAdded = { newMarker ->
                markers = markers + newMarker
                showAddMarkerDialog = false
                isPlacingMarker = false
                userLocation?.let {
                    scope.launch {
                        cameraPositionState.position =
                            CameraPosition.fromLatLngZoom(it, 18f)
                    }
                }
                scope.launch {
                    authRepository.getCurrentUserData().onSuccess { currentUser = it }
                }
            }
        )
    }

    if (locationPermissions.shouldShowRationale) {
        AlertDialog(
            onDismissRequest = {},
            icon = {
                Icon(
                    Icons.Default.LocationOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Location Required", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "TriviaMaps needs your location to show nearby trivia markers, " +
                            "let you place new markers, and detect when you're close to points of interest.",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(onClick = { locationPermissions.launchMultiplePermissionRequest() }) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = android.net.Uri.fromParts("package", context.packageName, null)
                        }
                    )
                }) { Text("Open Settings") }
            }
        )
    }

    if (showWelcome != 0) {
        WelcomeDialog(
            isNewUser = showWelcome == 1,
            onDismiss = onWelcomeDismissed
        )
    }
}

@Composable
fun LocationUnavailableBanner(
    message: String,
    buttonText: String,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.LocationOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Button(
                onClick = onButtonClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(buttonText, color = Color.White)
            }
        }
    }
}

@Composable
fun SmallFab(onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 4.dp,
        modifier = Modifier.size(44.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onSurface
            ) {
                content()
            }
        }
    }
}

fun sendProximityNotification(context: Context, marker: TriviaMarker) {
    val channelId = "proximity_channel"
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channel = NotificationChannel(
        channelId, "Nearby Trivia",
        NotificationManager.IMPORTANCE_DEFAULT
    )
    notificationManager.createNotificationChannel(channel)
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("📍 Trivia nearby!")
        .setContentText("You're near: ${marker.locationName}. Tap to answer!")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .build()
    notificationManager.notify(marker.id.hashCode(), notification)
}