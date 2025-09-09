package com.example.reviewapp.services

import android.content.Context
import android.util.Log
import com.example.reviewapp.utils.NotificationUtils
import com.example.reviewapp.utils.PermissionUtils
import com.example.reviewapp.utils.TimeUtils

/**
 * Emite uma notificação contextual do tipo **"Estás perto: <placeName>"**,
 * **restrita** à janela horária **16:00–18:00** (hora local).
 *
 * Fluxo:
 * 1. **Janela temporal** — validação via [TimeUtils.isWithinPromoWindow]; fora do intervalo
 *    a notificação é suprimida (retorna `false` e regista _log_ a nível `DEBUG`).
 * 2. Garantia do **canal** através de [NotificationUtils.ensureChannel].
 * 3. Verificação de **permissões** (Android 13+) com [PermissionUtils.hasNotificationPermission].
 * 4. Construção do objeto [android.app.Notification] com _tile_ temático.
 * 5. Apresentação via [NotificationUtils.showNotification].
 *
 * Comportamento:
 * - **Fora da janela (16–18):** não notifica e devolve `false`.
 * - **Sem permissão:** não notifica, regista `WARN` e devolve `false`.
 * - **Sucesso de entrega:** devolve `true`.
 *
 * @param context contexto da aplicação/atividade.
 * @param placeName nome do estabelecimento a destacar (pode ser `null`, caso em que é usada
 *                  uma mensagem genérica definida nos recursos).
 * @return `true` se a notificação foi enviada; `false` se suprimida ou em erro/permissão.
 */
fun notifyNearby(context: Context, placeName: String?): Boolean {
    // 0) Restringir por janela temporal (16:00–18:00)
    if (!TimeUtils.isWithinPromoWindow()) {
        Log.d("Geofence", "Fora da janela 16–18h — notificação suprimida.")
        return false
    }

    // 1) Canal
    NotificationUtils.ensureChannel(context)

    // 2) Permissão (Tiramisu+)
    if (!PermissionUtils.hasNotificationPermission(context)) {
        Log.w("Geofence", "Sem permissão de notificações.")
        return false
    }

    // 3) Construção
    val notification = NotificationUtils.createNotification(
        context = context,
        placeName = placeName
    )

    // 4) Entrega
    val ok = NotificationUtils.showNotification(context, notification)
    Log.d("Geofence", "notificação mostrada? $ok")
    return ok
}
