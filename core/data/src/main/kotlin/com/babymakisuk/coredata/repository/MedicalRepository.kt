package com.babymakisuk.coredata.repository

import com.babymakisuk.coredata.dao.MedicalDao
import com.babymakisuk.coredata.di.IoDispatcher
import com.babymakisuk.coredata.entity.toDomain
import com.babymakisuk.coredata.entity.toEntity
import com.babymakisuk.coremodel.MedicalVisit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface MedicalRepository {
    fun observeByChild(childId: Long): Flow<List<MedicalVisit>>
    suspend fun getAllOnce(): List<MedicalVisit>
    suspend fun getById(id: Long): MedicalVisit?
    suspend fun upsert(visit: MedicalVisit): Long
    suspend fun delete(visit: MedicalVisit)
    suspend fun updateAiFields(
        id: Long,
        diagnosisSummary: String,
        prescriptions: String,
        careInstructions: String,
        isUrgent: Boolean
    )
    fun observeAiPending(): Flow<List<MedicalVisit>>
    suspend fun count(): Int
}

@Singleton
class DefaultMedicalRepository @Inject constructor(
    private val dao: MedicalDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : MedicalRepository {

    override fun observeByChild(childId: Long): Flow<List<MedicalVisit>> =
        dao.observeByChild(childId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun getAllOnce(): List<MedicalVisit> = withContext(ioDispatcher) {
        dao.getAllOnce().map { it.toDomain() }
    }

    override suspend fun getById(id: Long): MedicalVisit? = withContext(ioDispatcher) {
        dao.getById(id)?.toDomain()
    }

    override suspend fun upsert(visit: MedicalVisit): Long = withContext(ioDispatcher) {
        dao.upsert(visit.toEntity())
    }

    override suspend fun delete(visit: MedicalVisit) = withContext(ioDispatcher) {
        dao.delete(visit.toEntity())
    }

    override suspend fun updateAiFields(
        id: Long,
        diagnosisSummary: String,
        prescriptions: String,
        careInstructions: String,
        isUrgent: Boolean
    ) = withContext(ioDispatcher) {
        dao.updateAiFields(id, diagnosisSummary, prescriptions, careInstructions, isUrgent)
    }

    override fun observeAiPending(): Flow<List<MedicalVisit>> =
        dao.observeAiPending()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun count(): Int = withContext(ioDispatcher) {
        dao.count()
    }
}
