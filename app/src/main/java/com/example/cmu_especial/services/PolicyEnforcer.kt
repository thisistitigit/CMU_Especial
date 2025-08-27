package com.example.cmu_especial.services

import com.example.cmu_especial.domain.usecase.CanUserReviewNow
import jakarta.inject.Inject

// anti-abuso

class PolicyEnforcer @Inject constructor(private val canReviewNow: CanUserReviewNow) {
    suspend fun assertCanReview(/* … */): Boolean = canReviewNow(/* … */)
}