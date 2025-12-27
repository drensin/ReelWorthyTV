package com.reelworthy.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.reelworthy.data.SearchHistoryEntity
import com.reelworthy.util.TimeUtils
import kotlinx.coroutines.delay

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
    val streamingText by chatViewModel.currentStreamingText.collectAsState()
    val searchHistory by chatViewModel.searchHistory.collectAsState()

    var showSearchInput by remember { mutableStateOf(false) }
    var showHistoryModal by remember { mutableStateOf(false) }
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

    // Auto-focus logic for Results
    LaunchedEffect(isLoading) {
        if (!isLoading && messages.lastOrNull()?.recommendations?.isNotEmpty() == true) {
            carouselFocusRequester.requestFocus()
        }
    }

    val lastAiMessage = messages.lastOrNull { !it.isUser }
    val recommendations = lastAiMessage?.recommendations ?: emptyList()

    // Immersive Background Gradient
    Box(
            modifier =
                    Modifier.fillMaxSize()
                            .background(
                                    brush =
                                            Brush.verticalGradient(
                                                    colors =
                                                            listOf(
                                                                    Color(
                                                                            0xFF1A1A1A
                                                                    ), // Dark Grey Top
                                                                    Color.Black, // Black Center
                                                                    Color(
                                                                            0xFF0D1117
                                                                    ) // Deep Blue/Black Bottom
                                                            )
                                            )
                            )
    ) {
        // Main Content Layer
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar (Icons)
            Row(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
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
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 2. Recommendations Carousel OR Text Response
                if (recommendations.isNotEmpty()) {
                    Text(
                            text = "${recommendations.size} RECOMMENDATIONS FOUND FOR YOU",
                            style =
                                    MaterialTheme.typography.labelSmall.copy(
                                            letterSpacing = 1.5.sp
                                    ),
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 24.dp),
                            modifier =
                                    Modifier.fillMaxWidth()
                                            .weight(
                                                    1f
                                            ) // Fill available vertical space in the parent Column
                                            .focusRequester(carouselFocusRequester)
                    ) {
                        items(recommendations) { rec ->
                            val context = LocalContext.current
                            DashboardVideoCard(
                                    rec = rec,
                                    onPlayClick = { videoId ->
                                        val intent =
                                                android.content.Intent(
                                                        android.content.Intent.ACTION_VIEW,
                                                        android.net.Uri.parse(
                                                                "https://www.youtube.com/watch?v=$videoId"
                                                        )
                                                )
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            android.util.Log.e(
                                                    "DashboardScreen",
                                                    "Error launching video",
                                                    e
                                            )
                                        }
                                    }
                            )
                        }
                    }
                } else if (lastAiMessage != null && !isLoading) {
                    // AI responded, but no videos found (or error)
                    Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize().padding(32.dp)
                    ) {
                        Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                                text = lastAiMessage.text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White,
                                textAlign = TextAlign.Center
                        )
                    }
                } else if (!isLoading) {
                    // Empty / Welcome State
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                                text = "Welcome back, $userDisplayName",
                                style =
                                        MaterialTheme.typography.headlineLarge.copy(
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
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 56.dp, vertical = 32.dp)
            ) {
                if (showSearchInput) {
                    TextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { androidx.compose.material3.Text("Search...") },
                            modifier =
                                    Modifier.fillMaxWidth(0.6f)
                                            .focusRequester(searchFocusRequester)
                                            .border(
                                                    1.dp,
                                                    Color.White.copy(alpha = 0.3f),
                                                    RoundedCornerShape(8.dp)
                                            ),
                            colors =
                                    TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Black.copy(alpha = 0.8f),
                                            unfocusedContainerColor =
                                                    Color.Black.copy(alpha = 0.5f),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                    ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions =
                                    KeyboardActions(
                                            onSearch = {
                                                if (inputText.isNotBlank()) {
                                                    chatViewModel.sendMessage(inputText)
                                                    inputText = ""
                                                    showSearchInput = false
                                                }
                                            }
                                    )
                    )
                } else {
                    // Quick Prompts + Search Icon + History Icon
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

                        // History Trigger
                        FocusableIcon(
                                icon = Icons.Default.History,
                                contentDescription = "History",
                                onClick = { showHistoryModal = true }
                        )

                        // Prompts
                        val prompts =
                                listOf(
                                        "Something funny",
                                        "Coding tutorials",
                                        "Music",
                                        "Surprise me"
                                )
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
        ) { ThinkingModal(statusText = streamingText) }

        // Search History Modal
        if (showHistoryModal) {
            Dialog(onDismissRequest = { showHistoryModal = false }) {
                SearchHistoryModal(
                        history = searchHistory,
                        onClose = { showHistoryModal = false },
                        onSelectQuery = { query ->
                            chatViewModel.sendMessage(query)
                            showHistoryModal = false
                        },
                        onDeleteQuery = { query -> chatViewModel.deleteSearchHistoryItem(query) }
                )
            }
        }
    }
}

