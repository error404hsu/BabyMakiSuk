package com.babymakisuk.coredata.repository

import com.babymakisuk.coredata.dao.VaccineReminderDao
import com.babymakisuk.coredata.entity.toDomain
import com.babymakisuk.coredata.entity.toEntity
import com.babymakisuk.coremodel.VaccineReminder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaccineReminderRepository @Inject constructor(private val dao: VaccineReminderDao) {

    fun observeAll(): Flow<List<VaccineReminder>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeByChild(childId: Long): Flow<List<VaccineReminder>> =
        dao.observeByChild(childId).map { list -> list.map { it.toDomain() } }

    suspend fun getNextDue(childId: Long): VaccineReminder? {
        val now = System.currentTimeMillis()
        return dao.getNextDue(childId, now)?.toDomain()
    }

    suspend fun save(reminder: VaccineReminder) {
        dao.insert(reminder.toEntity())
    }

    suspend fun update(reminder: VaccineReminder) {
        dao.update(reminder.toEntity())
    }

    suspend fun delete(reminder: VaccineReminder) {
        dao.delete(reminder.toEntity())
    }

    suspend fun generateDefaultSchedule(birthday: LocalDate, childId: Long) {
        // 預設疫苗項目已移除，改由使用者手動新增
    }
}
