package com.example.reviewapp.data.locals

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidade Room para reviews submetidas pelos utilizadores.
 *
 * Índices em [placeId] e [userId] suportam:
 * - *feeds* por estabelecimento,
 * - históricos por utilizador,
 * - políticas de *rate limiting* (globais e por local).
 *
 * @property id identificador único da review.
 * @property placeId id do estabelecimento.
 * @property placeName nome do estabelecimento (snapshot para estabilidade histórica).
 * @property placeAddress morada do estabelecimento (snapshot).
 * @property userId id do autor da review.
 * @property userName nome do autor (snapshot — pode divergir do perfil futuro).
 * @property pastryName nome da doçaria avaliada.
 * @property stars rating 1..5.
 * @property comment comentário textual obrigatório.
 * @property photoLocalPath caminho local da foto (para upload posterior).
 * @property photoCloudUrl URL público na cloud após upload.
 * @property createdAt epoch millis de criação.
 * @since 1.0
 */
@Entity(
    tableName = "reviews",
    indices = [Index("placeId"), Index("userId")]
)
data class ReviewEntity(
    @PrimaryKey val id: String,
    val placeId: String,
    val placeName: String?,
    val placeAddress: String?,
    val userId: String,
    val userName: String,
    val pastryName: String,
    val stars: Int,
    val comment: String,
    val photoLocalPath: String?,
    val photoCloudUrl: String?,
    val createdAt: Long
)
