package com.example.reviewapp.network.dto

import androidx.annotation.Keep
import com.google.firebase.database.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class ReviewRemoteDto(
    var id: String = "",
    var placeId: String = "",
    var placeName: String = "",
    var placeAddress: String? = null,
    var userId: String = "",
    var userName: String = "",
    var pastryName: String = "",
    var stars: Int = 0,
    var comment: String = "",
    var createdAt: Long = 0L,
    var photoCloudUrl: String? = null

)

