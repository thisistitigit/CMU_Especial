package com.example.cmu_especial.domain.repository

interface UserRepository {
    fun currentUserId(): String
    fun currentUserName(): String
}