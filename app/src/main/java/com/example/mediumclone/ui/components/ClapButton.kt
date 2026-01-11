package com.example.mediumclone.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FrontHand
import androidx.compose.material.icons.outlined.FrontHand
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material3.*
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ClapButton(
    clapCount: Int, 
    isClappedByMe: Boolean, 
    onClap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )
    
    // Reaction State
    // Default to ThumbUp if no specific reaction selected, but if we have local state, use it.
    // If isClappedByMe is false, we reset to ThumbUp visually for the next like, 
    // or keep the last one. Let's reset to ThumbUp when not liked to be clean.
    var selectedReaction by remember { mutableStateOf(Icons.Default.ThumbUp) }
    var selectedColor by remember { mutableStateOf(Color(0xFF2196F3)) } // Default Blue
    
    // Reset if not liked (optional, but requested behavior implies persistence WHILE liked)
    LaunchedEffect(isClappedByMe) {
        if (!isClappedByMe) {
            selectedReaction = Icons.Default.ThumbUp
            selectedColor = Color(0xFF2196F3)
        }
    }

    // Reaction Menu State
    var showReactions by remember { mutableStateOf(false) }
    
    // For showing the "+1" bubbles
    val clapsScope = rememberCoroutineScope()
    var floatingClaps by remember { mutableStateOf(listOf<Long>()) }

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        // Floating Bubbles
        floatingClaps.forEach { id ->
            FloatingClapBubble(id)
        }

        // Luxurious Reaction Menu
        MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(24.dp))) {
            DropdownMenu(
                expanded = showReactions,
                onDismissRequest = { showReactions = false },
                offset = androidx.compose.ui.unit.DpOffset(x = 0.dp, y = (-60).dp),
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp), RoundedCornerShape(24.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    ReactionOption(Icons.Default.ThumbUp, "Like", Color(0xFF2196F3)) { 
                        selectedReaction = Icons.Default.ThumbUp
                        selectedColor = Color(0xFF2196F3)
                        if (!isClappedByMe) onClap() // Trigger like if not already liked
                        showReactions = false 
                    }
                    ReactionOption(Icons.Default.Favorite, "Love", Color(0xFFE91E63)) { 
                        selectedReaction = Icons.Default.Favorite
                        selectedColor = Color(0xFFE91E63)
                        if (!isClappedByMe) onClap()
                        showReactions = false 
                    }
                    ReactionOption(Icons.Default.Star, "Star", Color(0xFFFFC107)) { 
                        selectedReaction = Icons.Default.Star
                        selectedColor = Color(0xFFFFC107)
                        if (!isClappedByMe) onClap()
                        showReactions = false 
                    }
                    ReactionOption(Icons.Default.SentimentVerySatisfied, "Funny", Color(0xFF4CAF50)) { 
                        selectedReaction = Icons.Default.SentimentVerySatisfied
                        selectedColor = Color(0xFF4CAF50)
                        if (!isClappedByMe) onClap()
                        showReactions = false 
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            showReactions = true
                        },
                        onPress = {
                            scale = 0.8f
                            tryAwaitRelease()
                            scale = 1f
                        },
                        onTap = {
                            scale = 1.2f
                            // If not liked, we like. If liked, we unlike.
                            onClap()
                            
                            if (!isClappedByMe) {
                                // If we are about to like (currently false), show bubble
                                floatingClaps = floatingClaps + System.nanoTime()
                                clapsScope.launch {
                                    delay(100)
                                    scale = 1f
                                }
                            }
                        }
                    )
                }
        ) {
            Icon(
                imageVector = if (isClappedByMe) selectedReaction else Icons.Outlined.ThumbUp,
                contentDescription = "Like",
                tint = if (isClappedByMe) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(26.dp)
                    .scale(animatedScale)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = "$clapCount",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isClappedByMe) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ReactionOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    label: String, 
    tint: Color,
    onClick: () -> Unit
) {
    var hovered by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (hovered) 1.2f else 1f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Icon(
            imageVector = icon, 
            contentDescription = label, 
            tint = tint, 
            modifier = Modifier
                .size(32.dp)
                .scale(scale)
        )
    }
}

@Composable
fun FloatingClapBubble(id: Long) {
    var offsetY by remember { mutableStateOf(0f) }
    var alpha by remember { mutableStateOf(1f) }

    LaunchedEffect(id) {
        animate(
            initialValue = 0f,
            targetValue = -100f,
            animationSpec = tween(800, easing = LinearOutSlowInEasing)
        ) { value, _ ->
            offsetY = value
        }
    }
    
    LaunchedEffect(id) {
        animate(
            initialValue = 1f,
            targetValue = 0f,
            animationSpec = tween(800)
        ) { value, _ ->
            alpha = value
        }
    }

    if (alpha > 0) {
        Text(
            text = "+1",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .offset(y = offsetY.dp)
                .graphicsLayer(alpha = alpha)
        )
    }
}
