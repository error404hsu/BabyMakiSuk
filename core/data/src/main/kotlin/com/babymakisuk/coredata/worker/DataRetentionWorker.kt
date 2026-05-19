package com.babymakisuk.coredata.worker

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
import androidx.work.workDataOf
import com.babymakisuk.coredata.repository.DataRetentionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@HiltWorker
class DataRetentionWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val retentionRepository: DataRetentionRepository,
) : CoroutineWorker(ctx, params) {

    companion object {
        const val WORK_NAME = "DataRetentionWorker"
        private val CUTOFF_60_DAYS = 60L * 24 * 3600 * 1000
        private val CUTOFF_180_DAYS = 180L * 24 * 3600 * 1000

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresCharging(true)
                .build()
            val request = PeriodicWorkRequestBuilder<DataRetentionWorker>(7, TimeUnit.DAYS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }
    }

    override suspend fun doWork(): Result {
        return try {
            // TODO [Phase F] 加入 backupManager.exportAndUploadToDrive() 前置檢查
            // 目前無 BackupManager，先跳過備份步驟
            // if (!backupSuccess) return Result.retry()

            val dailyCutoffDay = LocalDate.now().minusDays(90).toEpochDay()
            retentionRepository.cleanDailyLogsWithReportGuard(dailyCutoffDay)

            val now = System.currentTimeMillis()
            retentionRepository.cleanToiletRecordsWithReportGuard(now - CUTOFF_60_DAYS)
            retentionRepository.cleanAiInsights(now - CUTOFF_180_DAYS)
            retentionRepository.cleanTriggeredReminders()
            retentionRepository.cleanCompletedVaccineReminders()
            retentionRepository.rebuildFts()

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
