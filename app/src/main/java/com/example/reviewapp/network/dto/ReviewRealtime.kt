package com.example.reviewapp.network.dto

import com.example.reviewapp.data.models.Review
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

/**
 * Observa reviews filtradas por um campo (ex.: `"placeId"` ou `"userId"`),
 * ordenadas por `createdAt DESC`. Emite listas **inteiras** (snapshots).
 *
 * **Backpressure:** usa `conflate()` — a UI recebe apenas o último valor.
 */
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

/**
 * Stream **global** de reviews (ex.: para leaderboards/timelines).
 * Define um `limit` alto (p.ex. 5000) para proteger custos.
 */
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
