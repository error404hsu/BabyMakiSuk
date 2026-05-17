package com.babymakisuk.coredata.repository

import com.babymakisuk.coredata.dao.SystemReminderDao
import com.babymakisuk.coredata.di.IoDispatcher
import com.babymakisuk.coredata.entity.toDomain
import com.babymakisuk.coredata.entity.toEntity
import com.babymakisuk.coremodel.SystemReminder
import com.babymakisuk.coremodel.SystemReminderType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

// TODO [TEST] All DefaultXxxRepository: add fake implementation of interface for unit testing

interface SystemReminderRepository {
    fun getByChildId(childId: Long): Flow<List<SystemReminder>>
    fun getUnresolvedByType(childId: Long, type: SystemReminderType): Flow<List<SystemReminder>>
    suspend fun upsert(reminder: SystemReminder)
    suspend fun markResolved(id: String, resolvedAt: Long = System.currentTimeMillis())
    suspend fun markAllResolvedByType(childId: Long, type: SystemReminderType, resolvedAt: Long = System.currentTimeMillis())
    suspend fun deleteById(id: String)
    suspend fun createLongNoBmReminder(childId: Long, hoursSince: Int)
}

@Singleton
class DefaultSystemReminderRepository @Inject constructor(
    private val dao: SystemReminderDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : SystemReminderRepository {

    override fun getByChildId(childId: Long): Flow<List<SystemReminder>> =
        dao.getByChildIdIncludingGlobal(childId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun getUnresolvedByType(childId: Long, type: SystemReminderType): Flow<List<SystemReminder>> =
        dao.getUnresolvedByType(childId, type.name)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun upsert(reminder: SystemReminder) = withContext(ioDispatcher) {
        dao.insert(reminder.toEntity())
    }

    override suspend fun markResolved(id: String, resolvedAt: Long) = withContext(ioDispatcher) {
        dao.markResolved(id, resolvedAt)
    }

    override suspend fun markAllResolvedByType(childId: Long, type: SystemReminderType, resolvedAt: Long) = withContext(ioDispatcher) {
        dao.markAllResolvedByType(childId, type.name, resolvedAt)
    }

    override suspend fun deleteById(id: String) = withContext(ioDispatcher) {
        dao.deleteById(id)
    }

    override suspend fun createLongNoBmReminder(childId: Long, hoursSince: Int) = withContext(ioDispatcher) {
        val id = "nobm_${childId}_${System.currentTimeMillis()}"
        val reminder = SystemReminder(
            id = id,
            childId = childId,
            type = SystemReminderType.LONG_NO_BM,
            title = "超過 ${hoursSince} 小時未排便",
            content = "上次排便記錄距今已超過 ${hoursSince} 小時，敬請關注寶寶排便狀況。",
            createdAt = System.currentTimeMillis(),
            resolvedAt = null
        )
        dao.insert(reminder.toEntity())
    }
}
