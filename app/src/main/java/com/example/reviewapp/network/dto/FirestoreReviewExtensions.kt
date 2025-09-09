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

/** Atalho para a coleção de reviews (`/reviews`). */
fun FirebaseFirestore.reviewsCollection() = collection("reviews")

/**
 * Constrói uma query por campo com ordenação por `createdAt DESC` e `limit`.
 *
 * **Nota:** pode exigir **índice composto** em Firestore (campo + orderBy).
 */
fun FirebaseFirestore.queryByField(
    field: String,
    value: Any,
    limit: Long = 50
): Query = reviewsCollection()
    .whereEqualTo(field, value)
    .orderBy("createdAt", Query.Direction.DESCENDING)
    .limit(limit)

/** Mapeia um `QuerySnapshot` para uma `List<Review>` defensiva. */
fun QuerySnapshot.toReviews(): List<Review> =
    documents.mapNotNull { d ->
        d.toObject(ReviewRemoteDto::class.java)?.let { dto ->
            Review(
                id = dto.id,
                placeId = dto.placeId.trim(),
                placeName = dto.placeName.ifBlank { null },
                placeAddress = dto.placeAddress,
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

/**
 * `GET` por campo (ex.: `"placeId"`, `"userId"`), com logging de erros e
 * aviso de índices compostos em falta.
 */
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

/**
 * Paginador simples por `createdAt DESC`.
 *
 * @param pageSize tamanho da página (batch).
 * @param maxToFetch limite superior absoluto para evitar custos elevados.
 * @return lista acumulada até `maxToFetch` ou fim de dados.
 */
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
