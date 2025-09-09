package com.example.reviewapp.di

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.reviewapp.data.dao.ReviewDao
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * **Worker** para sincronizar fotos de reviews com o Firebase Storage
 * e refletir o URL em Firestore + Room.
 *
 * Estratégia:
 * 1) seleciona as reviews pendentes (foto local, sem `photoCloudUrl`);
 * 2) `putFile` → obtém `downloadUrl`;
 * 3) atualiza Firestore (`photoCloudUrl`) e Room (`updateCloudUrl`);
 * 4) repete em lote; se alguma falhar, devolve `Result.retry()`.
 *
 * **Constraints:** rede conectada e bateria não fraca.
 *
 * @since 1.0
 */
@HiltWorker
class ReviewPhotoSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val reviewDao: ReviewDao,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val singleId = inputData.getString(KEY_REVIEW_ID)

        val reviews = if (singleId != null) {
            reviewDao.getById(singleId)?.let { listOf(it) } ?: emptyList()
        } else {
            reviewDao.pendingPhotoUpload(50)
        }

        if (reviews.isEmpty()) return Result.success()

        var hadFailures = false

        for (r in reviews) {
            val localPath = r.photoLocalPath ?: continue
            try {
                // 1) Upload para Storage
                val ref = storage.reference.child("reviews/${r.id}.jpg")
                val fileUri = Uri.fromFile(File(localPath))
                ref.putFile(fileUri).await()
                val url = ref.downloadUrl.await().toString()

                // 2) Atualizar Firestore
                firestore.collection("reviews").document(r.id)
                    .update(mapOf("photoCloudUrl" to url))
                    .await()

                // 3) Refletir em Room
                reviewDao.updateCloudUrl(r.id, url)
            } catch (_: Exception) {
                hadFailures = true
            }
        }

        return if (hadFailures) Result.retry() else Result.success()
    }

    companion object {
        private const val KEY_REVIEW_ID = "reviewId"

        /**
         * Agenda a sincronização de fotos.
         *
         * @param reviewId se fornecido, sincroniza apenas essa review; caso contrário, processa lote.
         * @see Constraints
         */
        fun enqueue(context: Context, reviewId: String? = null) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val data = workDataOf(KEY_REVIEW_ID to reviewId)

            val req = OneTimeWorkRequestBuilder<ReviewPhotoSyncWorker>()
                .setInputData(data)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30, TimeUnit.SECONDS
                )
                .build()

            val name = if (reviewId != null)
                "upload-review-$reviewId"
            else
                "upload-reviews-batch"

            WorkManager.getInstance(context)
                .enqueueUniqueWork(name, ExistingWorkPolicy.KEEP, req)
        }
    }
}