/**
 * A modal dialog that displays user's search history. Supports click to select and long-press
 * (Center Button) to delete.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SearchHistoryModal(
        history: List<SearchHistoryEntity>,
        onClose: () -> Unit,
        onSelectQuery: (String) -> Unit,
        onDeleteQuery: (String) -> Unit
) {
    Box(
            modifier =
                    Modifier.fillMaxWidth(0.8f) // Adjusted for Dialog container
                            .fillMaxHeight(0.8f)
                            .background(Color(0xFF1E1E1E), RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .padding(32.dp)
    ) {
        Column {
            Text(
                    text = "Search History",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
            )
            Text(
                    text = "Hold Center Button to delete an item.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (history.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No recent searches.", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(history) { item ->
                        HistoryItem(
                                query = item.query,
                                onSelect = { onSelectQuery(item.query) },
                                onDelete = { onDeleteQuery(item.query) }
                        )
                    }
                }
            }
        }

        // Close Button Top-Right
        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd)) {
            Icon(Icons.Default.ExitToApp, contentDescription = "Close", tint = Color.White)
        }
    }
}

/**
 * Represents a single row in the Search History list.
 *
 * @param query The search query string.
 * @param onSelect Callback when the item is clicked/selected.
 * @param onDelete Callback when the item is to be deleted (confirmed via dialog).
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HistoryItem(query: String, onSelect: () -> Unit, onDelete: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        // Prevent accidental click from the "Long Press Release" event
        var isInputReady by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(250) // Reduced buffer
            isInputReady = true
        }

        // Nested Dialog for Delete Confirmation
        Dialog(onDismissRequest = { showDeleteDialog = false }) {
            Box(
                    modifier =
                            Modifier.background(Color(0xFF2B2B2B), RoundedCornerShape(8.dp))
                                    .padding(24.dp)
            ) {
                Column {
                    Text(
                            "Delete this search?",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                    )
                    Text(
                            "\"$query\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                    ) {
                        // Cancel Button
                        FocusableScaleWrapper(
                                onClick = { if (isInputReady) showDeleteDialog = false },
                                modifier = Modifier.height(40.dp)
                        ) { isFocused ->
                            Box(
                                    modifier =
                                            Modifier.background(
                                                            if (isFocused)
                                                                    Color.White.copy(alpha = 0.1f)
                                                            else Color.Transparent,
                                                            RoundedCornerShape(12.dp)
                                                    )
                                                    .border(
                                                            1.dp,
                                                            if (isFocused) Color.White
                                                            else Color.Gray,
                                                            RoundedCornerShape(12.dp)
                                                    )
                                                    .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                            ) { Text("Cancel", color = Color.White) }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Delete Button
                        FocusableScaleWrapper(
                                onClick = {
                                    if (isInputReady) {
                                        onDelete()
                                        showDeleteDialog = false
                                    }
                                },
                                modifier = Modifier.height(40.dp)
                        ) { isFocused ->
                            val backgroundColor =
                                    if (isFocused) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                            Box(
                                    modifier =
                                            Modifier.background(
                                                            backgroundColor,
                                                            RoundedCornerShape(12.dp)
                                                    )
                                                    .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                            ) { Text("Delete", color = Color.White, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
    }

    FocusableScaleWrapper(
            onClick = onSelect,
            onLongClick = { showDeleteDialog = true },
            modifier = Modifier.fillMaxWidth()
    ) { isFocused ->
        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .background(
                                        if (isFocused) Color(0xFF333333) else Color(0xFF252525),
                                        RoundedCornerShape(8.dp)
                                )
                                .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = if (isFocused) Color.White else Color.Gray,
                    modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))

            // Marquee Logic: Loop with speed adjustment
            val scrollState = rememberScrollState()
            LaunchedEffect(isFocused) {
                if (isFocused) {
                    delay(1000) // Initial delay to read start
                    while (true) {
                        val maxScroll = scrollState.maxValue
                        if (maxScroll > 0) {
                            // Calculate duration based on width. ~30 pixels per second sounds
                            // readable.
                            // Ensure a minimum duration of 2 seconds.
                            val duration = (maxScroll * 20).coerceAtLeast(2000)

                            scrollState.animateScrollTo(
                                    value = maxScroll,
                                    animationSpec =
                                            androidx.compose.animation.core.tween(
                                                    durationMillis = duration,
                                                    easing =
                                                            androidx.compose.animation.core
                                                                    .LinearEasing
                                            )
                            )
                            delay(2000) // Pause at end
                            scrollState.scrollTo(0) // Snap back
                            delay(2000) // Pause at start
                        } else {
                            break // Content fits, no scroll needed
                        }
                    }
                } else {
                    scrollState.scrollTo(0)
                }
            }

            Text(
                    text = query,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isFocused) Color.White else Color.LightGray,
                    modifier = Modifier.horizontalScroll(scrollState)
            )
        }
    }
}

/**
 * A modal dialog that appears while the AI is thinking. It streams the "consciousness" of the AI to
 * the user to reduce perceived latency.
 *
 * @param statusText The real-time text stream from the AI (thinking process).
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ThinkingModal(statusText: String) {
    Box(
            modifier =
                    Modifier.fillMaxSize() // Cover full screen to capture focus/clicks? Or just
                            // overlay visual.
                            .background(Color.Black.copy(alpha = 0.8f))
                            .focusable(
                                    false
                            ), // Pass through focus? No, we want to block interaction
            contentAlignment = Alignment.Center
    ) {
        Box(
                modifier =
                        Modifier.fillMaxWidth(0.7f)
                                .fillMaxHeight(0.7f)
                                .background(
                                        color = Color(0xFF1E1E1E),
                                        shape = RoundedCornerShape(24.dp)
                                )
                                .border(
                                        width = 1.dp,
                                        brush =
                                                Brush.verticalGradient(
                                                        listOf(
                                                                Color.White.copy(alpha = 0.3f),
                                                                Color.Transparent
                                                        )
                                                ),
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

                // Teleprompter Logic: Continuous smooth scroll
                // This decouples the scroll position from the network speed, ensuring a readable
                // pace.
                LaunchedEffect(Unit) {
                    while (true) {
                        val current = scrollState.value
                        val max = scrollState.maxValue

                        if (current < max) {
                            // Scroll speed: ~2 pixels per frame (at 60fps)
                            // This provides a smooth "movie computer" feel
                            scrollState.scrollTo(current + 2)
                        }

                        // Wait for next frame (approx 16ms for 60fps)
                        delay(16)
                    }
                }

                Text(
                        text = statusText,
                        style =
                                MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Light,
                                        lineHeight = 28.sp
                                ),
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(scrollState)
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
fun DashboardVideoCard(rec: RecommendedVideo, onPlayClick: (String) -> Unit) {
    val durationText =
            remember(rec.video.duration) { TimeUtils.formatIsoDuration(rec.video.duration) }

    var isRevealed by remember { mutableStateOf(false) }
    var isFocusedState by remember { mutableStateOf(false) }

    FocusableScaleWrapper(
            onClick = {
                // If D-pad focused OR already revealed (via touch), play.
                // Otherwise (first touch), reveal details.
                if (isFocusedState || isRevealed) {
                    onPlayClick(rec.video.id)
                } else {
                    isRevealed = true
                }
            },
            modifier =
                    Modifier.width(320.dp) // Increased width
                            .fillMaxHeight() // Fill the LazyRow's height
    ) { isFocused ->

        // Capture focus state for the click handler
        SideEffect { isFocusedState = isFocused }

        val showDetails = isFocused || isRevealed

        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .background(
                                        if (showDetails) Color(0xFF2A2A2A) else Color(0xFF1A1A1A)
                                )
        ) {
            // Thumbnail Container with Duration Overlay
            Box(modifier = Modifier.fillMaxWidth().weight(0.5f)) {
                AsyncImage(
                        model = rec.video.thumbnailUrl,
                        contentDescription = rec.video.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().background(Color.Gray)
                )

                // Duration Badge
                if (durationText != null) {
                    Box(
                            modifier =
                                    Modifier.align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .background(
                                                    Color.Black.copy(alpha = 0.8f),
                                                    RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                                text = durationText,
                                style =
                                        MaterialTheme.typography.labelSmall.copy(
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
                if (showDetails) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Auto-scrolling Reasoning Text
                    val scrollState = rememberScrollState()

                    LaunchedEffect(Unit) {
                        while (true) {
                            scrollState.scrollTo(0)
                            delay(2000)
                            if (scrollState.maxValue > 0) {
                                scrollState.animateScrollTo(
                                        scrollState.maxValue,
                                        animationSpec =
                                                androidx.compose.animation.core.tween(
                                                        durationMillis =
                                                                (scrollState.maxValue * 15)
                                                                        .coerceAtLeast(1000),
                                                        easing =
                                                                androidx.compose.animation.core
                                                                        .LinearEasing
                                                )
                                )
                                delay(2000)
                            } else {
                                break // Text fits, no scroll needed
                            }
                        }
                    }

                    Text(
                            text = rec.reason,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.verticalScroll(scrollState)
                    )
                }
            }
        }
    }
}
