/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License.
 * Virtual pet ported from Pixi (github.com/in3zalo-gk/pixi, MIT).
 */

package com.agupta07505.smartisland.ui

import android.graphics.Paint
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class PixiPetMood {
    IDLE, ALERT, HAPPY, ANNOYED, DIZZY, DRAG, DOWNLOAD, SLEEP, CHARGE
}

sealed interface PixiPetEvent {
    data object Notification : PixiPetEvent
    data class Download(val progress: Int) : PixiPetEvent
    data object Charge : PixiPetEvent
    data object BatteryLow : PixiPetEvent
    data object Idle : PixiPetEvent
}

private val ONE_SHOT_MOODS = setOf(
    PixiPetMood.HAPPY, PixiPetMood.ANNOYED, PixiPetMood.DIZZY
)

private const val ONE_SHOT_DURATION_MS = 1500L
private const val TAP_WINDOW_MS = 400L

/**
 * Floating virtual-pet overlay.  Ported from Pixi's PixiView — a square blob
 * with moods, procedural drawing, tap interactions, drag-to-reposition, and
 * auto-sleep.  Position persistence is handled by the caller via callbacks.
 */
@Composable
fun PixiPetOverlay(
    petSize: Dp,
    sleepTimeoutSec: Int,
    events: Flow<PixiPetEvent>,
    onDragOffset: (Int, Int) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mood by remember { mutableStateOf(PixiPetMood.IDLE) }
    var moodStartNanos by remember { mutableStateOf(System.nanoTime()) }
    var downloadProgress by remember { mutableStateOf(-1) }
    var lastInteractTime by remember { mutableStateOf(System.currentTimeMillis()) }
    val clicks = remember { ArrayDeque<Long>() }

    fun setMood(m: PixiPetMood) {
        if (mood != m) {
            mood = m
            moodStartNanos = System.nanoTime()
        }
    }

    // Continuous animation clock — drives Canvas redraw every frame
    var frameNanos by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) withFrameNanos { frameNanos = it }
    }

    // React to system events (notifications, charging, downloads, etc.)
    LaunchedEffect(Unit) {
        events.collect { event ->
            when (event) {
                is PixiPetEvent.Notification -> setMood(PixiPetMood.ALERT)
                is PixiPetEvent.Download -> {
                    downloadProgress = event.progress
                    setMood(PixiPetMood.DOWNLOAD)
                }
                is PixiPetEvent.Charge -> setMood(PixiPetMood.CHARGE)
                is PixiPetEvent.BatteryLow -> setMood(PixiPetMood.ALERT)
                is PixiPetEvent.Idle -> setMood(PixiPetMood.IDLE)
            }
            lastInteractTime = System.currentTimeMillis()
        }
    }

    // Auto-sleep after inactivity
    LaunchedEffect(mood, lastInteractTime) {
        if (mood == PixiPetMood.IDLE) {
            delay(sleepTimeoutSec * 1000L)
            if (mood == PixiPetMood.IDLE) setMood(PixiPetMood.SLEEP)
        }
    }

    // One-shot moods auto-return to IDLE
    LaunchedEffect(mood) {
        if (mood in ONE_SHOT_MOODS) {
            delay(ONE_SHOT_DURATION_MS)
            if (mood in ONE_SHOT_MOODS) setMood(PixiPetMood.IDLE)
        }
    }

    Box(
        modifier = modifier
            .size(petSize)
            .pointerInput(Unit) {
                val slop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown()
                    lastInteractTime = System.currentTimeMillis()
                    if (mood == PixiPetMood.SLEEP) setMood(PixiPetMood.IDLE)

                    var isDrag = false
                    var totalX = 0f
                    var totalY = 0f

                    while (true) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.first()
                        if (ch.changedToUp()) {
                            if (!isDrag) {
                                // Tap — count consecutive taps for mood
                                val now = System.currentTimeMillis()
                                clicks.addLast(now)
                                while (clicks.isNotEmpty() && now - clicks.first() > TAP_WINDOW_MS) {
                                    clicks.removeFirst()
                                }
                                when (clicks.size) {
                                    1 -> setMood(PixiPetMood.HAPPY)
                                    2 -> setMood(PixiPetMood.DIZZY)
                                    3 -> { setMood(PixiPetMood.ANNOYED); clicks.clear() }
                                }
                            } else {
                                onDragEnd()
                                setMood(PixiPetMood.IDLE)
                            }
                            break
                        } else {
                            val d = ch.positionChange()
                            totalX += d.x
                            totalY += d.y
                            if (abs(totalX) > slop || abs(totalY) > slop) {
                                if (!isDrag) {
                                    isDrag = true
                                    setMood(PixiPetMood.DRAG)
                                }
                                onDragOffset(d.x.toInt(), d.y.toInt())
                                ch.consume()
                            }
                        }
                    }
                }
            }
    ) {
        PixiPetCanvas(
            mood = mood,
            frameNanos = frameNanos,
            moodStartNanos = moodStartNanos,
            downloadProgress = downloadProgress,
            modifier = Modifier.fillMaxSize()
        )
    }
}

