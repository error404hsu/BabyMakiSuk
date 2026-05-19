package com.babymakisuk.corefirebase.firestore

import com.babymakisuk.coremodel.ChildProfile
import kotlinx.coroutines.flow.Flow

interface FirestoreChildRepository {
    suspend fun upsertChild(child: ChildProfile)
    suspend fun deleteChild(childId: Long)
    fun observeAllChildren(): Flow<List<ChildProfile>>
    suspend fun getAllChildren(): List<ChildProfile>
}
