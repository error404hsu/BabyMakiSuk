package com.babymakisuk.corefirebase.firestore

import com.babymakisuk.coredata.repository.ChildRepository
import com.babymakisuk.coremodel.ChildProfile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultFirestoreChildRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val childRepository: ChildRepository,
) : FirestoreChildRepository {

    override suspend fun upsertChild(child: ChildProfile) {
        val data = mapOf(
            "id" to child.id,
            "name" to child.name,
            "gender" to child.gender.name,
            "birthday" to child.birthday.toString(),
            "bloodType" to child.bloodType?.name,
            "allergies" to child.allergies,
            "note" to child.note,
            "lastModified" to System.currentTimeMillis()
        )
        firestore.collection("children").document(child.id.toString()).set(data).await()
    }

    override suspend fun deleteChild(childId: Long) {
        firestore.collection("children").document(childId.toString()).delete().await()
    }

    override fun observeAllChildren(): Flow<List<ChildProfile>> =
        childRepository.observeAll()

    override suspend fun getAllChildren(): List<ChildProfile> =
        childRepository.getChildren()
}
