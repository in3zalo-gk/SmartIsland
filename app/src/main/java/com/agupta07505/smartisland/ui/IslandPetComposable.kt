/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL v3 License Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.model.PetMood
import kotlinx.coroutines.delay

/**
 * A cute square virtual pet that lives inside or near the Dynamic Island.
 * It reacts to system events with different facial expressions and animations.
 * Can be dragged around and tapped for interactions.
 */
@Composable
fun IslandPetComposable(
    mood: PetMood,
    petColor: Long,
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {},
    onDrag: (Float, Float) -> Unit = { _, _ -> }
) {
    val baseColor = Color(petColor)
    val infiniteTransition = rememberInfiniteTransition(label = "petAnimations")

    // Breathing animation (idle bob)
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    // Bounce animation for excited states
    val bounceY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (mood == PetMood.Excited || mood == PetMood.Happy) -3f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    // Rotation for installing
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (mood == PetMood.Installing) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Blink animation
    var isBlinking by remember { mutableStateOf(false) }
    LaunchedEffect(mood) {
        while (true) {
            delay((2000L..5000L).random())
            isBlinking = true
            delay(150L)
            isBlinking = false
        }
    }

    // Sleep Z animation
    val sleepAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (mood == PetMood.Sleeping) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sleepAlpha"
    )

    val animatedScale = when (mood) {
        PetMood.Tapped -> 0.85f
        PetMood.Dragging -> 1.1f
        PetMood.Excited -> breatheScale * 1.1f
        else -> breatheScale
    }

    Box(
        modifier = modifier
            .size(28.dp)
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                translationY = bounceY
                rotationZ = rotation
            }
            .clip(RoundedCornerShape(8.dp))
            .background(baseColor)
            .pointerInput(mood) {
                detectTapGestures { onTap() }
            }
            .pointerInput(mood) {
                detectDragGestures(
                    onDragStart = { },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(28.dp)) {
            drawPetFace(mood, isBlinking, sleepAlpha)
        }
    }
}

