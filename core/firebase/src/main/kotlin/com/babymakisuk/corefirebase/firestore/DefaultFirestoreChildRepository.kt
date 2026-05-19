package com.babymakisuk.corefirebase.firestore

import android.util.Log
import com.babymakisuk.coredata.di.ApplicationScope
import com.babymakisuk.coredata.di.IoDispatcher
import com.babymakisuk.coredata.repository.ChildRepository
import com.babymakisuk.coremodel.BloodType
import com.babymakisuk.coremodel.ChildProfile
import com.babymakisuk.coremodel.Gender
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultFirestoreChildRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val childRepository: ChildRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val appScope: CoroutineScope,
) : FirestoreChildRepository {

    companion object {
        private const val TAG = "FirestoreChildRepo"
    }

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
        combine(
            childRepository.observeAll(),
            observeFirestoreChildren()
        ) { local, remote ->
            if (remote.isNotEmpty()) remote else local
        }.flowOn(ioDispatcher)

    private fun observeFirestoreChildren(): Flow<List<ChildProfile>> = callbackFlow {
        val registration = firestore.collection("children")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Firestore snapshot error: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val children = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.getLong("id") ?: return@mapNotNull null
                            val name = doc.getString("name") ?: return@mapNotNull null
                            ChildProfile(
                                id = id,
                                name = name,
                                gender = Gender.valueOf(doc.getString("gender") ?: return@mapNotNull null),
                                birthday = LocalDate.parse(doc.getString("birthday") ?: return@mapNotNull null),
                                bloodType = doc.getString("bloodType")?.let { BloodType.valueOf(it) },
                                allergies = doc.getString("allergies"),
                                note = doc.getString("note") ?: "",
                            )
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to parse child doc: ${e.message}")
                            null
                        }
                    }
                    // Upsert remote data to Room asynchronously
                    if (children.isNotEmpty()) {
                        appScope.launch {
                            for (child in children) {
                                val existing = childRepository.getById(child.id)
                                val merged = child.copy(
                                    photoUri = existing?.photoUri,
                                    defaultAiPrompt = existing?.defaultAiPrompt
                                )
                                childRepository.save(merged)
                            }
                        }
                    }
                    trySend(children)
                }
            }
        awaitClose { registration.remove() }
    }

    override suspend fun getAllChildren(): List<ChildProfile> =
        childRepository.getChildren()
}
