package com.example.reviewapp.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

/**
 * Utilitários para criar entradas no **MediaStore** (galeria) de forma segura.
 *
 * - Em Android 10+ grava para `VOLUME_EXTERNAL_PRIMARY`;
 * - Em versões anteriores usa `EXTERNAL_CONTENT_URI`.
 */
object MediaStoreUtils {

    /**
     * Cria e regista um `Uri` de imagem JPEG na galeria.
     *
     * @param context contexto.
     * @param displayName nome do ficheiro (ex.: `"review_1690000000000.jpg"`).
     * @return `Uri` pronto a ser escrito via `ContentResolver.openOutputStream`, ou `null` se falhar.
     */
    fun createImageUri(context: Context, displayName: String = "review_${System.currentTimeMillis()}.jpg"): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        return context.contentResolver.insert(collection, contentValues)
    }
}
