package com.reelworthy.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// "Magic" Constants for Animation
private const val ANIMATION_DURATION = 300
private const val FOCUSED_SCALE = 1.05f
private const val DEFAULT_SCALE = 1.0f

/**
 * A wrapper composable that handles D-pad focus states for TV interfaces.
 *
 * Automatically manages:
 * - Scale animation (grows when focused).
 * - Border highlight (white border when focused).
 * - Shadow elevation (lifts up when focused).
 *
 * Uses manual `onKeyEvent` handling for robust D-pad support (Click & Long Click).
 *
 * @param onClick Callback when the component is clicked (DPAD Center).
 * @param onLongClick Callback for long-press (DPAD Center hold).
 * @param modifier Modifier to be applied to the Surface.
 * @param shape Shape of the component (default: RoundedCornerShape(12.dp)).
 * @param content Composable content to display inside the wrapper. Receiver scope provides `isFocused` boolean.
 */
@Composable
fun FocusableScaleWrapper(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    content: @Composable BoxScope.(Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scope = rememberCoroutineScope()
    var longClickTriggered by remember { mutableStateOf(false) }
    var pressJob by remember { mutableStateOf<Job?>(null) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) FOCUSED_SCALE else DEFAULT_SCALE,
        animationSpec = tween(durationMillis = ANIMATION_DURATION),
        label = "scale"
    )

    val borderStroke = if (isFocused) {
        BorderStroke(2.dp, Color.White.copy(alpha = 0.8f))
    } else {
        BorderStroke(0.dp, Color.Transparent)
    }
    
    // Glow/Shadow effect
    val shadowElevation = if (isFocused) 12.dp else 2.dp

    Surface(
        modifier = modifier
            .scale(scale)
            .onKeyEvent { event ->
                if (event.key == Key.DirectionCenter || 
                    event.key == Key.Enter ||
                    event.key == Key.NumPadEnter) {
                     if (event.type == KeyEventType.KeyDown) {
                         if (event.nativeKeyEvent.repeatCount == 0) {
                             // Start timer for Long Press
                             longClickTriggered = false
                             pressJob?.cancel()
                             pressJob = scope.launch {
                                 delay(500) // Long press threshold: 500ms
                                 longClickTriggered = true
                                 onLongClick?.invoke()
                             }
                         }
                         true // Consume event
                     } else if (event.type == KeyEventType.KeyUp) {
                         // Cancel timer. If not yet triggered, treat as Click.
                         pressJob?.cancel()
                         if (!longClickTriggered) {
                             onClick()
                         }
                         true // Consume event
                     } else {
                         false
                     }
                } else {
                    false
                }
            }
            .focusable(interactionSource = interactionSource)
            .indication(
                interactionSource = interactionSource,
                indication = rememberRipple()
            ),
        shape = shape,
        border = borderStroke,
        shadowElevation = shadowElevation,
        color = Color.Transparent, // Let content describe background
    ) {
        Box(contentAlignment = Alignment.Center) {
            content(isFocused)
        }
    }
}

/**
 * A pill-shaped chip/button that highlights when focused.
 * Used for quick prompt suggestions.
 *
 * @param text The label to display.
 * @param onClick Action to perform on click.
 */
@Composable
fun FocusableChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FocusableScaleWrapper(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50) // Pill shape
    ) { isFocused ->
        val backgroundColor by animateColorAsState(
            targetValue = if (isFocused) Color.White else Color.White.copy(alpha = 0.1f),
            animationSpec = tween(ANIMATION_DURATION),
            label = "bgColor"
        )
        val textColor by animateColorAsState(
            targetValue = if (isFocused) Color.Black else Color.White,
            animationSpec = tween(ANIMATION_DURATION),
            label = "textColor"
        )

        Box(
            modifier = Modifier
                .background(backgroundColor)
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = text,
                color = textColor,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
            )
        }
    }
}

/**
 * A circular icon button that highlights when focused.
 * Used for top-bar actions (Settings, Sign Out, Search).
 *
 * @param icon The vector icon to display.
 * @param onClick Action to perform on click.
 * @param contentDescription Accessibility description.
 */
@Composable
fun FocusableIcon(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    FocusableScaleWrapper(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50) // Circle
    ) { isFocused ->
        val tint by animateColorAsState(
            targetValue = if (isFocused) Color.Black else Color.White.copy(alpha = 0.8f),
            label = "iconTint"
        )
        val background by animateColorAsState(
            targetValue = if (isFocused) Color.White else Color.Transparent,
            label = "iconBg"
        )
        
        Box(
            modifier = Modifier
                .background(background)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint
            )
        }
    }
}
