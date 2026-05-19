package com.babymakisuk.corefirebase.auth

import com.babymakisuk.coremodel.UserRole
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface FirebaseAuthRepository {
    suspend fun signInAnonymously(): Result<FirebaseUser>
    fun observeAuthState(): Flow<FirebaseUser?>
    fun getCurrentUserId(): String?
    suspend fun refreshCustomClaims(): UserRole
    suspend fun linkWithGoogleCredential(idToken: String): Result<FirebaseUser>
    fun signOut()
}