// -------------------------------------------------------------------
//  Canvas drawing — procedural port of PixiView.kt
// -------------------------------------------------------------------

@Composable
private fun PixiPetCanvas(
    mood: PixiPetMood,
    frameNanos: Long,
    moodStartNanos: Long,
    downloadProgress: Int,
    modifier: Modifier
) {
    val t = frameNanos / 1_000_000_000f
    val dt = (frameNanos - moodStartNanos) / 1_000_000_000f

    val textPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            color = android.graphics.Color.rgb(70, 110, 220)
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        // --- Animation offsets per mood ---
        var dx = 0f
        var dy = 0f
        var sy = 1f
        when (mood) {
            PixiPetMood.ALERT -> {
                dx = sin(t * 18f) * w * 0.06f
                dy = abs(sin(t * 12f)) * h * 0.03f
            }
            PixiPetMood.HAPPY -> {
                dy = -abs(sin(min(dt * 6f, 3.14f))) * h * 0.22f
                sy = 1f + 0.08f * abs(sin(min(dt * 6f, 3.14f)))
            }
            PixiPetMood.DRAG -> sy = 0.88f
            PixiPetMood.DIZZY -> {
                dx = sin(t * 10f) * w * 0.08f
                dy = cos(t * 8f) * h * 0.04f
            }
            PixiPetMood.SLEEP -> dy = 2f + abs(sin(t * 1.2f)) * 2f
            PixiPetMood.CHARGE -> {
                dy = -abs(sin(t * 4f)) * h * 0.08f
                sy = 1f + 0.05f * abs(sin(t * 4f))
            }
            PixiPetMood.DOWNLOAD -> dy = sin(t * 3f) * h * 0.02f
            else -> sy = 1f + 0.03f * sin(t * 2.2f)
        }

        val cx = w / 2f + dx
        val bottom = h * 0.88f + dy

        // --- Body ---
        val bodyColor = when (mood) {
            PixiPetMood.ALERT -> Color(255, 170, 100)
            PixiPetMood.ANNOYED -> Color(220, 150, 190)
            PixiPetMood.DIZZY -> Color(160, 230, 160)
            PixiPetMood.CHARGE -> Color(255, 220, 100)
            PixiPetMood.SLEEP -> Color(100, 140, 200)
            PixiPetMood.HAPPY -> Color(140, 220, 180)
            else -> Color(120, 180, 255)
        }
        val s = 1f + 0.03f * sin(t * 2f)
        val bw = w * 0.72f * s * (if (mood == PixiPetMood.DRAG) 1.12f else 1f)
        val bh = h * 0.58f * (2f - s) * sy * (if (mood == PixiPetMood.DRAG) 0.9f else 1f)
        val bodyLeft = cx - bw / 2f
        val bodyTop = bottom - bh
        val cr = CornerRadius(bw * 0.38f, bw * 0.38f)

        drawRoundRect(
            color = bodyColor,
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bw, bh),
            cornerRadius = cr
        )
        drawRoundRect(
            color = Color(40, 50, 70),
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bw, bh),
            cornerRadius = cr,
            style = Stroke(width = 2.5f * (w / 80f))
        )

        // --- Cheeks ---
        if (mood in setOf(
                PixiPetMood.IDLE,
                PixiPetMood.HAPPY,
                PixiPetMood.CHARGE,
                PixiPetMood.DOWNLOAD
            )
        ) {
            val cheekR = bw * 0.12f
            drawCircle(
                color = Color(255, 160, 170, 160),
                radius = cheekR,
                center = Offset(cx - bw * 0.28f, bodyTop + bh * 0.62f)
            )
            drawCircle(
                color = Color(255, 160, 170, 160),
                radius = cheekR,
                center = Offset(cx + bw * 0.28f, bodyTop + bh * 0.62f)
            )
        }

        // --- Eyes ---
        val blink = (frameNanos / 1_000_000L % 4200) < 140
        val er = bw * if (mood == PixiPetMood.ALERT || mood == PixiPetMood.DRAG) 0.15f else 0.11f
        val ey = bodyTop + bh * 0.40f
        val strokeColor = Color(40, 50, 70)

        for (sign in intArrayOf(-1, 1)) {
            val ex = cx + sign * bw * 0.22f
            when {
                mood == PixiPetMood.HAPPY || mood == PixiPetMood.CHARGE -> {
                    drawArc(
                        color = strokeColor,
                        startAngle = 200f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = Offset(ex - er, ey - er),
                        size = Size(er * 2, er * 2),
                        style = Stroke(width = er * 0.35f, cap = StrokeCap.Round)
                    )
                }
                mood == PixiPetMood.ANNOYED -> {
                    drawLine(
                        color = strokeColor,
                        start = Offset(ex - er, ey - sign * er * 0.45f),
                        end = Offset(ex + er, ey + sign * er * 0.45f),
                        strokeWidth = er * 0.35f,
                        cap = StrokeCap.Round
                    )
                }
                mood == PixiPetMood.DIZZY -> {
                    drawLine(
                        color = strokeColor,
                        start = Offset(ex - er, ey - er),
                        end = Offset(ex + er, ey + er),
                        strokeWidth = er * 0.35f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = strokeColor,
                        start = Offset(ex - er, ey + er),
                        end = Offset(ex + er, ey - er),
                        strokeWidth = er * 0.35f,
                        cap = StrokeCap.Round
                    )
                }
                mood == PixiPetMood.SLEEP || blink -> {
                    drawLine(
                        color = strokeColor,
                        start = Offset(ex - er, ey),
                        end = Offset(ex + er, ey),
                        strokeWidth = er * 0.35f,
                        cap = StrokeCap.Round
                    )
                }
                else -> {
                    val look = when (mood) {
                        PixiPetMood.DOWNLOAD -> er * 0.4f
                        PixiPetMood.DRAG -> -er * 0.15f
                        else -> 0f
                    }
                    drawCircle(
                        color = Color.White,
                        radius = er,
                        center = Offset(ex, ey)
                    )
                    drawCircle(
                        color = Color(30, 35, 50),
                        radius = er * 0.48f,
                        center = Offset(ex, ey + look)
                    )
                }
            }
        }

        // --- Mood effects ---

        // Sleep: floating Zzz
        if (mood == PixiPetMood.SLEEP) {
            val zx = cx + w * 0.28f
            val zy = bodyTop + h * 0.10f - abs(sin(t * 1.5f)) * h * 0.06f
            textPaint.textSize = h * 0.18f
            drawIntoCanvas { it.nativeCanvas.drawText("z", zx, zy, textPaint) }
            textPaint.textSize = h * 0.12f
            drawIntoCanvas { it.nativeCanvas.drawText("z", zx + w * 0.08f, zy - h * 0.08f, textPaint) }
        }

        // Charge: ⚡ symbol
        if (mood == PixiPetMood.CHARGE) {
            textPaint.textSize = h * 0.22f
            drawIntoCanvas {
                it.nativeCanvas.drawText("⚡", cx + w * 0.28f, bodyTop + h * 0.14f, textPaint)
            }
        }

        // Download: progress bar at bottom
        if (mood == PixiPetMood.DOWNLOAD) {
            val bl = w * 0.12f
            val br = w * 0.88f
            val by = h * 0.96f
            val barWidth = h * 0.055f
            drawLine(
                color = Color(128, 128, 128, 70),
                start = Offset(bl, by),
                end = Offset(br, by),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
            if (downloadProgress in 0..100) {
                drawLine(
                    color = Color(70, 110, 220),
                    start = Offset(bl, by),
                    end = Offset(bl + (br - bl) * downloadProgress / 100f, by),
                    strokeWidth = barWidth,
                    cap = StrokeCap.Round
                )
            } else {
                val s0 = bl + (br - bl) * ((t * 1.3f) % 1f) * 0.65f
                drawLine(
                    color = Color(70, 110, 220),
                    start = Offset(s0, by),
                    end = Offset(s0 + (br - bl) * 0.28f, by),
                    strokeWidth = barWidth,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
