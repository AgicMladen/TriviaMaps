package com.triviamaps.app.data

object Constants {
    // Cloudinary
    const val CLOUDINARY_CLOUD_NAME = "dvwjxocd7"
    const val CLOUDINARY_UPLOAD_PRESET = "triviamaps_preset"
    const val CLOUDINARY_BASE_URL = "https://api.cloudinary.com/v1_1/$CLOUDINARY_CLOUD_NAME/image/upload"

    // Firestore collections
    const val USERS_COLLECTION = "users"
    const val MARKERS_COLLECTION = "markers"
    const val ANSWERS_COLLECTION = "answers"

    // Points
    const val POINTS_CREATE_QUESTION = 10
    const val POINTS_SOMEONE_ANSWERS = 2
    const val POINTS_ANSWER_QUESTION = 5
    const val POINTS_CORRECT_ANSWER = 10

    // Proximity radius in meters
    const val PROXIMITY_RADIUS_METERS = 100f

    // Categories
    val CATEGORIES = listOf(
        "Natural Wonders",
        "Historical Buildings",
        "Monuments & Sculptures",
        "Hidden Gems",
        "Other"
    )

    val CATEGORY_EMOJIS = mapOf(
        "Natural Wonders" to "🌿",
        "Historical Buildings" to "🏛️",
        "Monuments & Sculptures" to "🗿",
        "Hidden Gems" to "💎",
        "Other" to "📌"
    )

    val CATEGORY_COLORS = mapOf(
        "Natural Wonders" to 0xFF2E7D32,      // Deep Green
        "Historical Buildings" to 0xFFE65100,  // Deep Orange
        "Monuments & Sculptures" to 0xFF6A1B9A, // Purple
        "Hidden Gems" to 0xFF00695C,           // Teal
        "Other" to 0xFF546E7A                  // Blue Gray
    )

    // Google Maps marker hues for each category
    val CATEGORY_HUES = mapOf(
        "Natural Wonders" to 120f,       // Green
        "Historical Buildings" to 30f,   // Orange
        "Monuments & Sculptures" to 290f, // Violet
        "Hidden Gems" to 174f,           // Teal
        "Other" to 200f                  // Blue Gray
    )
}