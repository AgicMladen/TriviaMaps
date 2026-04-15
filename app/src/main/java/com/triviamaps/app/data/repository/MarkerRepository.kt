package com.triviamaps.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.triviamaps.app.data.Constants
import com.triviamaps.app.data.model.TriviaAnswer
import com.triviamaps.app.data.model.TriviaMarker
import com.triviamaps.app.data.model.User
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MarkerRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun getAllMarkers(): Result<List<TriviaMarker>> {
        return try {
            val snapshot = firestore.collection(Constants.MARKERS_COLLECTION).get().await()
            val markers = snapshot.documents.mapNotNull { it.toObject(TriviaMarker::class.java) }
            Result.success(markers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addMarker(marker: TriviaMarker): Result<Unit> {
        return try {
            val id = UUID.randomUUID().toString()
            val markerWithId = marker.copy(id = id)
            firestore.collection(Constants.MARKERS_COLLECTION).document(id).set(markerWithId).await()

            // Give points + increment questionsCreated
            val userRef = firestore.collection(Constants.USERS_COLLECTION).document(marker.authorUid)
            firestore.runTransaction { transaction ->
                val snap = transaction.get(userRef)
                val total = (snap.getLong("totalPoints") ?: 0) + Constants.POINTS_CREATE_QUESTION
                val qPoints = (snap.getLong("questionPoints") ?: 0) + Constants.POINTS_CREATE_QUESTION
                val qCreated = (snap.getLong("questionsCreated") ?: 0) + 1
                transaction.update(userRef, mapOf(
                    "totalPoints" to total,
                    "questionPoints" to qPoints,
                    "questionsCreated" to qCreated
                ))
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitAnswer(
        marker: TriviaMarker,
        selectedIndex: Int,
        user: User
    ): Result<Boolean> {
        return try {
            val isCorrect = selectedIndex == marker.correctAnswerIndex
            val answer = TriviaAnswer(
                id = UUID.randomUUID().toString(),
                markerId = marker.id,
                userUid = user.uid,
                username = user.username,
                selectedAnswerIndex = selectedIndex,
                isCorrect = isCorrect
            )

            firestore.collection(Constants.ANSWERS_COLLECTION)
                .document(answer.id).set(answer).await()

            // Update marker stats
            val markerRef = firestore.collection(Constants.MARKERS_COLLECTION).document(marker.id)
            firestore.runTransaction { transaction ->
                val snap = transaction.get(markerRef)
                val currentAnswered = snap.getLong("timesAnswered") ?: 0
                val currentCorrect = snap.getLong("timesAnsweredCorrectly") ?: 0
                transaction.update(markerRef, "timesAnswered", currentAnswered + 1)
                if (isCorrect) {
                    transaction.update(markerRef, "timesAnsweredCorrectly", currentCorrect + 1)
                }
            }.await()

            // Points for answerer
            val answerDelta = if (isCorrect)
                Constants.POINTS_ANSWER_QUESTION + Constants.POINTS_CORRECT_ANSWER
            else
                Constants.POINTS_ANSWER_QUESTION

            // Update answerer points + questionsAnswered counter
            val answererRef = firestore.collection(Constants.USERS_COLLECTION).document(user.uid)
            firestore.runTransaction { transaction ->
                val snap = transaction.get(answererRef)
                val total = (snap.getLong("totalPoints") ?: 0) + answerDelta
                val aPoints = (snap.getLong("answerPoints") ?: 0) + answerDelta
                val qAnswered = (snap.getLong("questionsAnswered") ?: 0) + 1
                transaction.update(answererRef, mapOf(
                    "totalPoints" to total,
                    "answerPoints" to aPoints,
                    "questionsAnswered" to qAnswered
                ))
            }.await()

            // Points for question author (someone answered their question)
            updateUserPoints(
                uid = marker.authorUid,
                totalDelta = Constants.POINTS_SOMEONE_ANSWERS,
                questionDelta = Constants.POINTS_SOMEONE_ANSWERS
            )

            Result.success(isCorrect)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun hasUserAnsweredMarker(markerId: String, userUid: String): Boolean {
        return try {
            val snapshot = firestore.collection(Constants.ANSWERS_COLLECTION)
                .whereEqualTo("markerId", markerId)
                .whereEqualTo("userUid", userUid)
                .get().await()
            !snapshot.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getLeaderboard(): Result<List<User>> {
        return try {
            val snapshot = firestore.collection(Constants.USERS_COLLECTION)
                .orderBy("totalPoints", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(100)
                .get().await()
            val users = snapshot.documents.mapNotNull { it.toObject(User::class.java) }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserMarkers(uid: String): Result<List<TriviaMarker>> {
        return try {
            val snapshot = firestore.collection(Constants.MARKERS_COLLECTION)
                .whereEqualTo("authorUid", uid)
                .get().await()
            val markers = snapshot.documents.mapNotNull { it.toObject(TriviaMarker::class.java) }
            Result.success(markers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun updateUserPoints(
        uid: String,
        totalDelta: Int,
        questionDelta: Int = 0,
        answerDelta: Int = 0
    ) {
        val userRef = firestore.collection(Constants.USERS_COLLECTION).document(uid)
        firestore.runTransaction { transaction ->
            val snap = transaction.get(userRef)
            val total = (snap.getLong("totalPoints") ?: 0) + totalDelta
            val question = (snap.getLong("questionPoints") ?: 0) + questionDelta
            val answer = (snap.getLong("answerPoints") ?: 0) + answerDelta
            transaction.update(userRef, mapOf(
                "totalPoints" to total,
                "questionPoints" to question,
                "answerPoints" to answer
            ))
        }.await()
    }
}