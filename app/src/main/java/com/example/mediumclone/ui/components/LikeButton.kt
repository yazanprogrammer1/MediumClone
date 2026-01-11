package com.example.mediumclone.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun LikeButton(
    isLiked: Boolean,
    likesCount: Int,
    onLikeClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    showCount: Boolean = true
) {
    var animationPlayed by remember { mutableStateOf(false) }
    
    // Scale animation for the heart
    val scale by animateFloatAsState(
        targetValue = if (animationPlayed && isLiked) 1.3f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        finishedListener = {
            if (animationPlayed) {
                animationPlayed = false
            }
        }
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            onClick = {
                if (!isLoading) {
                    animationPlayed = true
                    onLikeClick()
                }
            },
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (isLiked) "Unlike" else "Like",
                    tint = if (isLiked) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.graphicsLayer(
                        scaleX = scale,
                        scaleY = scale
                    )
                )
            }
        }

        if (showCount) {
            Text(
                text = when {
                    likesCount >= 1000 -> "${likesCount / 1000}K"
                    else -> likesCount.toString()
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isLiked) FontWeight.Bold else FontWeight.Normal,
                color = if (isLiked) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
