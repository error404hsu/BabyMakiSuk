package com.babymakisuk.coredata.repository

import com.babymakisuk.coredata.dao.SystemReminderDao
import com.babymakisuk.coredata.entity.SystemReminderEntity
import com.babymakisuk.coredata.entity.toDomain
import com.babymakisuk.coredata.entity.toEntity
import com.babymakisuk.coremodel.SystemReminder
import com.babymakisuk.coremodel.SystemReminderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemReminderRepository @Inject constructor(
    private val dao: SystemReminderDao
) {

    fun getByChildId(childId: Long): Flow<List<SystemReminder>> =
        dao.getByChildId(childId).map { list -> list.map { it.toDomain() } }

    fun getUnresolvedByType(childId: Long, type: SystemReminderType): Flow<List<SystemReminder>> =
        dao.getUnresolvedByType(childId, type.name).map { list -> list.map { it.toDomain() } }

    suspend fun upsert(reminder: SystemReminder) {
        dao.insert(reminder.toEntity())
    }

    suspend fun markResolved(id: String, resolvedAt: Long = System.currentTimeMillis()) {
        dao.markResolved(id, resolvedAt)
    }

    suspend fun markAllResolvedByType(childId: Long, type: SystemReminderType, resolvedAt: Long = System.currentTimeMillis()) {
        dao.markAllResolvedByType(childId, type.name, resolvedAt)
    }

    suspend fun deleteById(id: String) {
        dao.deleteById(id)
    }

    suspend fun createLongNoBmReminder(childId: Long, hoursSince: Int) {
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
