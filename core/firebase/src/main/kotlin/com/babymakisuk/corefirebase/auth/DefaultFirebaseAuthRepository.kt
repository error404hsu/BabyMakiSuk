package com.babymakisuk.corefirebase.auth

import com.babymakisuk.coremodel.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultFirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
) : FirebaseAuthRepository {

    override fun getCurrentUser(): FirebaseUser? = auth.currentUser

    override suspend fun signInAnonymously(): Result<FirebaseUser> = runCatching {
        auth.signInAnonymously().await().user ?: throw IllegalStateException("Anonymous sign-in returned null user")
    }

    override fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override fun getCurrentUserId(): String? = auth.currentUser?.uid

    override suspend fun refreshCustomClaims(): UserRole = runCatching {
        val tokenResult = auth.currentUser?.getIdToken(true)?.await()
        val role = tokenResult?.claims?.get("role") as? String ?: return@runCatching UserRole.NONE
        when (role) {
            "data_manager" -> UserRole.DATA_MANAGER
            "ai_operator" -> UserRole.AI_OPERATOR
            "admin" -> UserRole.ADMIN
            else -> UserRole.NONE
        }
    }.getOrDefault(UserRole.NONE)

    override suspend fun linkWithGoogleCredential(idToken: String): Result<FirebaseUser> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.currentUser?.linkWithCredential(credential)?.await()?.user
            ?: throw IllegalStateException("Google link returned null user")
    }

    override fun signOut() = auth.signOut()
}
