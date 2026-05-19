package com.babymakisuk.corefirebase.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.babymakisuk.coredata.repository.MedicalRepository
import com.babymakisuk.coremodel.ImageStoragePath
import com.babymakisuk.corefirebase.storage.StorageRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@HiltWorker
class StorageCleanupWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val medicalRepo: MedicalRepository,
    private val storageRepository: StorageRepository,
) : CoroutineWorker(ctx, params) {

    companion object {
        const val WORK_NAME = "StorageCleanupWorker"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val now = LocalDate.now()
            val nextDec31 = now.with(java.time.MonthDay.of(12, 31))
            val initialDelayDays = nextDec31.toEpochDay() - now.toEpochDay()
            val request = PeriodicWorkRequestBuilder<StorageCleanupWorker>(365, TimeUnit.DAYS)
                .setConstraints(constraints)
                .setInitialDelay(initialDelayDays.coerceAtLeast(0), TimeUnit.DAYS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val cutoff = LocalDate.now().minusYears(1)
            val visits = medicalRepo.getAllOnce()
            val oldPaths = visits
                .filter { it.date.isBefore(cutoff) }
                .mapNotNull { visit ->
                    when (val path = visit.imageStoragePath) {
                        is ImageStoragePath.FirebaseStorage -> path.storagePath
                        else -> null
                    }
                }
            for (path in oldPaths) {
                storageRepository.delete(path)
            }
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
