package com.example.reviewapp.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.reviewapp.R

/**
 * Helpers para **notificações** com *custom view* (tile) e canal dedicado.
 *
 * Responsabilidades:
 * - Garantir canal `nearby_reviews` (Android O+);
 * - Construir notificações temáticas (dark/light) com cores da marca;
 * - Apresentar a notificação com ids não colidentes (timestamp).
 */
object NotificationUtils {
    private const val NEARBY_CHANNEL_ID = "nearby_reviews"

    /** Garante a existência do canal de notificações "Perto de si". */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (mgr.getNotificationChannel(NEARBY_CHANNEL_ID) == null) {
                val ch = NotificationChannel(
                    NEARBY_CHANNEL_ID,
                    context.getString(R.string.channel_nearby_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(R.string.channel_nearby_desc)
                }
                mgr.createNotificationChannel(ch)
            }
        }
    }

    /**
     * Cria uma notificação com _layout_ customizado (pequeno) adaptado ao tema.
     *
     * @param context contexto.
     * @param placeName nome do estabelecimento (pode ser `null` → titulo genérico).
     */
    fun createNotification(context: Context, placeName: String?): Notification {
        val isDark = (context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        val layoutId = if (isDark) R.layout.notification_tile_small_dark
        else       R.layout.notification_tile_small_light

        val titleText = context.getString(R.string.notif_nearby_title_prefix) + " " +
                (placeName ?: context.getString(R.string.channel_nearby_name))
        val bodyText  = context.getString(R.string.notif_nearby_distance)

        val titleColor = ContextCompat.getColor(
            context, if (isDark) R.color.neutral_on_dark else R.color.neutral_on_light
        )
        val textColor  = titleColor

        val content = RemoteViews(context.packageName, layoutId).apply {
            setImageViewResource(R.id.icon, R.drawable.logo)
            setTextViewText(R.id.title, titleText)
            setTextViewText(R.id.text, bodyText)
            setTextColor(R.id.title, titleColor)
            setTextColor(R.id.text,  textColor)
        }

        return NotificationCompat.Builder(context, NEARBY_CHANNEL_ID)
            .setSmallIcon(R.drawable.logo)
            .setCustomContentView(content)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setColorized(true)
            .setColor(ContextCompat.getColor(context, R.color.brand_lilac))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    /**
     * Envia a notificação com um id baseado no tempo atual.
     *
     * @return `true` em sucesso, `false` se falhar por falta de permissão (Tiramisu+).
     */
    fun showNotification(context: Context, notification: Notification): Boolean {
        return try {
            NotificationManagerCompat.from(context).notify(
                System.currentTimeMillis().toInt(), notification
            )
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
