package com.example.reviewapp.ui.components

import android.content.Context
import android.text.format.DateUtils
import com.example.reviewapp.data.models.Review

/**
 * Helpers centrados na apresentação de detalhes de reviews.
 */
object ReviewDetails {

    /** Escolhe a melhor imagem disponível (cloud → local). */
    fun bestPhoto(review: Review): String? =
        review.photoCloudUrl ?: review.photoLocalPath

    /** Data relativa (`"há 5 min"`, `"ontem"`, …) com abreviações. */
    fun relativeDate(context: Context, timeMillis: Long): String =
        DateUtils.getRelativeTimeSpanString(
            timeMillis,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()

    /** Média e contagem de estrelas numa coleção. */
    fun avgAndCount(reviews: List<Review>): Pair<Double, Int> {
        val c = reviews.size
        return if (c == 0) 0.0 to 0 else (reviews.sumOf { it.stars }.toDouble() / c) to c
    }
}
