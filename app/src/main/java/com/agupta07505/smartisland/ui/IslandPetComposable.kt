/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL v3 License Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.model.PetMood
import kotlinx.coroutines.delay

/** Friendly, rounded companion. It is an original Smart Island illustration, not a Toqi logo. */
@Composable
fun IslandPetComposable(
    mood: PetMood,
    petColor: Long,
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {},
    onDrag: (Float, Float) -> Unit = { _, _ -> },
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {}
) {
    // The observer keeps this in sync when Android's animator-duration scale changes while
    // the overlay is already on screen (for example through accessibility settings).
    val context = androidx.compose.ui.platform.LocalContext.current
    var motionEnabled by remember(context) { mutableStateOf(context.animatorMotionEnabled()) }
    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                motionEnabled = context.animatorMotionEnabled()
            }
        }
        context.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer
        )
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    val currentTap by rememberUpdatedState(onTap)
    val currentDrag by rememberUpdatedState(onDrag)
    val currentDragStart by rememberUpdatedState(onDragStart)
    val currentDragEnd by rememberUpdatedState(onDragEnd)
    val currentDragCancel by rememberUpdatedState(onDragCancel)
    val baseColor = Color(petColor)
    val petContentDescription = stringResource(R.string.pet_content_description)
    val ink = if (baseColor.luminance() > 0.48f) Color(0xFF18202A) else Color.White
    val motion = rememberPetMotion(mood = mood, enabled = motionEnabled)

    var blinking by remember { mutableStateOf(false) }
    LaunchedEffect(motionEnabled) {
        if (!motionEnabled) {
            blinking = false
            return@LaunchedEffect
        }
        while (true) {
            delay(3200)
            blinking = true
            delay(115)
            blinking = false
        }
    }

    // Keep the accumulated pointer displacement locally so the visual response is immediate
    // and returns to rest even when the ViewModel deliberately does not move the pet itself.
    var dragDx by remember { mutableFloatStateOf(0f) }
    var dragDy by remember { mutableFloatStateOf(0f) }
    val isDragging = mood == PetMood.Dragging
    val dragRotationZ by animateFloatAsState(
        targetValue = if (isDragging) (dragDx * 0.14f).coerceIn(-12f, 12f) else 0f,
        animationSpec = if (motionEnabled) spring(dampingRatio = 0.62f, stiffness = 460f) else snap(),
        label = "dragTiltZ"
    )
    val dragRotationX by animateFloatAsState(
        targetValue = if (isDragging) (-dragDy * 0.12f).coerceIn(-9f, 9f) else 0f,
        animationSpec = if (motionEnabled) spring(dampingRatio = 0.62f, stiffness = 460f) else snap(),
        label = "dragTiltX"
    )
    val tapScale by animateFloatAsState(
        targetValue = if (mood == PetMood.Tapped) .84f else 1f,
        animationSpec = if (motionEnabled) spring(dampingRatio = .42f, stiffness = 680f) else snap(),
        label = "tapSquash"
    )
    val dragScaleX by animateFloatAsState(
        targetValue = if (isDragging) 1.16f else 1f,
        animationSpec = if (motionEnabled) spring(.55f, 420f) else snap(),
        label = "dragStretchX"
    )
    val dragScaleY by animateFloatAsState(
        targetValue = if (isDragging) .88f else 1f,
        animationSpec = if (motionEnabled) spring(.55f, 420f) else snap(),
        label = "dragStretchY"
    )
    val stateBob = when (mood) {
        PetMood.Music -> -2.2f * motion.rhythm
        PetMood.Charging -> -1.6f * motion.rhythm
        PetMood.Calling -> -1.3f * motion.rhythm
        PetMood.LowBattery -> 1.3f * motion.rhythm
        PetMood.Sleeping -> 1.1f * motion.rhythm
        else -> 0f
    }
    val stateTilt = when (mood) {
        PetMood.Music -> 5f * motion.rhythm
        PetMood.Calling -> 4f * motion.rhythm
        PetMood.LowBattery -> -3f * motion.rhythm
        PetMood.Installing -> 8f * motion.rhythm
        else -> 0f
    }

    Box(
        modifier = modifier
            .size(32.dp)
            .graphicsLayer {
                scaleX = motion.breathe * tapScale * dragScaleX
                scaleY = motion.breathe * tapScale * dragScaleY
                translationY = stateBob + motion.bounce
                rotationX = dragRotationX
                rotationZ = stateTilt + dragRotationZ
            }
            .clip(RoundedCornerShape(12.dp))
            .background(baseColor)
            .semantics(mergeDescendants = true) {
                contentDescription = petContentDescription
                role = Role.Button
                // Retains the existing Portuguese action label while the element itself is localized.
                onClick(label = "Interagir com mascote") { currentTap(); true }
            }
            // Unit deliberately keeps the gesture coroutine alive through mood changes.
            .pointerInput(Unit) { detectTapGestures(onTap = { currentTap() }) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        dragDx = 0f
                        dragDy = 0f
                        currentDragStart()
                    },
                    onDragCancel = {
                        dragDx = 0f
                        dragDy = 0f
                        currentDragCancel()
                    },
                    onDragEnd = {
                        dragDx = 0f
                        dragDy = 0f
                        currentDragEnd()
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        dragDx = (dragDx + amount.x).coerceIn(-96f, 96f)
                        dragDy = (dragDy + amount.y).coerceIn(-96f, 96f)
                        currentDrag(amount.x, amount.y)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(32.dp)) { drawPetFace(mood, blinking, motion.rhythm, dragDx, ink) }
    }
}

