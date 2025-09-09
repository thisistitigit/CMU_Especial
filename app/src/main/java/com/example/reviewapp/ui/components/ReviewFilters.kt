package com.example.reviewapp.ui.components

import com.example.reviewapp.data.models.Review

/** Ordenação de reviews por data. */
enum class ReviewSort { OLDEST_FIRST, NEWEST_FIRST }

/** Estado serializável de filtros/ordenação de reviews. */
data class ReviewFilterState(
    val sort: ReviewSort = ReviewSort.OLDEST_FIRST,
    val withPhotoOnly: Boolean = false,
    val onlyMine: Boolean = false,
    val minStars: Int? = null
)

/**
 * Aplica filtros e ordenação a uma lista de reviews.
 *
 * @param all lista original.
 * @param f estado de filtros (foto, minhas, min estrelas, sort).
 * @param currentUserId UID atual para `onlyMine`.
 */
fun applyReviewFilters(
    all: List<Review>,
    f: ReviewFilterState,
    currentUserId: String?
): List<Review> {
    var seq = all.asSequence()
    if (f.withPhotoOnly) {
        seq = seq.filter { (it.photoCloudUrl?.isNotBlank() == true) || (it.photoLocalPath?.isNotBlank() == true) }
    }
    if (f.onlyMine && !currentUserId.isNullOrBlank()) {
        seq = seq.filter { it.userId == currentUserId }
    }
    f.minStars?.let { min -> seq = seq.filter { it.stars >= min } }

    seq = when (f.sort) {
        ReviewSort.OLDEST_FIRST -> seq.sortedBy { it.createdAt }
        ReviewSort.NEWEST_FIRST -> seq.sortedByDescending { it.createdAt }
    }
    return seq.toList()
}
