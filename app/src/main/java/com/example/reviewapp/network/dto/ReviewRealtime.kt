package com.example.reviewapp.network.dto

import com.example.reviewapp.data.models.Review
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

fun FirebaseFirestore.observeByField(
    field: String,
    value: Any,
    limit: Long = 50
) = callbackFlow<List<Review>> {
    val reg = reviewsCollection()
        .whereEqualTo(field, value)
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(limit)
        .addSnapshotListener { snap, e ->
            if (e != null) { close(e); return@addSnapshotListener }
            trySend(snap?.toReviews().orEmpty())
        }

    awaitClose { reg.remove() }
}.conflate()

/** Stream global para o leaderboard. Define um limite alto (ex.: 5000) para não rebentar em produção. */
fun FirebaseFirestore.observeAll(limit: Long = 5000) = callbackFlow<List<Review>> {
    val reg = reviewsCollection()
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(limit)
        .addSnapshotListener { snap, e ->
            if (e != null) { close(e); return@addSnapshotListener }
            trySend(snap?.toReviews().orEmpty())
        }

    awaitClose { reg.remove() }
}.conflate()
