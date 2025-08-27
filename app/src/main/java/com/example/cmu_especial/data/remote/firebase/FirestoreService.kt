package com.example.cmu_especial.data.remote.firebase

import com.example.cmu_especial.domain.model.Review
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

//publico/privado
class FirestoreService(
    private val firestore: FirebaseFirestore
) {
    fun publicEstablishmentDoc(id: String) = firestore.collection("public_establishments").document(id)
    fun publicReviewsColl(id: String) = publicEstablishmentDoc(id).collection("reviews") // público (média/últimas 10).

    fun privateUserColl(uid: String) = firestore.collection("users").document(uid).collection("private")

    suspend fun upsertPublicReview(estId: String, review: Review) { /* ... agrega média ... */ }
}


