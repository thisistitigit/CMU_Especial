package com.example.reviewapp.data.repository

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import android.content.ContentValues.TAG
import com.example.reviewapp.data.dao.ReviewDao
import com.example.reviewapp.data.models.Place
import com.example.reviewapp.data.models.Review
import com.example.reviewapp.network.dto.fetchByField
import com.example.reviewapp.network.dto.fetchPaged
import com.example.reviewapp.network.dto.observeAll
import com.example.reviewapp.network.dto.observeByField
import com.example.reviewapp.network.dto.reviewsCollection
import com.example.reviewapp.network.dto.toMapNonNull
import com.example.reviewapp.network.mappers.toRemoteDto
import com.example.reviewapp.utils.runCatchingLogAsync
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.core.net.toUri
import com.example.reviewapp.core.ext.requireSignedInUid
import com.example.reviewapp.data.dao.PlaceDao
import com.example.reviewapp.network.dto.ReviewRemoteDto
import com.example.reviewapp.network.mappers.toEntity
import com.example.reviewapp.network.mappers.toModel
import com.google.firebase.firestore.Query

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
        // 1) exige sessão iniciada
        val uid = auth.requireSignedInUid()
        val normalized = review.copy(userId = uid)

        // 1) Grava local (sempre)
        reviewDao.upsert(normalized.toEntity())

        // 2) Cloud (opcional)
        val db = firestore ?: return
        val st = storage

        val cloudUrl = if (st != null) uploadPhotoIfNeeded(st, normalized) else null

        runCatchingLogAsync(TAG, "Firestore write failed") {
            saveReviewDocs(db, normalized, cloudUrl)
        }.onSuccess {
            if (cloudUrl != null) {
                // atualização de url em background
                CoroutineScope(Dispatchers.IO).launch {
                    reviewDao.updateCloudUrl(normalized.id, cloudUrl)
                }
            }
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
        // tenta obter meta do local do Room (já foi cacheado noutros ecrãs)
        val pe = placeDao.get(review.placeId)
        val dto = review.toRemoteDto(
            photoUrl = cloudUrl,
            placeName = pe?.name,
            placeAddress = pe?.address
        )
        val payload = dto.toMapNonNull()

        db.reviewsCollection()
            .document(review.id)
            .set(payload, SetOptions.merge())
            .await()
    }


    override suspend fun latestReviews(placeId: String): List<Review> =
        reviewDao.latestForPlace(placeId).map { it.toModel() }

    override suspend fun history(userId: String): List<Review> =
        reviewDao.history(userId).map { it.toModel() }

    override suspend fun canUserReviewHere(userId: String, place: Place, now: Long): Boolean {
        val last = reviewDao.history(userId).firstOrNull()?.createdAt
        return com.example.reviewapp.utils.ReviewRules.canReview(
            distanceMeters = 0.0, // regra de distância tratada fora
            lastReviewAt = last,
            now = now
        )
    }

    override suspend fun getReview(id: String): Review? =
        reviewDao.getById(id)?.toModel()

    override suspend fun allReviews(placeId: String): List<Review> =
        reviewDao.allForPlace(placeId).map { it.toModel() }


    override suspend fun refreshPlaceReviews(placeId: String): List<Review> {
        val db = firestore ?: return reviewDao.allForPlace(placeId).map { it.toModel() }
        val remote = db.fetchByField("placeId", placeId)   // <- extensão nova
        if (remote.isNotEmpty()) reviewDao.upsertAll(remote.map { it.toEntity() })
        return remote
    }

    override suspend fun refreshUserReviews(uid: String): List<Review> {
        val db = firestore ?: return reviewDao.history(uid).map { it.toModel() }
        val remote = db.fetchByField("userId", uid)        // <- extensão nova
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
        // 1) Coleta remota e mantém Room sincronizado (só se houver Firestore)
        val remoteJob = firestore?.observeByField("placeId", placeId, limit = 200)
            ?.onEach { remote ->
                reviewDao.upsertAll(remote.map { it.toEntity() })
            }
            ?.launchIn(this) // <- começa a escutar em tempo-real neste scope

        // 2) Emite sempre o local (Room) para a UI
        reviewDao.flowForPlace(placeId)
            .map { list -> list.map { it.toModel() } }
            .collect { send(it) }

        remoteJob?.cancel()
    }

    override fun streamAllReviews(): Flow<List<Review>> = channelFlow {
        val remoteJob = firestore?.observeAll(limit = 5000)
            ?.onEach { remote -> reviewDao.upsertAll(remote.map { it.toEntity() }) }
            ?.launchIn(this)

        reviewDao.flowAll()
            .map { list -> list.map { it.toModel() } }
            .collect { send(it) }

        remoteJob?.cancel()
    }
    override fun streamUserHistory(uid: String): Flow<List<Review>> = channelFlow {
        // 1) se houver Firestore, escuta em tempo real e cacheia no Room
        val remoteJob = firestore?.observeByField("userId", uid, limit = 1000)
            ?.onEach { remote -> reviewDao.upsertAll(remote.map { it.toEntity() }) }
            ?.launchIn(this)

        // 2) emite SEMPRE o local (offline-first)
        reviewDao.flowHistoryForUser(uid)
            .map { list -> list.map { it.toModel() } }
            .collect { send(it) }

        remoteJob?.cancel()
    }

    override fun streamPlaceMetaFromReviews(placeId: String): Flow<Place?> = callbackFlow {
        val db = firestore
        if (db == null) {
            // fecha o canal com erro do próprio callbackFlow (SEM Os.close!)
            close(IllegalStateException("Firestore indisponível"))
            return@callbackFlow
        }

        val reg = db.reviewsCollection()
            .whereEqualTo("placeId", placeId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    // fecha o canal corretamente
                    close(e)
                    return@addSnapshotListener
                }

                val dto = snap?.documents?.firstOrNull()
                    ?.toObject(ReviewRemoteDto::class.java)

                val place = dto?.let {
                    Place(
                        id = it.placeId,
                        name = if (it.placeName.isBlank()) "Estabelecimento" else it.placeName,
                        address = it.placeAddress,
                        lat = 0.0, lng = 0.0,
                        phone = null,
                        category = null,
                        avgRating = 0.0,
                        ratingsCount = 0
                    )
                }

                // envia para o flow (ignora se o collector estiver lento/cancelado)
                trySend(place)
            }

        awaitClose { reg.remove() }
    }.conflate()
}

