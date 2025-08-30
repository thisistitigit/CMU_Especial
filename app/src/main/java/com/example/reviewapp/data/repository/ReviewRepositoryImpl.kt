package com.example.reviewapp.data.repository

import com.example.reviewapp.data.dao.PlaceDao
import com.example.reviewapp.data.dao.ReviewDao
import com.example.reviewapp.data.models.Place
import com.example.reviewapp.data.models.Review
import com.example.reviewapp.network.api.GooglePlacesApi
import com.example.reviewapp.utils.ReviewRules
// Firebase opcional (sincronização e fotos)
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class ReviewRepositoryImpl(
    private val reviewDao: ReviewDao,
    private val placeDao: PlaceDao,

    private val firestore: FirebaseFirestore? = null,
    private val storage: FirebaseStorage? = null
) : ReviewRepository {

    override suspend fun addReview(review: Review) {
        // 1) Guarda local (Room)
        reviewDao.upsert(review.toEntity())

        // 2) Atualiza agregados locais do Place (avg + count)
        val place = placeDao.get(review.placeId)
        if (place != null) {
            val newCount = place.ratingsCount + 1
            val newAvg = ((place.avgRating * place.ratingsCount) + review.stars) / newCount.toDouble()
            placeDao.upsert(
                place.copy(
                    avgRating = newAvg,
                    ratingsCount = newCount
                )
            )
        }

        // 3) (Opcional) Sobe a foto para Storage e sincroniza Firestore
        firestore?.let { db ->
            val publicRef = db.collection("places").document(review.placeId)
                .collection("reviews").document(review.id)
            val userRef = db.collection("users").document(review.userId)
                .collection("reviews").document(review.id)

            var cloudUrl: String? = review.photoCloudUrl
            if (cloudUrl == null && review.photoLocalPath != null && storage != null) {
                val fileRef = storage.reference
                    .child("images/reviews/${review.placeId}/${review.id}.jpg")
                // Upload simples (sem await para não bloquear esta função)
                val uri = android.net.Uri.parse(review.photoLocalPath)
                fileRef.putFile(uri).addOnSuccessListener {
                    fileRef.downloadUrl.addOnSuccessListener { u ->
                        publicRef.update("photoCloudUrl", u.toString())
                    }
                }
            }

            val payload = hashMapOf(
                "id" to review.id,
                "placeId" to review.placeId,
                "userId" to review.userId,
                "userName" to review.userName,
                "pastryName" to review.pastryName,
                "stars" to review.stars,
                "comment" to review.comment,
                "photoCloudUrl" to cloudUrl,
                "createdAt" to review.createdAt
            )
            publicRef.set(payload)
            userRef.set(payload)
        }
    }

    override suspend fun latestReviews(placeId: String): List<Review> =
        reviewDao.latestForPlace(placeId).map { it.toModel() }

    override suspend fun history(userId: String): List<Review> =
        reviewDao.history(userId).map { it.toModel() }

    override suspend fun canUserReviewHere(userId: String, place: Place, now: Long): Boolean {
        // Regra temporal (30 min) baseada no último review do utilizador
        val last = reviewDao.history(userId).firstOrNull()?.createdAt
        // Regra de distância (≤ 50 m) deve ser verificada fora (ViewModel) onde tens a localização atual
        // Ex.: val ok = ReviewRules.canReview(distanceMeters, last, now)
        return ReviewRules.canReview(
            distanceMeters = 0.0,     // TODO: passar distância real medida no ViewModel
            lastReviewAt = last,
            now = now
        )
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
