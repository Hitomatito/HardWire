package com.hitomatito.hardwire.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp

/**
 * Three pulsing dots for modern loading indication.
 */
@Composable
fun LoadingDots(
    modifier: Modifier = Modifier,
    dotSize: Int = 8,
    spacing: Int = 6,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val delay = index * 150
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = delay, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$index"
            )
            Box(
                modifier = Modifier
                    .size(dotSize.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.3f + scale * 0.7f))
            )
        }
    }
}

/**
 * Shimmer placeholder for skeleton loading states.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    width: androidx.compose.ui.unit.Dp = 200.dp,
    height: androidx.compose.ui.unit.Dp = 16.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    Box(
        modifier = modifier
            .size(width, height)
            .clip(MaterialTheme.shapes.small)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = 0.5f + (shimmerOffset * 0.5f)
                )
            )
    )
}

/**
 * Animated section that expands/collapses with smooth transition.
 */
@Composable
fun AnimatedSection(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) + fadeIn(
            animationSpec = tween(300)
        ),
        exit = shrinkVertically(
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) + fadeOut(
            animationSpec = tween(300)
        ),
        modifier = modifier.animateContentSize()
    ) {
        content()
    }
}
