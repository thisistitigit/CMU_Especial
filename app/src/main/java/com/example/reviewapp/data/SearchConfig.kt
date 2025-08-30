package com.example.reviewapp.data

import com.example.reviewapp.data.enums.PlaceType

data class SearchConfig(
    val radiusMeters: Int,
    val types: Set<PlaceType>,
    val keyword: String? = null
)