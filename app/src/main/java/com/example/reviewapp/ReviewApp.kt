package com.example.reviewapp

import android.app.Application
import com.example.reviewapp.di.ReviewPhotoSyncWorker
import com.google.android.libraries.places.api.Places
import dagger.hilt.android.HiltAndroidApp
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

/**
 * ## [ReviewApp]
 *
 * _Application_ raiz, anotada com [HiltAndroidApp], responsável por inicializações
 * globais e _bootstrapping_ de serviços de terceiros.
 *
 * Responsabilidades:
 * - **Inicializar Firebase** cedo no ciclo de vida do processo para garantir
 *   disponibilidade de Auth/Firestore/Storage em componentes injetados.
 * - **Ativar _logging_ do Firestore** em _debug_ (útil para diagnóstico de regras/índices).
 * - **Agendar o `ReviewPhotoSyncWorker`** para sincronização assíncrona de fotografias de
 *   avaliações (subida para _cloud_, _retry_ exponencial, conectividade, etc.).
 *
 * Observações:
 * - O `enqueue` do _worker_ é _idempotente_ (usar `UniqueWork` na implementação do _worker_)
 *   para evitar duplicações após reinícios da aplicação.
 */
@HiltAndroidApp
class ReviewApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Places.initialize(this, BuildConfig.PLACES_API_KEY)

        // 1) Firebase – obrigatório antes de qualquer uso de serviços Firebase.
        FirebaseApp.initializeApp(this)

        // 2) Firestore – logging verboso para inspeção de queries, índices e cache.
        FirebaseFirestore.setLoggingEnabled(true)

        // 3) Worker de sincronização de fotos – arranca em segundo plano conforme _constraints_.
        ReviewPhotoSyncWorker.enqueue(this)
    }
}
