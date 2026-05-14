package com.babymakisuk.coredata.repository

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.babymakisuk.coredata.dao.MemoDao
import com.babymakisuk.coredata.entity.toDomain
import com.babymakisuk.coredata.entity.toEntity
import com.babymakisuk.coremodel.Memo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoRepository @Inject constructor(
    private val dao: MemoDao,
    @param:ApplicationContext private val context: Context
) {

    fun observeAll(): Flow<List<Memo>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeByChildId(childId: Long): Flow<List<Memo>> =
        dao.observeByChildId(childId).map { list -> list.map { it.toDomain() } }

    @Suppress("unused")
    fun observeByChildIdAndDate(childId: Long, date: Long): Flow<List<Memo>> =
        dao.observeByChildIdAndDate(childId, date).map { list -> list.map { it.toDomain() } }

    suspend fun getByDate(date: Long): List<Memo> =
        dao.getByDate(date).map { it.toDomain() }

    @Suppress("unused")
    suspend fun getByChildIdAndDate(childId: Long, date: Long): List<Memo> =
        dao.getByChildIdAndDate(childId, date).map { it.toDomain() }

    suspend fun save(memo: Memo): Long {
        val id = dao.insert(memo.toEntity())
        scheduleReminder(id, memo.title, memo.content, memo.reminderAt)
        return id
    }

    suspend fun update(memo: Memo) {
        dao.update(memo.toEntity())
        scheduleReminder(memo.id, memo.title, memo.content, memo.reminderAt)
    }

    suspend fun deleteById(id: Long) {
        cancelReminder(id)
        dao.deleteById(id)
    }

    @Suppress("unused")
    suspend fun getLatestByChildId(childId: Long): Memo? =
        dao.getLatestByChildId(childId)?.toDomain()

    private fun scheduleReminder(memoId: Long, title: String, content: String, reminderAt: Long?) {
        cancelReminder(memoId)
        if (reminderAt == null) return

        val now = System.currentTimeMillis()
        val delay = reminderAt - now
        if (delay <= 0) return

        val data = Data.Builder()
            .putString("title", title)
            .putString("content", content)
            .putLong("memo_id", memoId)
            .build()

        val request = OneTimeWorkRequestBuilder<com.babymakisuk.coredata.worker.MemoReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag("memo_$memoId")
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "memo_reminder_$memoId",
                ExistingWorkPolicy.REPLACE,
                request
            )
    }

    private fun cancelReminder(memoId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork("memo_reminder_$memoId")
    }
}
