package com.example.reviewapp.core.ext

fun Double?.orZero() = this ?: 0.0

fun <T> List<T>.averageBy(selector: (T) -> Double): Double =
    if (isEmpty()) 0.0 else sumOf(selector) / size