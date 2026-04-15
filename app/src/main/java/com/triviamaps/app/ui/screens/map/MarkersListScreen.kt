package com.triviamaps.app.ui.screens.map

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.google.android.gms.maps.model.LatLng
import com.triviamaps.app.data.Constants
import com.triviamaps.app.data.model.TriviaMarker
import com.triviamaps.app.data.repository.MarkerRepository
import kotlin.math.*

enum class MarkerSortOption(val label: String, val emoji: String) {
    MOST_ANSWERED("Most answered", "🔥"),
    LEAST_ANSWERED("Least answered", "💤"),
    NEWEST("Newest first", "🆕"),
    OLDEST("Oldest first", "🕰️"),
    HIGHEST_SUCCESS("Highest success rate", "✅"),
    LOWEST_SUCCESS("Lowest success rate", "❌"),
    CLOSEST("Closest to me", "📍"),
    FARTHEST("Farthest from me", "🌍")
}

fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2).pow(2)
    return r * 2 * atan2(sqrt(a), sqrt(1 - a))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkersListScreen(
    onBack: () -> Unit,
    onViewOnMap: (LatLng) -> Unit,
    userLocation: LatLng?
) {
    val markerRepository = remember { MarkerRepository() }
    var allMarkers by remember { mutableStateOf<List<TriviaMarker>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedSort by remember { mutableStateOf(MarkerSortOption.MOST_ANSWERED) }
    var selectedRadius by remember { mutableStateOf<Int?>(null) }
    var showSortDropdown by remember { mutableStateOf(false) }
    var selectedMarker by remember { mutableStateOf<TriviaMarker?>(null) }

    val categories = listOf("All") + Constants.CATEGORIES

    LaunchedEffect(Unit) {
        markerRepository.getAllMarkers().onSuccess { allMarkers = it }
        isLoading = false
    }

    // Apply search, category filter, radius filter, sort
    val filteredMarkers = remember(
        allMarkers, searchQuery, selectedCategory, selectedSort, selectedRadius
    ) {
        var result = allMarkers

        // Search by location name or author
        if (searchQuery.isNotBlank()) {
            result = result.filter {
                it.locationName.contains(searchQuery, ignoreCase = true) ||
                        it.authorUsername.contains(searchQuery, ignoreCase = true)
            }
        }

        // Category filter
        if (selectedCategory != "All") {
            result = result.filter { it.category == selectedCategory }
        }

        // Radius filter
        if (selectedRadius != null && userLocation != null) {
            result = result.filter { marker ->
                distanceKm(
                    userLocation.latitude, userLocation.longitude,
                    marker.latitude, marker.longitude
                ) <= selectedRadius!!
            }
        }

        // Sort
        result = when (selectedSort) {
            MarkerSortOption.MOST_ANSWERED -> result.sortedByDescending { it.timesAnswered }
            MarkerSortOption.LEAST_ANSWERED -> result.sortedBy { it.timesAnswered }
            MarkerSortOption.NEWEST -> result.sortedByDescending { it.createdAt }
            MarkerSortOption.OLDEST -> result.sortedBy { it.createdAt }
            MarkerSortOption.HIGHEST_SUCCESS -> result.sortedByDescending {
                if (it.timesAnswered > 0)
                    it.timesAnsweredCorrectly.toFloat() / it.timesAnswered
                else 0f
            }
            MarkerSortOption.LOWEST_SUCCESS -> result.sortedBy {
                if (it.timesAnswered > 0)
                    it.timesAnsweredCorrectly.toFloat() / it.timesAnswered
                else Float.MAX_VALUE
            }
            MarkerSortOption.CLOSEST -> if (userLocation != null) {
                result.sortedBy {
                    distanceKm(
                        userLocation.latitude, userLocation.longitude,
                        it.latitude, it.longitude
                    )
                }
            } else result
            MarkerSortOption.FARTHEST -> if (userLocation != null) {
                result.sortedByDescending {
                    distanceKm(
                        userLocation.latitude, userLocation.longitude,
                        it.latitude, it.longitude
                    )
                }
            } else result
        }

        result
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "All Markers (${filteredMarkers.size})",
                        fontWeight = FontWeight.Bold
                    )
                },
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
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by location or author...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            // Category filter chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                        color = if (isSelected) categoryColor.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) categoryColor
                            else Color.Gray.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(emoji, fontSize = 13.sp)
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

            // Sort + Radius row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sort dropdown
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSortDropdown = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, Color.Gray.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = 12.dp, vertical = 10.dp
                            ),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${selectedSort.emoji} ${selectedSort.label}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = showSortDropdown,
                        onDismissRequest = { showSortDropdown = false }
                    ) {
                        MarkerSortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "${option.emoji} ${option.label}",
                                        fontWeight = if (selectedSort == option)
                                            FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedSort == option)
                                            MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    selectedSort = option
                                    showSortDropdown = false
                                }
                            )
                        }
                    }
                }

                // Radius filter
                if (userLocation != null) {
                    Box {
                        val radiusOptions = listOf(null, 1, 5, 10, 25, 50)
                        var showRadiusDropdown by remember { mutableStateOf(false) }
                        Surface(
                            modifier = Modifier.clickable { showRadiusDropdown = true },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedRadius != null)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (selectedRadius != null) 2.dp else 1.dp,
                                color = if (selectedRadius != null)
                                    MaterialTheme.colorScheme.primary
                                else Color.Gray.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = 12.dp, vertical = 10.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (selectedRadius != null)
                                        "📍 ${selectedRadius}km"
                                    else "📍 Radius",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (selectedRadius != null)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = showRadiusDropdown,
                            onDismissRequest = { showRadiusDropdown = false }
                        ) {
                            radiusOptions.forEach { radius ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (radius == null) "Any distance"
                                            else "Within ${radius}km",
                                            fontWeight = if (selectedRadius == radius)
                                                FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedRadius == radius)
                                                MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        selectedRadius = radius
                                        showRadiusDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredMarkers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🔍", fontSize = 48.sp)
                        Text(
                            "No markers found",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            "Try adjusting your search or filters",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredMarkers) { marker ->
                        MarkerListItem(
                            marker = marker,
                            userLocation = userLocation,
                            onClick = { selectedMarker = marker }
                        )
                    }
                }
            }
        }
    }

    // Marker detail dialog
    selectedMarker?.let { marker ->
        MarkerListDetailDialog(
            marker = marker,
            userLocation = userLocation,
            onDismiss = { selectedMarker = null },
            onViewOnMap = {
                selectedMarker = null
                onViewOnMap(LatLng(marker.latitude, marker.longitude))
            }
        )
    }
}

