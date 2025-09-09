package com.example.reviewapp.utils

import java.time.LocalTime

/**
 * Utilitários temporais simples.
 *
 * Inclui *janela promocional* para notificações (entre 16:00 e 18:00, hora local).
 */
object TimeUtils {
    /** Epoch millis corrente. */
    fun now() = System.currentTimeMillis()

    private val start = LocalTime.of(16, 0)
    private val end   = LocalTime.of(18, 0)

    /**
     * @return `true` se a hora atual (ou fornecida) está dentro de [start,end].
     * Limita o horário em que são enviadas as notificações de perto de local.
     */
    fun isWithinPromoWindow(now: LocalTime = LocalTime.now()): Boolean =
        !now.isBefore(start) && !now.isAfter(end)
}
