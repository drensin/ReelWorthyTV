package com.reelworthy.ui

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.GoogleAuthProvider
import com.reelworthy.data.AuthRepository
import kotlinx.coroutines.launch

/**
 * The single Activity for the ReelWorthy TV application.
 *
 * This Activity serves as the entry point and dependency injection root (manual DI for MVP).
 * It manages:
 * - Application lifecycle.
 * - Authentication flow (Google Sign-In / Firebase).
 * - Navigation between Login and Dashboard/Settings.
 * - Initialization of Repositories and ViewModels.
 * - Scheduling of background synchronization workers.
 */
class MainActivity : ComponentActivity() {

    private val authRepository = AuthRepository()

    // Factory should inject this, but for simple MVP we can construct it if we get the key
    // Actually, we need the API Key from google-services or resources.
    // For now, I'll hardcode the known key for testing, or read it from a resource if safely possible.
    // Ideally, injected.
    private lateinit var chatViewModel: ChatViewModel

    private lateinit var settingsRepository: com.reelworthy.data.SettingsRepository
    private lateinit var settingsViewModel: SettingsViewModel

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(Exception::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                lifecycleScope.launch {
                    try {
                        authRepository.signInWithCredential(credential)
                        Toast.makeText(this@MainActivity, "Signed in as ${account.displayName}", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Firebase sign in failed", e)
                        Toast.makeText(this@MainActivity, "Auth Failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Google sign in failed", e)
                Toast.makeText(this@MainActivity, "Google Sign In Failed", Toast.LENGTH_LONG).show()
            }
        }
    }

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val db = com.reelworthy.data.AppDatabase.getDatabase(this)
        val videoDao = db.videoDao()
        val videoRepository = com.reelworthy.data.VideoRepository(videoDao)
        settingsRepository = com.reelworthy.data.SettingsRepository(this)
        
        // Securely retrieve API Key from BuildConfig
        val apiKey = com.reelworthy.BuildConfig.YOUTUBE_API_KEY

        settingsViewModel = SettingsViewModel(
            application = this.application,
            settingsRepository = settingsRepository,
            videoRepository = videoRepository,
            authRepository = authRepository,
            apiKey = apiKey
        )
        
        // Chat Repo now needs SettingsRepo too - but I haven't updated ChatRepo constructor yet! 
        // Wait, I should do that first or pass it here.
        // For now, let's just break ChatViewModel temporarily or inject it properly?
        // I need to update ChatRepository FIRST.
        // But for this edit, I will just setup the UI.
        
        // TODO: Update ChatViewModel to use Settings
        chatViewModel = ChatViewModel(com.reelworthy.BuildConfig.YOUTUBE_API_KEY, db.videoDao(), settingsRepository)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("679260739905-e699h3i2ns0vrgreomau1s0b364fbq2l.apps.googleusercontent.com")
            .requestEmail()
            .requestScopes(com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/youtube.readonly"))
            .build()
        val googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Schedule Background Sync
        scheduleBackgroundSync()

        setContent {
            val user by authRepository.currentUser.collectAsState(initial = null)
            var showSettings by remember { mutableStateOf(false) }

            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (user != null) {
                            if (showSettings) {
                                BackHandler { showSettings = false }
                                SettingsScreen(
                                    viewModel = settingsViewModel,
                                    onClose = { showSettings = false }
                                )
                            } else {
                                // Main Content: Dashboard "Concierge"
                                DashboardScreen(
                                    userDisplayName = user?.displayName,
                                    chatViewModel = chatViewModel,
                                    onOpenSettings = { showSettings = true },
                                    onSignOut = { lifecycleScope.launch { authRepository.signOut() } }
                                )
                            }
                        } else {
                            // Login Screen
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "ReelWorthy TV", style = MaterialTheme.typography.displayMedium)
                                Spacer(modifier = Modifier.height(30.dp))
                                Button(onClick = {
                                    signInLauncher.launch(googleSignInClient.signInIntent)
                                }) {
                                    Text("Sign In with Google")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Schedules WorkManager tasks for background data synchronization.
     *
     * - **Periodic Sync**: Runs every 6 hours (when charging/idle usually).
     * - **One-Time Sync**: Runs immediately on launch to refresh data.
     */
    private fun scheduleBackgroundSync() {
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()
            
        // 1. Periodic Sync (Every 6 hours)
        val periodicSyncRequest = androidx.work.PeriodicWorkRequest.Builder(
            com.reelworthy.workers.SyncWorker::class.java,
            6, java.util.concurrent.TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()
            
        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "periodic_sync",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            periodicSyncRequest
        )
        
        // 2. Launch Sync (Immediate One-Time)
        // We use KEEP so that if the periodic sync is already running, we don't spam another one.
        // Actually, if we use a different unique name, they can run in parallel?
        // Let's use a unique name "launch_sync" with KEEP. 
        // If "launch_sync" is already running (unlikely unless we just launched), it keeps the old one.
        val oneTimeSyncRequest = androidx.work.OneTimeWorkRequest.Builder(
            com.reelworthy.workers.SyncWorker::class.java
        )
        .setConstraints(constraints)
        .build()
        
        androidx.work.WorkManager.getInstance(this).enqueueUniqueWork(
            "launch_sync",
            androidx.work.ExistingWorkPolicy.KEEP, // Don't replace if somehow running
            oneTimeSyncRequest
        )
    }
}