private fun Context.animatorMotionEnabled(): Boolean =
    Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f

private data class PetMotion(
    val breathe: Float = 1f,
    val rhythm: Float = 0f,
    val bounce: Float = 0f
)

/** Infinite transitions are only composed while system motion is enabled. */
@Composable
private fun rememberPetMotion(mood: PetMood, enabled: Boolean): PetMotion {
    if (!enabled) return PetMotion()

    val transition = rememberInfiniteTransition(label = "petMotion")
    val breathe by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (mood !in setOf(PetMood.Dragging, PetMood.Tapped)) 1.035f else 1f,
        animationSpec = infiniteRepeatable(tween(1900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathing"
    )
    val rhythm by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (mood in setOf(
                PetMood.Music, PetMood.Charging, PetMood.Calling, PetMood.LowBattery,
                PetMood.Installing, PetMood.Sleeping
            )
        ) 1f else 0f,
        animationSpec = infiniteRepeatable(
            tween(
                when (mood) {
                    PetMood.Calling -> 520
                    PetMood.LowBattery -> 1100
                    PetMood.Music -> 700
                    PetMood.Sleeping -> 1800
                    else -> 900
                },
                easing = FastOutSlowInEasing
            ),
            RepeatMode.Reverse
        ),
        label = "stateRhythm"
    )
    val bounce by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (mood == PetMood.Happy || mood == PetMood.Excited) -3.4f else 0f,
        animationSpec = infiniteRepeatable(tween(520, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "happyBounce"
    )
    return PetMotion(breathe = breathe, rhythm = rhythm, bounce = bounce)
}

private fun DrawScope.drawPetFace(
    mood: PetMood,
    blinking: Boolean,
    rhythm: Float,
    dragDx: Float,
    ink: Color
) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val eyeY = h * .40f
    val gap = w * .19f
    val r = w * .075f
    val left = cx - gap
    val right = cx + gap
    val asleep = mood == PetMood.Sleeping || blinking
    if (asleep || mood == PetMood.Music) {
        listOf(left, right).forEach { x ->
            drawArc(ink, 20f, 140f, false, Offset(x - r, eyeY - r * .5f), Size(r * 2, r), style = Stroke(2.2f))
        }
    } else if (mood == PetMood.Excited || mood == PetMood.Happy) {
        drawStarEye(left, eyeY, r, ink)
        drawStarEye(right, eyeY, r, ink)
    } else {
        val pupilShift = if (mood == PetMood.Dragging) (dragDx / 96f).coerceIn(-1f, 1f) * r * .65f else 0f
        listOf(left, right).forEach { x ->
            drawCircle(ink, r, Offset(x, eyeY))
            drawCircle(
                if (ink == Color.White) Color(0xFF202632) else Color.White,
                r * .42f,
                Offset(x + pupilShift, eyeY)
            )
        }
    }
    val mouthY = h * .65f
    when (mood) {
        PetMood.LowBattery -> drawArc(ink, 200f, 140f, false, Offset(cx - w * .13f, mouthY), Size(w * .26f, w * .13f), style = Stroke(2.2f))
        PetMood.Tapped -> drawCircle(ink, w * .052f, Offset(cx, mouthY), style = Stroke(2f))
        PetMood.Installing -> drawLine(ink, Offset(cx - w * .10f, mouthY), Offset(cx + w * .10f, mouthY), 2.2f)
        else -> drawArc(ink, 25f, 130f, false, Offset(cx - w * .12f, mouthY - w * .07f), Size(w * .24f, w * .14f), style = Stroke(2.2f))
    }
    when (mood) {
        PetMood.Charging -> drawLine(ink.copy(alpha = .7f), Offset(w * .18f, h * .17f), Offset(w * .25f, h * .10f), 1.8f)
        PetMood.Calling -> drawCircle(ink.copy(alpha = .65f), w * .055f + rhythm * w * .025f, Offset(w * .83f, h * .20f), style = Stroke(1.5f))
        PetMood.Sleeping -> drawLine(ink.copy(alpha = .7f), Offset(w * .72f, h * .16f), Offset(w * .87f, h * .16f), 1.5f)
        else -> Unit
    }
}

private fun DrawScope.drawStarEye(cx: Float, cy: Float, radius: Float, color: Color) {
    val path = Path()
    repeat(10) { i ->
        val angle = Math.PI * 2 * i / 10 - Math.PI / 2
        val r = if (i % 2 == 0) radius * 1.25f else radius * .52f
        val x = cx + (r * kotlin.math.cos(angle)).toFloat()
        val y = cy + (r * kotlin.math.sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}
