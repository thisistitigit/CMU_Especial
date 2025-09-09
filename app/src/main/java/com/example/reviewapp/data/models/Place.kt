package com.example.reviewapp.data.models

/**
 * Modelo de domínio para exibição e lógica de UI.
 *
 * Este *model* agrega métricas internas (resultantes das reviews dos utilizadores da app)
 * e externas (Google), permitindo composições como leaderboards híbridos.
 *
 * @property id identificador único (normalmente o place_id).
 * @property name nome do local.
 * @property category categoria textual opcional exibida na UI.
 * @property phone contacto telefónico (opcional).
 * @property lat latitude (graus).
 * @property lng longitude (graus).
 * @property address morada (opcional).
 * @property avgRating média **externa** (Google) conhecida.
 * @property ratingsCount contagem de avaliações **externas**.
 * @property internalAvgRating média **interna** (comunidade da app).
 * @property internalRatingsCount contagem de avaliações **internas**.
 * @since 1.0
 */
data class Place(
    val id: String,
    val name: String,
    val category: String?,
    val phone: String?,
    val lat: Double,
    val lng: Double,
    val address: String?,
    val avgRating: Double = 0.0,
    val ratingsCount: Int = 0,
    val internalAvgRating: Double = 0.0,
    val internalRatingsCount: Int = 0,
)