private fun DrawScope.drawPetFace(
    mood: PetMood,
    isBlinking: Boolean,
    sleepAlpha: Float
) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val eyeY = h * 0.38f
    val eyeSpacing = w * 0.22f
    val eyeRadius = w * 0.07f

    val leftEyeX = cx - eyeSpacing
    val rightEyeX = cx + eyeSpacing

    // Draw eyes based on mood
    when {
        mood == PetMood.Sleeping -> {
            // Closed eyes (lines)
            drawLine(
                color = Color.White,
                start = Offset(leftEyeX - eyeRadius, eyeY),
                end = Offset(leftEyeX + eyeRadius, eyeY),
                strokeWidth = 2f
            )
            drawLine(
                color = Color.White,
                start = Offset(rightEyeX - eyeRadius, eyeY),
                end = Offset(rightEyeX + eyeRadius, eyeY),
                strokeWidth = 2f
            )
        }
        isBlinking -> {
            drawLine(
                color = Color.White,
                start = Offset(leftEyeX - eyeRadius, eyeY),
                end = Offset(leftEyeX + eyeRadius, eyeY),
                strokeWidth = 2f
            )
            drawLine(
                color = Color.White,
                start = Offset(rightEyeX - eyeRadius, eyeY),
                end = Offset(rightEyeX + eyeRadius, eyeY),
                strokeWidth = 2f
            )
        }
        mood == PetMood.Excited || mood == PetMood.Happy -> {
            // Star eyes (^ ^)
            drawStarEye(leftEyeX, eyeY, eyeRadius)
            drawStarEye(rightEyeX, eyeY, eyeRadius)
        }
        mood == PetMood.Installing -> {
            // Spiral/loading eyes (small circles with dots)
            drawCircle(color = Color.White, radius = eyeRadius, center = Offset(leftEyeX, eyeY))
            drawCircle(color = Color.White, radius = eyeRadius, center = Offset(rightEyeX, eyeY))
            drawCircle(color = baseColorForPet(), radius = eyeRadius * 0.4f, center = Offset(leftEyeX, eyeY))
            drawCircle(color = baseColorForPet(), radius = eyeRadius * 0.4f, center = Offset(rightEyeX, eyeY))
        }
        mood == PetMood.Music -> {
            // Closed happy eyes for music
            drawArc(
                color = Color.White,
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(leftEyeX - eyeRadius, eyeY - eyeRadius * 0.7f),
                size = Size(eyeRadius * 2, eyeRadius * 1.4f),
                style = Stroke(width = 2f)
            )
            drawArc(
                color = Color.White,
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(rightEyeX - eyeRadius, eyeY - eyeRadius * 0.7f),
                size = Size(eyeRadius * 2, eyeRadius * 1.4f),
                style = Stroke(width = 2f)
            )
        }
        else -> {
            // Normal round eyes
            drawCircle(color = Color.White, radius = eyeRadius, center = Offset(leftEyeX, eyeY))
            drawCircle(color = Color.White, radius = eyeRadius, center = Offset(rightEyeX, eyeY))
            drawCircle(color = Color(0xFF1A1A2E), radius = eyeRadius * 0.55f, center = Offset(leftEyeX, eyeY))
            drawCircle(color = Color(0xFF1A1A2E), radius = eyeRadius * 0.55f, center = Offset(rightEyeX, eyeY))
        }
    }

    // Draw mouth based on mood
    val mouthY = h * 0.62f
    when (mood) {
        PetMood.Happy, PetMood.Excited, PetMood.Music -> {
            // Big smile
            drawArc(
                color = Color.White,
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(cx - w * 0.15f, mouthY - w * 0.08f),
                size = Size(w * 0.3f, w * 0.16f),
                style = Stroke(width = 2f)
            )
        }
        PetMood.Sleeping -> {
            // Small O mouth
            drawCircle(
                color = Color.White,
                radius = w * 0.04f,
                center = Offset(cx, mouthY),
                style = Stroke(width = 1.5f)
            )
        }
        PetMood.Installing -> {
            // Focused straight line
            drawLine(
                color = Color.White,
                start = Offset(cx - w * 0.08f, mouthY),
                end = Offset(cx + w * 0.08f, mouthY),
                strokeWidth = 2f
            )
        }
        PetMood.Tapped -> {
            // Small surprised O
            drawCircle(color = Color.White, radius = w * 0.05f, center = Offset(cx, mouthY))
        }
        PetMood.Dragging -> {
            // Wavy mouth
            drawLine(
                color = Color.White,
                start = Offset(cx - w * 0.1f, mouthY),
                end = Offset(cx + w * 0.1f, mouthY),
                strokeWidth = 2f
            )
        }
        PetMood.LowBattery -> {
            // Sad mouth (frown)
            drawArc(
                color = Color.White,
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(cx - w * 0.12f, mouthY),
                size = Size(w * 0.24f, w * 0.12f),
                style = Stroke(width = 2f)
            )
        }
        else -> {
            // Neutral small smile
            drawArc(
                color = Color.White,
                startAngle = 30f,
                sweepAngle = 120f,
                useCenter = false,
                topLeft = Offset(cx - w * 0.1f, mouthY - w * 0.04f),
                size = Size(w * 0.2f, w * 0.08f),
                style = Stroke(width = 1.8f)
            )
        }
    }

    // Sleep Z's
    if (sleepAlpha > 0.1f) {
        drawLine(
            color = Color.White.copy(alpha = sleepAlpha),
            start = Offset(w * 0.7f, h * 0.15f),
            end = Offset(w * 0.85f, h * 0.15f),
            strokeWidth = 1.5f
        )
        drawLine(
            color = Color.White.copy(alpha = sleepAlpha * 0.7f),
            start = Offset(w * 0.72f, h * 0.08f),
            end = Offset(w * 0.82f, h * 0.08f),
            strokeWidth = 1.2f
        )
    }
}

private fun DrawScope.drawStarEye(cx: Float, cy: Float, radius: Float) {
    val path = Path().apply {
        val outerR = radius * 1.2f
        val innerR = radius * 0.5f
        for (i in 0 until 10) {
            val angle = Math.PI * 2 * i / 10 - Math.PI / 2
            val r = if (i % 2 == 0) outerR else innerR
            val x = cx + (r * kotlin.math.cos(angle)).toFloat()
            val y = cy + (r * kotlin.math.sin(angle)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path, color = Color.White)
}

private fun baseColorForPet(): Color = Color(0xFF1A1A2E)
