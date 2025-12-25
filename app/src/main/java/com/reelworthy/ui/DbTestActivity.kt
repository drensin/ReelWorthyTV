package com.reelworthy.ui

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.lifecycle.lifecycleScope
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import androidx.compose.ui.Alignment
import com.reelworthy.data.AppDatabase
import com.reelworthy.data.VideoEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * A temporary testing activity for verifying Database and Network operations.
 *
 * This activity provides a simple UI button to trigger a manual fetch-and-save operation
 * for a specific YouTube video ID. It logs the result to Logcat and displays a Toast.
 *
 * Useful for debugging basic connectivity and Room persistence without the full UI.
 */
class DbTestActivity : ComponentActivity() {

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val db = AppDatabase.getDatabase(this)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Button(onClick = { 
                            lifecycleScope.launch {
                                try {
                                    val repo = com.reelworthy.data.VideoRepository(db.videoDao())
                                    // Use a known public video ID (e.g., Google I/O intro)
                                    // And the API Key from google-services.json
                                    repo.fetchAndSaveVideo("M7fiCoo7e7U", com.reelworthy.BuildConfig.YOUTUBE_API_KEY)
                                    
                                    val count = db.videoDao().getAllVideos().first().size
                                    
                                    val message = "Net+DB Success! Count: $count"
                                    Log.d("DbTestActivity", message)
                                    Toast.makeText(this@DbTestActivity, message, Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Log.e("DbTestActivity", "Error", e)
                                    Toast.makeText(this@DbTestActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }) {
                            Text("Test Network & DB")
                        }
                    }
                }
            }
        }
    }
}
