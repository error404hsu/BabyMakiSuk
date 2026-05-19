package com.babymakisuk.coredata.repository

import androidx.room.Transaction
import com.babymakisuk.coredata.dao.AiInsightDao
import com.babymakisuk.coredata.dao.DailyLogDao
import com.babymakisuk.coredata.dao.MonthlyReportDao
import com.babymakisuk.coredata.dao.SystemReminderDao
import com.babymakisuk.coredata.dao.ToiletDao
import com.babymakisuk.coredata.dao.VaccineReminderDao
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataRetentionRepository @Inject constructor(
    private val dailyLogDao: DailyLogDao,
    private val toiletDao: ToiletDao,
    private val aiInsightDao: AiInsightDao,
    private val systemReminderDao: SystemReminderDao,
    private val vaccineReminderDao: VaccineReminderDao,
    private val monthlyReportDao: MonthlyReportDao,
) {
    suspend fun cleanDailyLogsWithReportGuard(cutoffEpochDay: Long) {
        val cutoffDate = LocalDate.ofEpochDay(cutoffEpochDay).toString()
        dailyLogDao.deleteOlderThanWithReportGuard(cutoffDate)
    }

    suspend fun cleanToiletRecordsWithReportGuard(cutoffMillis: Long) =
        toiletDao.deleteOlderThanWithReportGuard(cutoffMillis)

    @Transaction
    suspend fun cleanRawDataForMonth(yearMonth: YearMonth) {
        val yearMonthStr = yearMonth.toString()
        dailyLogDao.deleteByYearMonth(yearMonthStr)
        toiletDao.deleteByYearMonth(yearMonthStr)
    }

    suspend fun cleanAiInsights(cutoffMillis: Long) = aiInsightDao.deleteOlderThan(cutoffMillis)
    suspend fun cleanTriggeredReminders() = systemReminderDao.deleteTriggered()
    suspend fun cleanCompletedVaccineReminders() = vaccineReminderDao.deleteCompleted()
    suspend fun rebuildFts() = monthlyReportDao.rebuildFts()
}
