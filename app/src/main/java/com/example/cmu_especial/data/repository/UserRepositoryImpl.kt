package com.example.cmu_especial.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.cmu_especial.domain.repository.UserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : UserRepository {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("user_repo", Context.MODE_PRIVATE)
    }

    private fun ensureUser(): Pair<String, String> {
        val id = prefs.getString("uid", null)
        val name = prefs.getString("name", null)
        if (id != null && name != null) return id to name
        val newId = UUID.randomUUID().toString()
        val newName = "Guest-${newId.take(6)}"
        prefs.edit().putString("uid", newId).putString("name", newName).apply()
        return newId to newName
    }

    override fun currentUserId(): String = ensureUser().first

    override fun currentUserName(): String = ensureUser().second
}
