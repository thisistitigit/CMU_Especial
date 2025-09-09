package com.example.reviewapp.data.models

import java.util.UUID

/**
 * Modelo de domínio para uma review submetida por um utilizador.
 *
 * Este modelo é usado na camada de **domínio/UI** e pode ser mapeado para
 * entidades Room e DTOs remotos. Mantém _snapshots_ opcionais do nome e
 * morada do estabelecimento para robustez histórica.
 *
 * @property id identificador único (UUID por omissão).
 * @property placeId identificador do estabelecimento (tipicamente `place_id`).
 * @property userId UID do autor (Firebase Auth).
 * @property userName nome do autor na altura da submissão.
 * @property pastryName nome da doçaria avaliada.
 * @property stars rating 1..5.
 * @property comment comentário textual.
 * @property photoLocalPath caminho local da foto (para upload assíncrono).
 * @property photoCloudUrl URL público após upload (se já sincronizado).
 * @property createdAt epoch millis (UTC) do momento de criação.
 * @property placeName nome do estabelecimento no momento (snapshot).
 * @property placeAddress morada do estabelecimento no momento (snapshot).
 * @since 1.0
 */
data class Review(
    val id: String = UUID.randomUUID().toString(),
    val placeId: String,
    val userId: String,
    val userName: String,
    val pastryName: String,
    val stars: Int,
    val comment: String,
    val photoLocalPath: String?,
    val photoCloudUrl: String?,
    val createdAt: Long,
    val placeName: String? = null,
    val placeAddress: String? = null,
)