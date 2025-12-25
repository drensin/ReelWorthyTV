package com.reelworthy.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.reelworthy.data.VideoEntity

/**
 * The primary screen of the ReelWorthy TV application.
 *
 * This screen handles:
 * - Immersive background gradient.
 * - Top navigation bar (Settings, Sign Out).
 * - "Thinking" overlay when the AI is processing logic.
 * - Video recommendations carousel.
 * - Search input / Quick prompt chips.
 *
 * @param userDisplayName The name of the signed-in user (for welcome message).
 * @param chatViewModel The ViewModel managing chat state and AI interactions.
 * @param onOpenSettings Callback to navigate to Settings.
 * @param onSignOut Callback to sign out.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DashboardScreen(
    userDisplayName: String?,
    chatViewModel: ChatViewModel,
    onOpenSettings: () -> Unit,
    onSignOut: () -> Unit
) {
    val messages by chatViewModel.messages.collectAsState()
    val isLoading by chatViewModel.isLoading.collectAsState()
    val streamingText by chatViewModel.currentStreamingText.collectAsState() // New streaming state
    
    var showSearchInput by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    
    // Focus Requesters
    val searchFocusRequester = remember { FocusRequester() }
    val carouselFocusRequester = remember { FocusRequester() }
    
    // Auto-focus logic for Search
    LaunchedEffect(showSearchInput) {
        if (showSearchInput) {
            searchFocusRequester.requestFocus()
        }
    }
    
    // Auto-focus logic for Results (When loading finishes and we have results)
    LaunchedEffect(isLoading) {
        if (!isLoading && messages.lastOrNull()?.recommendations?.isNotEmpty() == true) {
            carouselFocusRequester.requestFocus()
        }
    }
    
    val lastAiMessage = messages.lastOrNull { !it.isUser }
    // If loading, use streaming text. Otherwise use last message text.
    // Note: While loading, messages list hasn't been updated yet, so lastAiMessage is the OLD one.
    // We only rely on streamingText for the overlay.
    val recommendations = lastAiMessage?.recommendations ?: emptyList()

    // Immersive Background Gradient
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A1A), // Dark Grey Top
                        Color.Black,       // Black Center
                        Color(0xFF0D1117)  // Deep Blue/Black Bottom
                    )
                )
            )
    ) {
        // Main Content Layer
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar (Icons)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                 FocusableIcon(
                    icon = Icons.Default.Settings,
                    contentDescription = "Settings",
                    onClick = onOpenSettings
                )
                Spacer(modifier = Modifier.width(16.dp))
                FocusableIcon(
                    icon = Icons.Default.ExitToApp,
                    contentDescription = "Sign Out",
                    onClick = onSignOut
                )
            }

            // Center Area (Recommendations OR Welcome)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 2. Recommendations Carousel
                if (recommendations.isNotEmpty()) {
                    Text(
                        text = "${recommendations.size} RECOMMENDATIONS FOUND FOR YOU",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 24.dp), 
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f) // Fill available vertical space in the parent Column
                            .focusRequester(carouselFocusRequester)
                    ) {
                        items(recommendations) { rec ->
                            val context = androidx.compose.ui.platform.LocalContext.current
                            DashboardVideoCard(
                                rec = rec,
                                onPlayClick = { videoId ->
                                    val intent = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse("https://www.youtube.com/watch?v=$videoId")
                                    )
                                    // Verify intent resolves to avoid crash? 
                                    // On TV it likely will, but good practice.
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        android.util.Log.e("DashboardScreen", "Error launching video", e)
                                    }
                                }
                            )
                        }
                    }
                } else if (!isLoading) {
                    // Empty / Welcome State
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Welcome back, $userDisplayName",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Thin,
                                letterSpacing = 1.sp
                            ),
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "What would you like to watch today?",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Bottom Bar (Search & Chips)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 56.dp, vertical = 32.dp)
            ) {
                 if (showSearchInput) {
                     androidx.compose.material3.TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { androidx.compose.material3.Text("Search...") },
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .focusRequester(searchFocusRequester)
                            .border(1.dp, Color.White.copy(alpha=0.3f), RoundedCornerShape(8.dp)),
                        colors = androidx.compose.material3.TextFieldDefaults.colors(
                            focusedContainerColor = Color.Black.copy(alpha=0.8f),
                            unfocusedContainerColor = Color.Black.copy(alpha=0.5f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            if (inputText.isNotBlank()) {
                                chatViewModel.sendMessage(inputText)
                                inputText = ""
                                showSearchInput = false
                            }
                        })
                    )
                } else {
                     // Quick Prompts + Search Icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Search Trigger
                         FocusableIcon(
                             icon = Icons.Default.Search,
                             contentDescription = "Search",
                             onClick = { showSearchInput = true }
                         )
                        
                        // Prompts
                        val prompts = listOf("Something funny", "Coding tutorials", "Music", "Surprise me")
                        prompts.forEach { prompt ->
                            FocusableChip(
                                text = prompt,
                                onClick = { chatViewModel.sendMessage(prompt) }
                            )
                        }
                    }
                }
            }
        }
        
        // Modal Overlay (Thinking)
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            ThinkingModal(statusText = streamingText)
        }
    }
}

/**
 * A modal dialog that appears while the AI is thinking.
 * It streams the "consciousness" of the AI to the user to reduce perceived latency.
 *
 * @param statusText The real-time text stream from the AI (thinking process).
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ThinkingModal(statusText: String) {
    Box(
        modifier = Modifier
            .fillMaxSize() // Cover full screen to capture focus/clicks? Or just overlay visual.
            .background(Color.Black.copy(alpha = 0.8f))
            .focusable(false), // Pass through focus? No, we want to block interaction
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .fillMaxHeight(0.7f)
                .background(
                    color = Color(0xFF1E1E1E),
                    shape = RoundedCornerShape(24.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.3f), Color.Transparent)),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(48.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                // CircularProgressIndicator removed for cleaner UI
                Text(
                    text = "Consulting the AI...",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(24.dp))
                // Streaming Text
                val scrollState = rememberScrollState()
                
                // OPTIMIZATION: Use scrollTo instead of animateScrollTo for streaming text
                // ensuring rapid updates don't lag behind animations.
                LaunchedEffect(statusText) {
                    scrollState.scrollTo(scrollState.maxValue)
                }
                
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Light,
                        lineHeight = 28.sp
                    ),
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scrollState)
                )
            }
        }
    }
}

/**
 * A specialized card component for displaying a recommended video.
 *
 * Features:
 * - Large thumbnail with duration badge overlay.
 * - Title and Description.
 * - Auto-scrolling text for the "Reasoning" field when focused.
 *
 * @param rec The recommendation data.
 * @param onPlayClick Callback when the card is clicked.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DashboardVideoCard(
    rec: RecommendedVideo,
    onPlayClick: (String) -> Unit
) {
    val durationText = remember(rec.video.duration) {
        com.reelworthy.util.TimeUtils.formatIsoDuration(rec.video.duration)
    }

    FocusableScaleWrapper(
        onClick = { onPlayClick(rec.video.id) },
        modifier = Modifier
            .width(320.dp) // Increased width
            .fillMaxHeight() // Fill the LazyRow's height (which fills parent weight)
    ) { isFocused ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isFocused) Color(0xFF2A2A2A) else Color(0xFF1A1A1A)
                )
        ) {
             // Thumbnail Container with Duration Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f)
            ) {
                AsyncImage(
                    model = rec.video.thumbnailUrl,
                    contentDescription = rec.video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Gray) 
                )
                
                // Duration Badge
                if (durationText != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = durationText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }
            
            Column(modifier = Modifier.padding(16.dp).weight(0.5f)) {
                Text(
                    text = rec.video.title,
                    maxLines = 2,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                if (isFocused) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Auto-scrolling Reasoning Text
                    val scrollState = rememberScrollState()
                    
                    LaunchedEffect(Unit) {
                        while (true) {
                            scrollState.scrollTo(0)
                            kotlinx.coroutines.delay(2000)
                            // Calculate duration based on text length to keep speed consistent. 
                            // Rough est: 50ms per pixel? or just fixed slow speed.
                            // Max value can be large. 
                            // Let's use a safe fixed duration for now or dynamic.
                            // If max value is 0, it won't scroll.
                            if (scrollState.maxValue > 0) {
                                scrollState.animateScrollTo(
                                    scrollState.maxValue,
                                    animationSpec = androidx.compose.animation.core.tween(
                                        durationMillis = (scrollState.maxValue * 15).coerceAtLeast(1000), // Slower scroll
                                        easing = androidx.compose.animation.core.LinearEasing
                                    )
                                )
                                kotlinx.coroutines.delay(2000)
                            } else {
                                break // Text fits, no scroll needed
                            }
                        }
                    }
                    
                    Text(
                        text = rec.reason,
                        // No maxLines to allow scrolling
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.verticalScroll(scrollState)
                    )
                }
            }
        }
    }
}
