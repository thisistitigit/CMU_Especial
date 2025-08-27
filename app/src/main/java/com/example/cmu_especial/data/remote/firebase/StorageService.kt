package com.example.cmu_especial.data.remote.firebase

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

//fotos

/**
 * Serviço simples para upload de imagens para Firebase Storage.
 * Devolve o download URL público (ou autenticado conforme regras).
 */
@Singleton
class StorageService @Inject constructor(
    private val storage: FirebaseStorage
) {
    /**
     * Faz upload de uma imagem local para a pasta remota definida.
     * @param localUri Uri do ficheiro local (ex.: content:// ou file://)
     * @param remoteFolder pasta no Storage (ex.: "reviews/EST123")
     * @param fileName opcional; por defeito usa UUID.jpg
     * @return URL de download (string) pronto a guardar/mostrar
     */
    suspend fun uploadPhoto(
        localUri: Uri,
        remoteFolder: String,
        fileName: String = "${UUID.randomUUID()}.jpg"
    ): String {
        val ref = storage.reference.child("$remoteFolder/$fileName")
        // Faz o upload (putFile) e espera de forma suspensiva
        val taskSnapshot = ref.putFile(localUri).await()
        // Obtém o download URL
        return taskSnapshot.storage.downloadUrl.await().toString()
    }
}
