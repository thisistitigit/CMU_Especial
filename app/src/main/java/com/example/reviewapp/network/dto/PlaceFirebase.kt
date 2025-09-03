package com.example.reviewapp.network.dto

import com.example.reviewapp.data.models.Place
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.tasks.await

// DTO com defaults (necessário para toObject/Firestore)
data class PlaceRemoteDto(
    var id: String = "",
    var name: String = "",
    var address: String? = null,
    var lat: Double? = null,
    var lng: Double? = null,
    var phone: String? = null,
    var category: String = ""
)

fun PlaceRemoteDto.toDomain(): Place = Place(
    id = id,
    name = name,
    address = address,
    lat = lat ?: 0.0,
    lng = lng ?: 0.0,
    phone = phone,
    category = category,
)

fun DocumentSnapshot.toPlaceOrNull(): Place? =
    this.toObject(PlaceRemoteDto::class.java)?.toDomain()

fun FirebaseFirestore.placesCollection() = collection("places")
fun FirebaseFirestore.placeDoc(id: String) = placesCollection().document(id)

// GET 1 documento
suspend fun FirebaseFirestore.fetchPlace(id: String): Place? =
    placeDoc(id).get().await().toPlaceOrNull()

// LISTEN 1 documento em tempo real
fun FirebaseFirestore.observePlace(id: String) = callbackFlow<Place?> {
    val reg = placeDoc(id).addSnapshotListener { snap, e ->
        if (e != null) { close(e); return@addSnapshotListener }
        trySend(snap?.toPlaceOrNull())
    }
    awaitClose { reg.remove() }
}.conflate()
