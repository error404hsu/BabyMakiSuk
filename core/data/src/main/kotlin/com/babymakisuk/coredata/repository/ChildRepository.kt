package com.babymakisuk.coredata.repository

import com.babymakisuk.coredata.dao.ChildDao
import com.babymakisuk.coredata.entity.toDomain
import com.babymakisuk.coredata.entity.toEntity
import com.babymakisuk.coremodel.ChildProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChildRepository @Inject constructor(private val dao: ChildDao) {
    fun observeAll(): Flow<List<ChildProfile>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    suspend fun getById(id: Long): ChildProfile? = dao.getById(id)?.toDomain()
    suspend fun save(child: ChildProfile): Long = dao.upsert(child.toEntity())
    suspend fun delete(child: ChildProfile) = dao.delete(child.toEntity())
}
