package com.example.reviewapp.network.dto

import android.util.Log
import com.example.reviewapp.data.models.Review
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.tasks.await

private const val TAG = "ReviewRepo"

fun FirebaseFirestore.reviewsCollection() =
    collection("reviews")

fun FirebaseFirestore.queryByField(
    field: String,
    value: Any,
    limit: Long = 50
): Query = reviewsCollection()
    .whereEqualTo(field, value)
    .orderBy("createdAt", Query.Direction.DESCENDING)
    .limit(limit)

fun QuerySnapshot.toReviews(): List<Review> =
    documents.mapNotNull { d ->
        d.toObject(ReviewRemoteDto::class.java)?.let { dto ->
            Review(
                id = dto.id,
                placeId = dto.placeId.trim(),
                placeName = dto.placeName.ifBlank { null },   // NOVO
                placeAddress = dto.placeAddress,               // NOVO
                userId = dto.userId,
                userName = dto.userName,
                pastryName = dto.pastryName,
                stars = dto.stars,
                comment = dto.comment,
                createdAt = dto.createdAt,
                photoLocalPath = null,
                photoCloudUrl = dto.photoCloudUrl
            )
        }
    }

suspend fun FirebaseFirestore.fetchByField(
    field: String,
    value: Any
): List<Review> = runCatching {
    queryByField(field, value).get().await().toReviews()
}.onFailure { e ->
    if (e is FirebaseFirestoreException &&
        e.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION
    ) {
        Log.e(TAG, "Falta índice composto para '$field' + orderBy('createdAt').")
    }
    Log.e(TAG, "Firestore error ao ler $field=$value", e)
}.getOrDefault(emptyList())

/** Paginador simples, genérico para reviews ordenadas por createdAt DESC. */
suspend fun FirebaseFirestore.fetchPaged(
    pageSize: Int,
    maxToFetch: Int
): List<Review> {
    val base = reviewsCollection()
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(pageSize.toLong())

    var fetched = 0
    var last: DocumentSnapshot? = null
    val acc = mutableListOf<Review>()

    while (fetched < maxToFetch) {
        val q = if (last == null) base else base.startAfter(last)
        val snap = runCatching { q.get().await() }
            .onFailure { e -> Log.e(TAG, "GET page falhou", e) }
            .getOrNull() ?: break

        if (snap.isEmpty) break

        val batch = snap.toReviews()
        if (batch.isEmpty()) break
        acc += batch
        fetched += batch.size
        last = snap.documents.last()
    }
    return acc
}
