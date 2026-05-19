package com.babymakisuk.corefirebase.auth

import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface FirebaseAuthRepository {
    suspend fun signInAnonymously(): Result<FirebaseUser>
    fun observeAuthState(): Flow<FirebaseUser?>
    fun getCurrentUserId(): String?
    fun signOut()
}
