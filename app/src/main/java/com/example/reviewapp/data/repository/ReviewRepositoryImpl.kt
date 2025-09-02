package com.example.reviewapp.data.repository

import android.content.ContentValues.TAG
import android.net.Uri
import android.util.Log
import com.example.reviewapp.data.dao.PlaceDao
import com.example.reviewapp.data.dao.ReviewDao
import com.example.reviewapp.data.models.Place
import com.example.reviewapp.data.models.Review
import com.example.reviewapp.network.dto.ReviewRemoteDto
import com.example.reviewapp.network.dto.toMapNonNull
import com.example.reviewapp.network.mappers.toRemoteDto
import com.example.reviewapp.utils.ReviewRules
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
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
        val dto = review.toRemoteDto(cloudUrl)        // Review -> DTO
        val payload = dto.toMapNonNull()              // sem chaves nulas

        try {
            db.collection("reviews")
                .document(review.id)
                .set(payload, SetOptions.merge())
                .await()

            Log.d("ReviewRepo", "saveReviewDocSingle: OK (review=${review.id})")
        } catch (e: Exception) {
            Log.e("ReviewRepo", "saveReviewDocSingle: FAILED", e)
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

    // --- QUERY genérica por campo (placeId/userId) ---
    private fun queryReviewsByField(
        db: FirebaseFirestore,
        field: String,
        value: Any
    ): Query = db.collection("reviews")
        .whereEqualTo(field, value)
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(50)

    // --- Map: DTO -> Domain ---
    private fun ReviewRemoteDto.toDomain() =
      Review(
            id = id,
            placeId = placeId,
            userId = userId,
            userName = userName,
            pastryName = pastryName,
            stars = stars,
            comment = comment,
            createdAt = createdAt,
            photoLocalPath = null,
            photoCloudUrl = photoCloudUrl
        )

    private fun QuerySnapshot.toDomainReviews(): List<Review> =
        documents.mapNotNull { doc ->
            doc.toObject(ReviewRemoteDto::class.java)
                ?.toDomain()
        }

    // --- FETCH genérico do Firestore ---
    private suspend fun fetchReviewsByFieldFromCloud(
        db: FirebaseFirestore,
        field: String,
        value: Any
    ): List<Review> {
        try {
            val q = queryReviewsByField(db, field, value)
            val snap = q.get().await()
            Log.d("ReviewRepo", "fetchReviewsByField: got ${snap.size()} docs for $field=$value")

            val list = snap.toDomainReviews()
            if (list.isEmpty()) {
                Log.w("ReviewRepo", "fetchReviewsByField: empty for $field=$value (ok if truly no data)")
            }
            return list
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                Log.e(
                    "ReviewRepo",
                    "ÍNDICE EM FALTA para '$field' + orderBy('createdAt'). " +
                            "Cria um índice composto em 'reviews': [$field ASC, createdAt DESC]."
                )
            } else {
                Log.e("ReviewRepo", "Firestore error ($field=$value): ${e.code}", e)
            }
            throw e
        } catch (e: Exception) {
            Log.e("ReviewRepo", "fetchReviewsByField: generic error ($field=$value)", e)
            throw e
        }
    }
    // --- Wrappers específicos (usam o genérico) ---
    override suspend fun refreshPlaceReviews(placeId: String): List<Review> {
        val db = firestore ?: return reviewDao.allForPlace(placeId).map { it.toModel() }
        val remote = fetchReviewsByFieldFromCloud(db, "placeId", placeId)
        if (remote.isNotEmpty()) reviewDao.upsertAll(remote.map { it.toEntity() })
        return remote
    }

    override suspend fun refreshUserReviews(uid: String): List<Review> {
        val db = firestore ?: return reviewDao.history(uid).map { it.toModel() }
        val remote = fetchReviewsByFieldFromCloud(db, "userId", uid)
        if (remote.isNotEmpty()) reviewDao.upsertAll(remote.map { it.toEntity() })
        return remote
    }
    override suspend fun refreshAllReviews(maxToFetch: Int, pageSize: Int): Int {
        val db = firestore ?: return 0
        var fetched = 0
        var page = 0
        var last: DocumentSnapshot? = null

        val base = db.collection("reviews")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(pageSize.toLong())

        while (fetched < maxToFetch) {
            val q = if (last == null) base else base.startAfter(last!!)
            val snap = try {
                q.get().await()
            } catch (e: Exception) {
                Log.e("ReviewRepo", "refreshAllReviews: Firestore GET falhou (page=$page)", e)
                break
            }

            if (snap.isEmpty) {
                Log.d("ReviewRepo", "refreshAllReviews: page=$page vazio, termina")
                break
            }

            val batch = snap.documents.mapNotNull { d ->
                d.toObject(ReviewRemoteDto::class.java)?.let { dto ->
                    Review(
                        id = dto.id,
                        placeId = dto.placeId,
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

            Log.d("ReviewRepo", "refreshAllReviews: page=$page docs=${batch.size}")
            if (batch.isNotEmpty()) reviewDao.upsertAll(batch.map { it.toEntity() })
            fetched += batch.size
            last = snap.documents.last()
            page++
        }

        Log.d("ReviewRepo", "refreshAllReviews: total fetched=$fetched")
        return fetched
    }
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
