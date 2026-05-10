package com.error404hsu.babymakisuk.coredata.repository

import com.error404hsu.babymakisuk.coredata.dao.GrowthDao
import com.error404hsu.babymakisuk.coredata.entity.toDomain
import com.error404hsu.babymakisuk.coredata.entity.toEntity
import com.error404hsu.babymakisuk.coremodel.GrowthRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GrowthRepository @Inject constructor(private val dao: GrowthDao) {
    fun observeByChild(childId: Long): Flow<List<GrowthRecord>> =
        dao.observeByChild(childId).map { list -> list.map { it.toDomain() } }
    suspend fun save(record: GrowthRecord): Long = dao.upsert(record.toEntity())
    suspend fun delete(record: GrowthRecord) = dao.delete(record.toEntity())
}