@Composable
fun MarkerListItem(
    marker: TriviaMarker,
    userLocation: LatLng?,
    onClick: () -> Unit
) {
    val categoryColor = Color(Constants.CATEGORY_COLORS[marker.category] ?: 0xFF546E7A)
    val emoji = Constants.CATEGORY_EMOJIS[marker.category] ?: "📌"

    val distanceText = userLocation?.let {
        val km = distanceKm(
            it.latitude, it.longitude,
            marker.latitude, marker.longitude
        )
        if (km < 1.0) "${(km * 1000).toInt()}m away"
        else "${"%.1f".format(km)}km away"
    }

    val successRate = if (marker.timesAnswered > 0)
        (marker.timesAnsweredCorrectly * 100 / marker.timesAnswered)
    else null

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image
            if (marker.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = marker.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                // Category + distance row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$emoji ${marker.category}",
                        fontSize = 10.sp,
                        color = categoryColor,
                        fontWeight = FontWeight.Medium
                    )
                    distanceText?.let {
                        Text(
                            text = it,
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = marker.locationName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Text(
                    text = "by ${marker.authorUsername}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Stats row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "👁️ ${marker.timesAnswered}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (successRate != null) {
                        Text(
                            "✅ $successRate%",
                            fontSize = 11.sp,
                            color = when {
                                successRate >= 70 -> Color(0xFF4CAF50)
                                successRate >= 40 -> Color(0xFFFFA000)
                                else -> MaterialTheme.colorScheme.error
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MarkerListDetailDialog(
    marker: TriviaMarker,
    userLocation: LatLng?,
    onDismiss: () -> Unit,
    onViewOnMap: () -> Unit
) {
    val categoryColor = Color(Constants.CATEGORY_COLORS[marker.category] ?: 0xFF546E7A)
    val emoji = Constants.CATEGORY_EMOJIS[marker.category] ?: "📌"

    val distanceText = userLocation?.let {
        val km = distanceKm(
            it.latitude, it.longitude,
            marker.latitude, marker.longitude
        )
        if (km < 1.0) "${(km * 1000).toInt()}m away"
        else "${"%.1f".format(km)}km away"
    }

    val successRate = if (marker.timesAnswered > 0)
        (marker.timesAnsweredCorrectly * 100 / marker.timesAnswered)
    else null

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Image
                if (marker.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = marker.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                // Category + distance
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = categoryColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "$emoji ${marker.category}",
                            fontSize = 12.sp,
                            color = categoryColor,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(
                                horizontal = 10.dp, vertical = 4.dp
                            )
                        )
                    }
                    distanceText?.let {
                        Text(it, fontSize = 12.sp, color = Color.Gray)
                    }
                }

                // Name + author
                Text(
                    text = marker.locationName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "by ${marker.authorUsername}",
                    fontSize = 13.sp,
                    color = Color.Gray
                )

                // Coordinates
                Text(
                    text = "📍 ${"%.4f".format(marker.latitude)}, ${"%.4f".format(marker.longitude)}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

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
                        Text(
                            if (successRate != null) "$successRate%" else "N/A",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = when {
                                successRate == null -> Color.Gray
                                successRate >= 70 -> Color(0xFF4CAF50)
                                successRate >= 40 -> Color(0xFFFFA000)
                                else -> MaterialTheme.colorScheme.error
                            }
                        )
                        Text("Success rate", fontSize = 11.sp, color = Color.Gray)
                    }
                }

                HorizontalDivider()

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