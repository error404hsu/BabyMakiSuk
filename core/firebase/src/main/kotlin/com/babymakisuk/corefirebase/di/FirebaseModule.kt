package com.babymakisuk.corefirebase.di

import com.babymakisuk.corefirebase.auth.DefaultFirebaseAuthRepository
import com.babymakisuk.corefirebase.auth.FirebaseAuthRepository
import com.babymakisuk.corefirebase.firestore.DefaultFirestoreChildRepository
import com.babymakisuk.corefirebase.firestore.DefaultFirestoreMedicalRepository
import com.babymakisuk.corefirebase.firestore.FirestoreChildRepository
import com.babymakisuk.corefirebase.firestore.FirestoreMedicalRepository
import com.babymakisuk.corefirebase.storage.DefaultImageUploadRepository
import com.babymakisuk.corefirebase.storage.ImageUploadRepository
import com.babymakisuk.corefirebase.storage.MedicalImageCacheManager
import com.babymakisuk.corefirebase.storage.StorageRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.storage.FirebaseStorage
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance().apply {
        firestoreSettings = firestoreSettings {
            setPersistenceEnabled(true)
        }
    }

    @Provides @Singleton
    fun provideAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides @Singleton
    fun provideStorage(): FirebaseStorage = FirebaseStorage.getInstance()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class FirebaseBindModule {
    @Binds @Singleton
    abstract fun bindAuthRepository(impl: DefaultFirebaseAuthRepository): FirebaseAuthRepository

    @Binds @Singleton
    abstract fun bindChildRepository(impl: DefaultFirestoreChildRepository): FirestoreChildRepository

    @Binds @Singleton
    abstract fun bindMedicalRepository(impl: DefaultFirestoreMedicalRepository): FirestoreMedicalRepository

    @Binds @Singleton
    abstract fun bindImageUploadRepository(impl: DefaultImageUploadRepository): ImageUploadRepository
}
