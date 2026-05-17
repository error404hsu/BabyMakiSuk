package com.babymakisuk.coredata.repository

import com.babymakisuk.coredata.dao.ToiletDao
import com.babymakisuk.coredata.di.IoDispatcher
import com.babymakisuk.coredata.entity.toDomain
import com.babymakisuk.coredata.entity.toEntity
import com.babymakisuk.coremodel.ToiletRecord
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

// TODO [TEST] All DefaultXxxRepository: add fake implementation of interface for unit testing

interface ToiletRepository {
    suspend fun insertToilet(record: ToiletRecord)
    suspend fun getLatestToiletTime(childId: Long): Long?
    fun getToiletRecords(childId: Long): Flow<List<ToiletRecord>>
}

@Singleton
class DefaultToiletRepository @Inject constructor(
    private val dao: ToiletDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ToiletRepository {

    override suspend fun insertToilet(record: ToiletRecord) = withContext(ioDispatcher) {
        dao.insert(record.toEntity())
    }

    override suspend fun getLatestToiletTime(childId: Long): Long? = withContext(ioDispatcher) {
        val all = dao.getByChild(childId).first()
        all.firstOrNull()?.timestamp
    }

    override fun getToiletRecords(childId: Long): Flow<List<ToiletRecord>> =
        dao.getByChild(childId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)
}
