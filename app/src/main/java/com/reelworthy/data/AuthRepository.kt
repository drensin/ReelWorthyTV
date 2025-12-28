package com.reelworthy.data

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages user authentication state and credential retrieval via Google Sign-In.
 *
 * Removes Firebase dependency and relies purely on GoogleSignInAccount.
 */
class AuthRepository(context: Context) {

    // Ideally, we'd inject GoogleSignInClient, but for now we'll construct or access it via helper
    // if needed.
    // However, GoogleSignIn.getLastSignedInAccount is static and synchronous-like (fast).

    private val _currentUser = MutableStateFlow<GoogleSignInAccount?>(null)
    val currentUser: Flow<GoogleSignInAccount?> = _currentUser.asStateFlow()

    init {
        // Initialize state
        refreshUser(context)
    }

    /** Refreshes the current user state from GoogleSignIn. */
    fun refreshUser(context: Context) {
        _currentUser.value = GoogleSignIn.getLastSignedInAccount(context)
    }

    /** Synchronously retrieves the current user. */
    fun getCurrentUser(context: Context): GoogleSignInAccount? {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        _currentUser.value = account // keep flow in sync
        return account
    }

    /** Signs out the current user. */
    suspend fun signOut(client: GoogleSignInClient) {
        try {
            client.signOut().addOnCompleteListener { _currentUser.value = null }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Retrieves a valid OAuth 2.0 Access Token for the "youtube.readonly" scope. */
    suspend fun getAccessToken(context: Context): String? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val account = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext null

            // Scope must match what we requested
            val scope = "oauth2:https://www.googleapis.com/auth/youtube.readonly"
            try {
                com.google.android.gms.auth.GoogleAuthUtil.getToken(
                        context,
                        account.account!!,
                        scope
                )
            } catch (e: Exception) {
                android.util.Log.e("AuthRepository", "Error getting token", e)
                null
            }
        }
    }
}
