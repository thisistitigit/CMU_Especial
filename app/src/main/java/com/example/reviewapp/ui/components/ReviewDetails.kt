package com.example.reviewapp.ui.components

import android.content.Context
import android.text.format.DateUtils
import com.example.reviewapp.data.models.Review

object  ReviewDetails {

    /** Foto preferida: dá prioridade ao link público; senão usa o URI local. */
    fun bestPhoto(review: Review): String? =
        review.photoCloudUrl ?: review.photoLocalPath

    /** "há 5 min", "ontem", etc. */
    fun relativeDate(context: Context, timeMillis: Long): String =
        DateUtils.getRelativeTimeSpanString(
            timeMillis,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()

    /** média e contagem de estrelas (útil para cabeçalhos/leaderboards). */
    fun avgAndCount(reviews: List<Review>): Pair<Double, Int> {
        val c = reviews.size
        return if (c == 0) 0.0 to 0 else (reviews.sumOf { it.stars }.toDouble() / c) to c
    }
}