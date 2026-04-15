package com.triviamaps.app.data.model

data class User(
    val uid: String = "",
    val username: String = "",
    val fullName: String = "",
    val phoneNumber: String = "",
    val profileImageUrl: String = "",
    val country: String = "",
    val totalPoints: Int = 0,
    val questionPoints: Int = 0,
    val answerPoints: Int = 0,
    val correctAnswerPoints: Int = 0,
    val questionsCreated: Int = 0,
    val questionsAnswered: Int = 0
)