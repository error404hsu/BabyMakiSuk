package com.babymakisuk.coredata.repository

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.babymakisuk.coredata.dao.MemoDao
import com.babymakisuk.coredata.di.IoDispatcher
import com.babymakisuk.coredata.entity.toDomain
import com.babymakisuk.coredata.entity.toEntity
import com.babymakisuk.coremodel.Memo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

// TODO [PERF] MemoRepository: add Paging3 for memo list if user has > 100 memos
// TODO [TEST] All DefaultXxxRepository: add fake implementation of interface for unit testing

interface MemoRepository {
    fun observeAll(): Flow<List<Memo>>
    fun observeByChildId(childId: Long): Flow<List<Memo>>
    fun observeByChildIdAndDate(childId: Long, date: Long): Flow<List<Memo>>
    suspend fun getByDate(date: Long): List<Memo>
    suspend fun getByChildIdAndDate(childId: Long, date: Long): List<Memo>
    suspend fun save(memo: Memo): Long
    suspend fun update(memo: Memo)
    suspend fun deleteById(id: Long)
    suspend fun getLatestByChildId(childId: Long): Memo?
}

@Singleton
class DefaultMemoRepository @Inject constructor(
    private val dao: MemoDao,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : MemoRepository {

    override fun observeAll(): Flow<List<Memo>> =
        dao.observeAll()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun observeByChildId(childId: Long): Flow<List<Memo>> =
        dao.observeByChildId(childId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun observeByChildIdAndDate(childId: Long, date: Long): Flow<List<Memo>> =
        dao.observeByChildIdAndDate(childId, date)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun getByDate(date: Long): List<Memo> = withContext(ioDispatcher) {
        dao.getByDate(date).map { it.toDomain() }
    }

    override suspend fun getByChildIdAndDate(childId: Long, date: Long): List<Memo> = withContext(ioDispatcher) {
        dao.getByChildIdAndDate(childId, date).map { it.toDomain() }
    }

    override suspend fun save(memo: Memo): Long = withContext(ioDispatcher) {
        val id = dao.insert(memo.toEntity())
        scheduleReminder(id, memo.title, memo.content, memo.reminderAt)
        id
    }

    override suspend fun update(memo: Memo) = withContext(ioDispatcher) {
        dao.update(memo.toEntity())
        scheduleReminder(memo.id, memo.title, memo.content, memo.reminderAt)
    }

    override suspend fun deleteById(id: Long) = withContext(ioDispatcher) {
        cancelReminder(id)
        dao.deleteById(id)
    }

    override suspend fun getLatestByChildId(childId: Long): Memo? = withContext(ioDispatcher) {
        dao.getLatestByChildId(childId)?.toDomain()
    }

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
