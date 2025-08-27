package com.example.cmu_especial.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Regista uma visita do utilizador a um estabelecimento.
 * Usado para regras de anti-abuso (>=30 min entre reviews e <=50 m do local),
 * histórico e controlo de notificações de proximidade.
 */
@Entity(
    tableName = "user_visits",
    indices = [Index("userId"), Index("establishmentId"), Index("visitedAt")]
)
data class UserVisitEntity(
    @PrimaryKey val id: String,            // ex.: UUID
    val userId: String,
    val establishmentId: String,
    val lat: Double,
    val lon: Double,
    val visitedAt: Long                    // epoch ms
)
