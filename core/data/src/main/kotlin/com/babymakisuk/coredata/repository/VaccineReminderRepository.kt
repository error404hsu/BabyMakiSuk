package com.babymakisuk.coredata.repository

import com.babymakisuk.coredata.dao.VaccineReminderDao
import com.babymakisuk.coredata.di.IoDispatcher
import com.babymakisuk.coredata.entity.toDomain
import com.babymakisuk.coredata.entity.toEntity
import com.babymakisuk.coremodel.VaccineReminder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

// TODO [OFFLINE] VaccineReminderRepository: add lastSyncTimestamp to skip redundant fetches
// TODO [TEST] All DefaultXxxRepository: add fake implementation of interface for unit testing

interface VaccineReminderRepository {
    fun observeAll(): Flow<List<VaccineReminder>>
    fun observeByChild(childId: Long): Flow<List<VaccineReminder>>
    suspend fun getNextDue(childId: Long): VaccineReminder?
    suspend fun save(reminder: VaccineReminder)
    suspend fun update(reminder: VaccineReminder)
    suspend fun delete(reminder: VaccineReminder)
    suspend fun generateDefaultSchedule(birthday: LocalDate, childId: Long)
}

@Singleton
class DefaultVaccineReminderRepository @Inject constructor(
    private val dao: VaccineReminderDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : VaccineReminderRepository {

    override fun observeAll(): Flow<List<VaccineReminder>> =
        dao.observeAll()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun observeByChild(childId: Long): Flow<List<VaccineReminder>> =
        dao.observeByChild(childId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun getNextDue(childId: Long): VaccineReminder? = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        dao.getNextDue(childId, now)?.toDomain()
    }

    override suspend fun save(reminder: VaccineReminder) = withContext(ioDispatcher) {
        dao.insert(reminder.toEntity())
    }

    override suspend fun update(reminder: VaccineReminder) = withContext(ioDispatcher) {
        dao.update(reminder.toEntity())
    }

    override suspend fun delete(reminder: VaccineReminder) = withContext(ioDispatcher) {
        dao.delete(reminder.toEntity())
    }

    override suspend fun generateDefaultSchedule(birthday: LocalDate, childId: Long) = withContext(ioDispatcher) {
        // 預設疫苗項目已移除，改由使用者手動新增
    }
}
