package com.reelworthy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.tv.material3.Button
import androidx.tv.material3.Checkbox
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.RadioButton
import androidx.tv.material3.Switch
import androidx.tv.material3.Text

/**
 * The Settings screen overlay.
 *
 * Allows the user to:
 * - Configure the AI Model (e.g., Gemini 1.5 Flash vs Pro).
 * - Toggle "Deep Thinking" mode (affects temperature/reasoning).
 * - Select which YouTube Playlists to use as context for recommendations.
 * - View sync status for background data fetching.
 *
 * @param viewModel The [SettingsViewModel] managing state.
 * @param onClose Callback to close the settings screen.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onClose: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val availableModels by viewModel.availableModels.collectAsState()
    val context = LocalContext.current

    var showModelDialog by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }

    // Toast notification for sync messages
    LaunchedEffect(uiState.syncMessage) {
        uiState.syncMessage?.let { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearSyncMessage()
        }
    }

    // Auto-fetch playlists if empty
    LaunchedEffect(uiState.allPlaylists) {
        if (uiState.allPlaylists.isEmpty()) {
            viewModel.fetchPlaylists()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E)) // Opaque dark gray background
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Settings", style = MaterialTheme.typography.displaySmall, color = Color.White)
                
                if (uiState.isSyncing) {
                   Row(verticalAlignment = Alignment.CenterVertically) {
                       androidx.compose.material3.CircularProgressIndicator(
                           modifier = Modifier.size(24.dp),
                           color = Color.White,
                           strokeWidth = 2.dp
                       )
                       Spacer(modifier = Modifier.width(8.dp))
                       Text("Syncing...", color = Color.LightGray)
                   }
                }

                Button(onClick = onClose) {
                    Text("Close")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Scrollable Content
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                
                // Section: AI Model
                item {
                    Column {
                        Text("AI Model", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = { showModelDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Selected Model", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                                    Text(uiState.settings.aiModel, style = MaterialTheme.typography.bodyLarge)
                                }
                                Text("Change >", color = Color.LightGray)
                            }
                        }
                    }
                }

                // Section: Deep Thinking
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.onDeepThinkingChanged(!uiState.settings.isDeepThinkingEnabled) }
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Deep Thinking", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text(
                                "Enable slower, more detailed AI analysis",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.LightGray
                            )
                        }
                        Switch(
                            checked = uiState.settings.isDeepThinkingEnabled,
                            onCheckedChange = { viewModel.onDeepThinkingChanged(it) }
                        )
                    }
                }

                // Section: Playlists
                item {
                    Column {
                        Text("Context Sources", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                         Button(
                            onClick = { showPlaylistDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Active Playlists", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                                    val count = uiState.settings.selectedPlaylistIds.size
                                    Text("$count playlists selected", style = MaterialTheme.typography.bodyLarge)
                                }
                                Text("Edit >", color = Color.LightGray)
                            }
                        }
                        }
                    }


                // Section: Subscription Feed
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.onSubscriptionFeedChanged(!uiState.settings.includeSubscriptionFeed) }
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Recent from Subscriptions", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text(
                                "Include recent videos from your subscribed channels",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.LightGray
                            )
                        }
                        Switch(
                            checked = uiState.settings.includeSubscriptionFeed,
                            onCheckedChange = { viewModel.onSubscriptionFeedChanged(it) }
                        )
                    }
                }
            } // End LazyColumn
        } // End Column
        
        if (showModelDialog) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { showModelDialog = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .background(Color(0xFF2B2B2B), shape = MaterialTheme.shapes.medium)
                        .padding(24.dp)
                ) {
                    Column {
                        Text("Select AI Model", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        LazyColumn(modifier = Modifier.height(300.dp)) {
                             if (availableModels.isEmpty()) {
                                 item { Text("Loading models...", color = Color.Gray) }
                            } else {
                                items(availableModels) { modelId ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { 
                                                viewModel.onModelSelected(modelId)
                                                showModelDialog = false
                                            }
                                            .padding(vertical = 8.dp)
                                    ) {
                                        RadioButton(
                                            selected = uiState.settings.aiModel == modelId,
                                            onClick = { 
                                                viewModel.onModelSelected(modelId)
                                                showModelDialog = false
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(text = modelId, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showModelDialog = false },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }

        if (showPlaylistDialog) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { showPlaylistDialog = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .fillMaxSize(0.8f) // Taller dialog for playlists
                        .background(Color(0xFF2B2B2B), shape = MaterialTheme.shapes.medium)
                        .padding(24.dp)
                ) {
                    Column {
                        Text("Select Playlists", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                        Text("Select which playlists the AI can use for context.", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (uiState.allPlaylists.isEmpty()) {
                             Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp), 
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Fetching playlists...", color = Color.White) 
                            }
                        } else {
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                items(uiState.allPlaylists) { playlist ->
                                    val isSelected = uiState.settings.selectedPlaylistIds.contains(playlist.id)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.onPlaylistSelectionChanged(playlist.id, !isSelected) }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { viewModel.onPlaylistSelectionChanged(playlist.id, it) }
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = playlist.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.White
                                            )
                                             Text(
                                                text = "${playlist.itemCount} videos",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showPlaylistDialog = false },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Done")
                        }
                    }
                }
            }
        }
    }
}

