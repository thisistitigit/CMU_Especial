package com.example.reviewapp.data.repository

import android.content.ContentValues.TAG
import android.content.Context
import androidx.core.net.toUri
import com.example.reviewapp.core.ext.requireSignedInUid
import com.example.reviewapp.data.dao.PlaceDao
import com.example.reviewapp.data.dao.ReviewDao
import com.example.reviewapp.data.models.Place
import com.example.reviewapp.data.models.Review
import com.example.reviewapp.di.ReviewPhotoSyncWorker
import com.example.reviewapp.network.dto.ReviewRemoteDto
import com.example.reviewapp.network.dto.fetchByField
import com.example.reviewapp.network.dto.fetchPaged
import com.example.reviewapp.network.dto.observeAll
import com.example.reviewapp.network.dto.observeByField
import com.example.reviewapp.network.dto.reviewsCollection
import com.example.reviewapp.network.mappers.toEntity
import com.example.reviewapp.network.mappers.toModel
import com.example.reviewapp.utils.ReviewRules
import com.example.reviewapp.utils.ReviewRules.distanceMeters
import com.example.reviewapp.utils.runCatchingLogAsync
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.tasks.await

class ReviewRepositoryImpl(
    private val reviewDao: ReviewDao,
    private val placeDao: PlaceDao,
    private val firestore: FirebaseFirestore? = null,
    private val storage: FirebaseStorage? = null,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    @ApplicationContext private val appContext: Context
) : ReviewRepository {

    override fun currentUid(): String? = auth.currentUser?.uid

    override suspend fun lastReviewAtByUser(userId: String): Long? =
        reviewDao.lastCreatedAtByUser(userId)

    override suspend fun addReview(
        review: Review,
        userLat: Double,
        userLng: Double,
        now: Long
    ) {
        val uid = auth.requireSignedInUid()

        // Regras antes de gravar
        enforceCanReviewOrThrow(uid, review.placeId, userLat, userLng, now)

        // Local (sempre)
        val normalized = review.copy(userId = uid, createdAt = now)
        reviewDao.upsert(normalized.toEntity())

        // Firestore: doc sem foto (cache offline trata do resto)
        firestore?.let { db -> runCatching { saveReviewDocs(db, normalized, null) } }

        // Foto (tenta já; senão agenda)
        val st = storage
        if (st != null && normalized.photoLocalPath != null) {
            val cloudUrl = runCatching { uploadPhotoIfNeeded(st, normalized) }.getOrNull()
            if (cloudUrl != null) {
                firestore?.collection("reviews")?.document(normalized.id)
                    ?.update(mapOf("photoCloudUrl" to cloudUrl))
                reviewDao.updateCloudUrl(normalized.id, cloudUrl)
            } else {
                ReviewPhotoSyncWorker.enqueue(appContext, normalized.id)
            }
        }
    }

    class ReviewDeniedException(val reason: Reason) : IllegalStateException(reason.name) {
        enum class Reason { TOO_FAR, TOO_SOON }
    }

    suspend fun enforceCanReviewOrThrow(
        userId: String, placeId: String, userLat: Double, userLng: Double, now: Long
    ) {
        val place = placeDao.get(placeId)
            ?: throw IllegalStateException("Place $placeId não encontrado")

        val last = reviewDao.lastCreatedAtByUser(userId)
        val dist = distanceMeters(userLat, userLng, place.lat, place.lng)
        val ok = ReviewRules.canReview(dist, last, now)
        if (!ok) {
            val tooFar = dist > ReviewRules.MIN_DISTANCE_METERS
            throw ReviewDeniedException(
                if (tooFar) ReviewDeniedException.Reason.TOO_FAR
                else ReviewDeniedException.Reason.TOO_SOON
            )
        }
    }

    private suspend fun uploadPhotoIfNeeded(storage: FirebaseStorage, review: Review): String? {
        if (review.photoCloudUrl != null) return review.photoCloudUrl
        val localPath = review.photoLocalPath ?: return null

        val ref = storage.reference.child("images/reviews/${review.placeId}/${review.id}.jpg")
        val uri = localPath.toUri()
        val meta = com.google.firebase.storage.StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .build()

        return runCatchingLogAsync(TAG, "Upload falhou") {
            ref.putFile(uri, meta).await()
            ref.downloadUrl.await().toString()
        }.getOrNull()
    }

    private suspend fun saveReviewDocs(
        db: FirebaseFirestore,
        review: Review,
        cloudUrl: String?
    ) {
        val pe = placeDao.get(review.placeId)
        val name = review.placeName?.takeIf { it.isNotBlank() } ?: pe?.name ?: ""
        val address = review.placeAddress ?: pe?.address
        val photo = cloudUrl ?: review.photoCloudUrl

        val payload = buildMap<String, Any> {
            put("id", review.id)
            put("placeId", review.placeId)
            put("placeName", name)
            address?.let { put("placeAddress", it) }
            put("userId", review.userId)
            put("userName", review.userName)
            put("pastryName", review.pastryName)
            put("stars", review.stars)
            put("comment", review.comment)
            put("createdAt", review.createdAt)
            photo?.let { put("photoCloudUrl", it) }
        }

        db.reviewsCollection().document(review.id).set(payload, SetOptions.merge()).await()
    }

    override suspend fun latestReviews(placeId: String) =
        reviewDao.latestForPlace(placeId).map { it.toModel() }

    override suspend fun history(userId: String) =
        reviewDao.history(userId).map { it.toModel() }

    override suspend fun canUserReviewHere(
        userId: String, place: Place, now: Long, userLat: Double, userLng: Double
    ): Boolean {
        val last = reviewDao.lastCreatedAtByUser(userId)
        val dist = distanceMeters(userLat, userLng, place.lat, place.lng)
        return ReviewRules.canReview(dist, last, now)
    }

    override suspend fun getReview(id: String) =
        reviewDao.getById(id)?.toModel()

    override suspend fun allReviews(placeId: String) =
        reviewDao.allForPlace(placeId).map { it.toModel() }

    override suspend fun refreshPlaceReviews(placeId: String): List<Review> {
        val db = firestore ?: return reviewDao.allForPlace(placeId).map { it.toModel() }
        val remote = db.fetchByField("placeId", placeId)
        if (remote.isNotEmpty()) reviewDao.upsertAll(remote.map { it.toEntity() })
        return remote
    }

    override suspend fun refreshUserReviews(uid: String): List<Review> {
        val db = firestore ?: return reviewDao.history(uid).map { it.toModel() }
        val remote = db.fetchByField("userId", uid)
        if (remote.isNotEmpty()) reviewDao.upsertAll(remote.map { it.toEntity() })
        return remote
    }

    override suspend fun refreshAllReviews(maxToFetch: Int, pageSize: Int): Int {
        val db = firestore ?: return 0
        val all = db.fetchPaged(pageSize, maxToFetch)
        if (all.isNotEmpty()) reviewDao.upsertAll(all.map { it.toEntity() })
        return all.size
    }

    override fun streamPlaceReviews(placeId: String): Flow<List<Review>> = channelFlow {
        val remoteJob = firestore?.observeByField("placeId", placeId, limit = 200)
            ?.onEach { reviewDao.upsertAll(it.map { dto -> dto.toEntity() }) }
            ?.launchIn(this)

        reviewDao.flowForPlace(placeId)
            .map { list -> list.map { it.toModel() } }
            .collect { send(it) }

        remoteJob?.cancel()
    }

    override fun streamAllReviews(): Flow<List<Review>> = channelFlow {
        val remoteJob = firestore?.observeAll(limit = 5000)
            ?.onEach { reviewDao.upsertAll(it.map { dto -> dto.toEntity() }) }
            ?.launchIn(this)

        reviewDao.flowAll()
            .map { list -> list.map { it.toModel() } }
            .collect { send(it) }

        remoteJob?.cancel()
    }

    override fun streamUserHistory(uid: String): Flow<List<Review>> = channelFlow {
        val remoteJob = firestore?.observeByField("userId", uid, limit = 1000)
            ?.onEach { reviewDao.upsertAll(it.map { dto -> dto.toEntity() }) }
            ?.launchIn(this)

        reviewDao.flowHistoryForUser(uid)
            .map { list -> list.map { it.toModel() } }
            .collect { send(it) }

        remoteJob?.cancel()
    }

    override fun streamPlaceMetaFromReviews(placeId: String): Flow<Place?> = callbackFlow {
        val db = firestore ?: run {
            close(IllegalStateException("Firestore indisponível"))
            return@callbackFlow
        }

        val reg = db.reviewsCollection()
            .whereEqualTo("placeId", placeId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snap, e ->
                if (e != null) { close(e); return@addSnapshotListener }
                val dto = snap?.documents?.firstOrNull()?.toObject(ReviewRemoteDto::class.java)
                val place = dto?.let {
                    Place(
                        id = it.placeId, name = it.placeName, address = it.placeAddress,
                        lat = 0.0, lng = 0.0, phone = null, category = null,
                        avgRating = 0.0, ratingsCount = 0
                    )
                }
                trySend(place)
            }

        awaitClose { reg.remove() }
    }.conflate()
}
