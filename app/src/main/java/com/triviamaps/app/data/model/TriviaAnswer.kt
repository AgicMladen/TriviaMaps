package com.triviamaps.app.data.model

data class TriviaAnswer(
    val id: String = "",
    val markerId: String = "",
    val userUid: String = "",
    val username: String = "",
    val selectedAnswerIndex: Int = 0,
    val isCorrect: Boolean = false,
    val answeredAt: Long = System.currentTimeMillis()
)