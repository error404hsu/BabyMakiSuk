package com.babymakisuk.coredata.repository

import com.babymakisuk.coredata.dao.GrowthDao
import com.babymakisuk.coredata.di.IoDispatcher
import com.babymakisuk.coredata.entity.toDomain
import com.babymakisuk.coredata.entity.toEntity
import com.babymakisuk.coremodel.GrowthRecord
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

// TODO [TEST] All DefaultXxxRepository: add fake implementation of interface for unit testing

interface GrowthRepository {
    fun observeByChild(childId: Long): Flow<List<GrowthRecord>>
    suspend fun save(record: GrowthRecord): Long
    suspend fun delete(record: GrowthRecord)
    suspend fun getLatest(childId: Long): GrowthRecord?
}

@Singleton
class DefaultGrowthRepository @Inject constructor(
    private val dao: GrowthDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : GrowthRepository {

    override fun observeByChild(childId: Long): Flow<List<GrowthRecord>> =
        dao.observeByChild(childId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun save(record: GrowthRecord): Long = withContext(ioDispatcher) {
        dao.upsert(record.toEntity())
    }

    override suspend fun delete(record: GrowthRecord) = withContext(ioDispatcher) {
        dao.delete(record.toEntity())
    }

    override suspend fun getLatest(childId: Long): GrowthRecord? = withContext(ioDispatcher) {
        dao.getLatestByChild(childId)?.toDomain()
    }
}
