package com.example.reviewapp.data.locals

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidade Room para caching de locais (Places).
 *
 * **Chave:** [id] (tipicamente o `place_id` da Google).
 *
 * @property id identificador único.
 * @property name nome público do estabelecimento.
 * @property phone telefone (opcional).
 * @property lat latitude (graus).
 * @property lng longitude (graus).
 * @property address morada formatada (opcional).
 * @property avgRating média pública (Google) conhecida no momento do fetch.
 * @property ratingsCount contagem de avaliações públicas conhecida.
 * @property lastFetchedAt epoch millis do último _fetch_ (invalidação/sincronização).
 * @since 1.0
 */
@Entity(tableName = "places")
data class PlaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String?,
    val lat: Double,
    val lng: Double,
    val address: String?,
    val avgRating: Double,
    val ratingsCount: Int,
    val lastFetchedAt: Long
)
