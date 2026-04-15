package com.triviamaps.app.data.model

data class TriviaMarker(
    val id: String = "",
    val authorUid: String = "",
    val authorUsername: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val locationName: String = "",
    val imageUrl: String = "",
    val question: String = "",
    val options: List<String> = emptyList(),
    val correctAnswerIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val timesAnswered: Int = 0,
    val timesAnsweredCorrectly: Int = 0,
    val category: String = "General"
)