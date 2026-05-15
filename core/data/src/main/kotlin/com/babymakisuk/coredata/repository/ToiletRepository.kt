package com.babymakisuk.coredata.repository

import com.babymakisuk.coredata.dao.ToiletDao
import com.babymakisuk.coredata.entity.toDomain
import com.babymakisuk.coredata.entity.toEntity
import com.babymakisuk.coremodel.ToiletRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToiletRepository @Inject constructor(private val dao: ToiletDao) {

    suspend fun insertToilet(record: ToiletRecord) {
        dao.insert(record.toEntity())
    }

    suspend fun getLatestToiletTime(childId: Long): Long? {
        val all = dao.getByChild(childId).first()
        return all.firstOrNull()?.timestamp
    }

    fun getToiletRecords(childId: Long): Flow<List<ToiletRecord>> =
        dao.getByChild(childId).map { list -> list.map { it.toDomain() } }
}
