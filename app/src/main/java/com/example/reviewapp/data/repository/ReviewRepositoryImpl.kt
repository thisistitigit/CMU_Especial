package com.example.reviewapp.data.repository

import android.content.ContentValues.TAG
import android.net.Uri
import android.util.Log
import com.example.reviewapp.data.dao.PlaceDao
import com.example.reviewapp.data.dao.ReviewDao
import com.example.reviewapp.data.models.Place
import com.example.reviewapp.data.models.Review
import com.example.reviewapp.data.remote.dto.toMapNonNull
import com.example.reviewapp.network.mappers.toRemoteDto
import com.example.reviewapp.utils.ReviewRules
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ReviewRepositoryImpl(
    private val reviewDao: ReviewDao,
    private val placeDao: PlaceDao,
    private val firestore: FirebaseFirestore? = null,
    private val storage: FirebaseStorage? = null,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),

) : ReviewRepository {

    override fun currentUid(): String? = auth.currentUser?.uid

    override suspend fun lastReviewAtByUser(userId: String): Long? =
        reviewDao.lastCreatedAtByUser(userId)

    override suspend fun addReview(review: Review) {
        ensureSignedInIfNeeded()
        val uid = currentUid() ?: review.userId // fallback
        Log.d(TAG, "UID=${auth.currentUser?.uid}")

        // força consistência do user na review guardada
        val normalized = review.copy(userId = uid)

        // 1) local
        reviewDao.upsert(normalized.toEntity())

        // 2) cloud (opcional)
        val db = firestore
        val st = storage
        if (db == null || st == null) return

        val cloudUrl = uploadPhotoIfNeeded(st, normalized)
        try {
            saveReviewDocs(db, normalized, cloudUrl)   // <- agora é suspend e lança se falhar
        } catch (e: Exception) {
            Log.e(TAG, "Firestore write failed", e)
            // opcional: rethrow ou mostrar snackbar/estado de erro
        }
        if (cloudUrl != null) {
            CoroutineScope(Dispatchers.IO).launch {
                reviewDao.updateCloudUrl(normalized.id, cloudUrl)
            }
        }
    }
        /** Garante que existe um user autenticado; caso não exista, faz sign-in anónimo. */
        private suspend fun ensureSignedInIfNeeded() {
            if (auth.currentUser != null) return
            try {
                auth.signInAnonymously().await()
                Log.d(TAG, "Auth anónima OK: uid=${auth.currentUser?.uid}")
            } catch (e: Exception) {
                Log.e(TAG, "Auth anónima falhou", e)
                // Podes lançar se as tuas regras exigirem auth obrigatória
                throw e
            }
        }

        /**
         * Faz upload da foto para `images/reviews/{placeId}/{reviewId}.jpg` se:
         *  - existir `photoLocalPath`, e
         *  - ainda não houver `photoCloudUrl`.
         * Devolve a URL pública (downloadUrl) ou null se nada foi feito.
         */
        private suspend fun uploadPhotoIfNeeded(storage: FirebaseStorage, review: Review): String? {
            if (review.photoCloudUrl != null) {
                Log.d(TAG, "uploadPhotoIfNeeded: já tem cloudUrl → skip")
                return review.photoCloudUrl
            }
            val localPath = review.photoLocalPath ?: run {
                Log.d(TAG, "uploadPhotoIfNeeded: sem photoLocalPath → skip")
                return null
            }

            val fileRef = storage.reference.child("images/reviews/${review.placeId}/${review.id}.jpg")
            val localUri = Uri.parse(localPath)
            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build()

            return try {
                Log.d(TAG, "uploadPhotoIfNeeded: uploading to ${fileRef.path} from=$localUri")
                fileRef.putFile(localUri, metadata).await()
                val url = fileRef.downloadUrl.await().toString()
                Log.d(TAG, "uploadPhotoIfNeeded: downloadUrl=$url")
                url
            } catch (e: Exception) {
                Log.e(TAG, "uploadPhotoIfNeeded: upload failed", e)
                null // não bloqueia o resto; Firestore será gravado sem a foto
            }
        }

    private suspend fun saveReviewDocs(
        db: FirebaseFirestore,
        review: Review,
        cloudUrl: String?
    ) {
        val publicRef = db.collection("places")
            .document(review.placeId)
            .collection("reviews")
            .document(review.id)

        val userRef = db.collection("users")
            .document(review.userId)
            .collection("reviews")
            .document(review.id)

        // 🔁 Review -> DTO -> Map (sem nulos)
        val dto = review.toRemoteDto(cloudUrl)
        val payload = dto.toMapNonNull()

        val batch = db.batch()
        batch.set(publicRef, payload, SetOptions.merge())
        batch.set(userRef,    payload, SetOptions.merge())

        try {
            batch.commit().await()
            Log.d("ReviewRepo", "saveReviewDocs: batch OK (place=${review.placeId}, review=${review.id})")
        } catch (e: Exception) {
            Log.e("ReviewRepo", "saveReviewDocs: batch FAILED", e)
            throw e
        }
    }
        override suspend fun latestReviews(placeId: String): List<Review> =
        reviewDao.latestForPlace(placeId).map { it.toModel() }

    override suspend fun history(userId: String): List<Review> =
        reviewDao.history(userId).map { it.toModel() }

    override suspend fun canUserReviewHere(userId: String, place: Place, now: Long): Boolean {
        val last = reviewDao.history(userId).firstOrNull()?.createdAt
        // Regra de distância (≤ 50 m) deve ser verificada fora (ViewModel) onde tens a localização atual
        // Ex.: val ok = ReviewRules.canReview(distanceMeters, last, now)
        return ReviewRules.canReview(
            distanceMeters = 0.0,
            lastReviewAt = last,
            now = now
        )
    }
    override suspend fun getReview(id: String): Review? =
        reviewDao.getById(id)?.toModel()

    override suspend fun allReviews(placeId: String): List<Review> =
        reviewDao.allForPlace(placeId).map { it.toModel() }

}

// ---- Mapeadores ----
private fun Review.toEntity() = com.example.reviewapp.data.locals.ReviewEntity(
    id = id,
    placeId = placeId,
    userId = userId,
    userName = userName,
    pastryName = pastryName,
    stars = stars,
    comment = comment,
    photoLocalPath = photoLocalPath,
    photoCloudUrl = photoCloudUrl,
    createdAt = createdAt
)

private fun com.example.reviewapp.data.locals.ReviewEntity.toModel() = Review(
    id = id,
    placeId = placeId,
    userId = userId,
    userName = userName,
    pastryName = pastryName,
    stars = stars,
    comment = comment,
    photoLocalPath = photoLocalPath,
    photoCloudUrl = photoCloudUrl,
    createdAt = createdAt
)
