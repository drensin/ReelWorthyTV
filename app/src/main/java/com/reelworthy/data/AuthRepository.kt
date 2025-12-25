package com.reelworthy.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Manages user authentication state and credential retrieval.
 *
 * This repository wraps [FirebaseAuth] and Google Sign-In logic to provide
 * observable user state and specific tokens required for API access (e.g., YouTube Data API).
 *
 * @property auth The FirebaseAuth instance. Defaults to [FirebaseAuth.getInstance].
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    /**
     * A Flow emitting the current [FirebaseUser] whenever the auth state changes.
     * Emits `null` if the user is signed out.
     */
    val currentUser: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Synchronously retrieves the current user.
     * @return The [FirebaseUser] or null.
     */
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    /**
     * Signs out the current user from Firebase.
     */
    suspend fun signOut() {
        auth.signOut()
    }

    /**
     * Signs in using a specific credential (e.g., from Google Sign-In result).
     * @param credential The Firebase AuthCredential.
     */
    suspend fun signInWithCredential(credential: com.google.firebase.auth.AuthCredential) {
        auth.signInWithCredential(credential).await()
    }
    
    /**
     * Retrieves a valid OAuth 2.0 Access Token for the "youtube.readonly" scope.
     *
     * This method requires a valid signed-in Google Account. It uses [GoogleAuthUtil]
     * to fetch the token synchronously on an IO thread.
     *
     * @param context The Android Context (needed for account retrieval).
     * @return The access token string, or null if retrieval failed (or user not signed in).
     */
    suspend fun getAccessToken(context: android.content.Context): String? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val user = getCurrentUser() ?: return@withContext null
            // We need the Google Account, not Firebase User, to get the token for Google APIs.
            // However, FirebaseUser doesn't expose the Google Account directly easily.
            // We typically need the account from GoogleSignIn.getLastSignedInAccount.
            
            // Allow passing account, or try to get it from context if possible
            val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
            if (account != null) {
                // Scope must match what we requested
                val scope = "oauth2:https://www.googleapis.com/auth/youtube.readonly"
                try {
                     com.google.android.gms.auth.GoogleAuthUtil.getToken(context, account.account!!, scope)
                } catch (e: Exception) {
                    android.util.Log.e("AuthRepository", "Error getting token", e)
                    null
                }
            } else {
                null
            }
        }
    }
}
