package com.babymakisuk.coredata.repository

import com.babymakisuk.coredata.dao.ChildDao
import com.babymakisuk.coredata.di.ApplicationScope
import com.babymakisuk.coredata.di.IoDispatcher
import com.babymakisuk.coredata.entity.toDomain
import com.babymakisuk.coredata.entity.toEntity
import com.babymakisuk.coremodel.ChildProfile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

// TODO [TEST] All DefaultXxxRepository: add fake implementation of interface for unit testing

interface ChildRepository {
    fun observeAll(): Flow<List<ChildProfile>>
    suspend fun getChildren(): List<ChildProfile>
    suspend fun getById(id: Long): ChildProfile?
    suspend fun save(child: ChildProfile)
    suspend fun delete(child: ChildProfile)
}

@Singleton
class DefaultChildRepository @Inject constructor(
    private val dao: ChildDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val appScope: CoroutineScope
) : ChildRepository {

    private val _cache = dao.observeAll()
        .map { it.map { e -> e.toDomain() } }
        .flowOn(ioDispatcher)
        .shareIn(appScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    override fun observeAll(): Flow<List<ChildProfile>> = _cache
    // TODO [OFFLINE]: when Firebase sync is added, merge remote flow here via combine()

    override suspend fun getChildren(): List<ChildProfile> = withContext(ioDispatcher) {
        dao.getAllOnce().map { it.toDomain() }
    }

    override suspend fun getById(id: Long): ChildProfile? = withContext(ioDispatcher) {
        dao.getById(id)?.toDomain()
    }

    override suspend fun save(child: ChildProfile) = withContext(ioDispatcher) {
        if (child.id == 0L) {
            dao.upsert(child.toEntity())
        } else {
            dao.update(child.toEntity())
        }
        Unit
    }

    override suspend fun delete(child: ChildProfile) = withContext(ioDispatcher) {
        dao.delete(child.toEntity())
    }
}
