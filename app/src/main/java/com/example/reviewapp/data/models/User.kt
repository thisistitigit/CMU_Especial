package com.example.reviewapp.data.models

data class UserProfile(
    val uid: String,
    val displayName: String,
    val lastReviewAt: Long? = null
)