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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.model.PetMood
import kotlinx.coroutines.delay

/**
 * Small animated pet used by both the preview and the live overlay. Gesture
 * callbacks are lifecycle-aware so callers can always clear Dragging when a
 * pointer is cancelled by the window manager.
 */
@Composable
fun IslandPetComposable(
    mood: PetMood,
    petColor: Long,
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {},
    onDrag: (Float, Float) -> Unit = { _, _ -> },
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    onMotionChanged: (PetMood) -> Unit = {}
) {
    val currentTap by rememberUpdatedState(onTap)
    val currentDrag by rememberUpdatedState(onDrag)
    val currentDragStart by rememberUpdatedState(onDragStart)
    val currentDragEnd by rememberUpdatedState(onDragEnd)
    val currentMotionChanged by rememberUpdatedState(onMotionChanged)
    val transition = rememberInfiniteTransition(label = "petMotion")
    val breathing by transition.animateFloat(
        initialValue = 0.96f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "petBreathing"
    )
    val bounce by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (mood == PetMood.Excited || mood == PetMood.Happy) -3f else 0f,
        animationSpec = infiniteRepeatable(tween(560, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "petBounce"
    )
    val rotation by transition.animateFloat(
        initialValue = 0f, targetValue = if (mood == PetMood.Installing) 360f else 0f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "petRotation"
    )
    val sleepAlpha by transition.animateFloat(
        initialValue = 0f, targetValue = if (mood == PetMood.Sleeping) 1f else 0f,
        animationSpec = infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "petSleep"
    )
    var blinking by remember { mutableStateOf(false) }

    // Observer keeps overlay state in sync even when a system mood changes
    // without pointer input (battery, music, calls, installation).
    LaunchedEffect(mood) { currentMotionChanged(mood) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            blinking = true
            delay(130)
            blinking = false
        }
    }

    val scale = when (mood) {
        PetMood.Tapped -> .86f
        PetMood.Dragging -> 1.1f
        PetMood.Excited -> breathing * 1.08f
        else -> breathing
    }
    val description = stringResource(R.string.pet_content_description)
    Box(
        modifier = modifier
            .size(28.dp)
            .semantics { contentDescription = description }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = bounce
                rotationZ = rotation
            }
            .clip(RoundedCornerShape(8.dp))
            .background(Color(petColor))
            .pointerInput(Unit) { detectTapGestures(onTap = { currentTap() }) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { currentDragStart() },
                    onDragCancel = { currentDragEnd() },
                    onDragEnd = { currentDragEnd() },
                    onDrag = { change, amount ->
                        change.consume()
                        currentDrag(amount.x, amount.y)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(28.dp)) {
            val w = size.width
            val h = size.height
            val eyeY = h * .38f
            val left = w * .30f
            val right = w * .70f
            val eye = w * .07f
            val white = Color.White
            if (mood == PetMood.Sleeping || blinking) {
                drawLine(white, Offset(left - eye, eyeY), Offset(left + eye, eyeY), 2f)
                drawLine(white, Offset(right - eye, eyeY), Offset(right + eye, eyeY), 2f)
            } else if (mood == PetMood.Excited || mood == PetMood.Happy) {
                drawCircle(white, eye * 1.25f, Offset(left, eyeY))
                drawCircle(white, eye * 1.25f, Offset(right, eyeY))
            } else {
                drawCircle(white, eye, Offset(left, eyeY))
                drawCircle(white, eye, Offset(right, eyeY))
                drawCircle(Color(0xFF1A1A2E), eye * .48f, Offset(left, eyeY))
                drawCircle(Color(0xFF1A1A2E), eye * .48f, Offset(right, eyeY))
            }
            val mouthY = h * .64f
            when (mood) {
                PetMood.LowBattery -> drawArc(white, 200f, 140f, false, Offset(w*.38f, mouthY), androidx.compose.ui.geometry.Size(w*.24f, w*.12f), Stroke(2f))
                PetMood.Tapped -> drawCircle(white, w*.05f, Offset(w/2, mouthY))
                PetMood.Installing -> drawLine(white, Offset(w*.42f, mouthY), Offset(w*.58f, mouthY), 2f)
                else -> drawArc(white, 25f, 130f, false, Offset(w*.38f, mouthY-w*.05f), androidx.compose.ui.geometry.Size(w*.24f, w*.13f), Stroke(2f))
            }
            if (sleepAlpha > .05f) drawLine(white.copy(alpha = sleepAlpha), Offset(w*.72f,h*.14f), Offset(w*.88f,h*.14f), 1.5f)
        }
    }
}
