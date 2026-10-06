/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License.
 * Virtual pet ported from Pixi (github.com/in3zalo-gk/pixi, MIT).
 */

package com.agupta07505.smartisland.ui

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.AvTimer
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.model.IslandMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

enum class PixiPetMood {
    IDLE, ALERT, HAPPY, ANNOYED, DIZZY, DRAG, DOWNLOAD, SLEEP, CHARGE, BATTERY_LOW
}

sealed interface PixiPetEvent {
    data class Notification(
        val appName: String,
        val icon: Bitmap?,
        val mode: IslandMode,
        val eventId: Long
    ) : PixiPetEvent
    data class Download(val progress: Int) : PixiPetEvent
    data object Charge : PixiPetEvent
    data object BatteryLow : PixiPetEvent
    data object Idle : PixiPetEvent
}

private sealed interface PixiPetBadge {
    data class App(val appName: String, val icon: Bitmap?, val mode: IslandMode) : PixiPetBadge
    data class Download(val progress: Int) : PixiPetBadge
    data object Charging : PixiPetBadge
    data object BatteryLow : PixiPetBadge
}

private val ONE_SHOT_MOODS = setOf(
    PixiPetMood.ALERT, PixiPetMood.HAPPY, PixiPetMood.ANNOYED, PixiPetMood.DIZZY
)

private const val ONE_SHOT_DURATION_MS = 1150L
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
    nightModeEnabled: Boolean = false,
    nightModeStartHour: Int = 22,
    nightModeEndHour: Int = 7,
    modifier: Modifier = Modifier
) {
    var baseMood by remember { mutableStateOf(PixiPetMood.IDLE) }
    var baseBadge by remember { mutableStateOf<PixiPetBadge?>(null) }
    var mood by remember { mutableStateOf(PixiPetMood.IDLE) }
    var eventBadge by remember { mutableStateOf<PixiPetBadge?>(null) }
    var moodStartNanos by remember { mutableStateOf(System.nanoTime()) }
    var downloadProgress by remember { mutableStateOf(-1) }
    var oneShotToken by remember { mutableStateOf(0L) }
    var lastInteractTime by remember { mutableStateOf(System.currentTimeMillis()) }
    val clicks = remember { ArrayDeque<Long>() }
    val latestBaseMood = rememberUpdatedState(baseMood)
    val latestBaseBadge = rememberUpdatedState(baseBadge)
    val latestMood = rememberUpdatedState(mood)

    // Periodically re-evaluate whether it's currently nighttime
    var isNight by remember { mutableStateOf(false) }
    LaunchedEffect(nightModeEnabled, nightModeStartHour, nightModeEndHour) {
        if (!nightModeEnabled) {
            isNight = false
        } else {
            while (true) {
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                isNight = if (nightModeStartHour <= nightModeEndHour) {
                    hour in nightModeStartHour..nightModeEndHour
                } else {
                    hour >= nightModeStartHour || hour < nightModeEndHour
                }
                delay(60_000L) // re-check every minute
            }
        }
    }

    fun setMood(m: PixiPetMood, badge: PixiPetBadge? = eventBadge) {
        mood = m
        eventBadge = badge
        moodStartNanos = System.nanoTime()
        if (m in ONE_SHOT_MOODS) oneShotToken += 1L
    }

    fun setBaseMood(m: PixiPetMood, badge: PixiPetBadge?) {
        baseMood = m
        baseBadge = badge
        if (mood !in ONE_SHOT_MOODS) setMood(m, badge)
    }

    fun restoreBaseMood() {
        mood = latestBaseMood.value
        eventBadge = latestBaseBadge.value
        moodStartNanos = System.nanoTime()
    }

    // Keep frame time as draw state so animation frames invalidate Canvas, not
    // the whole overlay composition. This avoids a full recomposition at 60 fps.
    val frameNanos = remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) withFrameNanos { frameNanos.value = it }
    }

    // React to system events. Notifications are one-shot; charge/download/low
    // battery are base states, so a new alert returns to the correct behavior.
    LaunchedEffect(events) {
        events.collect { event ->
            when (event) {
                is PixiPetEvent.Notification -> {
                    setMood(
                        PixiPetMood.ALERT,
                        PixiPetBadge.App(event.appName, event.icon, event.mode)
                    )
                }
                is PixiPetEvent.Download -> {
                    downloadProgress = event.progress.coerceIn(-1, 100)
                    setBaseMood(PixiPetMood.DOWNLOAD, PixiPetBadge.Download(downloadProgress))
                }
                is PixiPetEvent.Charge -> {
                    downloadProgress = -1
                    setBaseMood(PixiPetMood.CHARGE, PixiPetBadge.Charging)
                }
                is PixiPetEvent.BatteryLow -> {
                    downloadProgress = -1
                    setBaseMood(PixiPetMood.BATTERY_LOW, PixiPetBadge.BatteryLow)
                }
                is PixiPetEvent.Idle -> {
                    downloadProgress = -1
                    setBaseMood(PixiPetMood.IDLE, null)
                }
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

    // Every alert/tap gets its own token. Repeated notifications can therefore
    // restart the animation even when the pet is already in ALERT.
    LaunchedEffect(oneShotToken) {
        if (mood in ONE_SHOT_MOODS) {
            val token = oneShotToken
            delay(ONE_SHOT_DURATION_MS)
            if (token == oneShotToken && latestMood.value in ONE_SHOT_MOODS) restoreBaseMood()
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
                                restoreBaseMood()
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
            nightMode = isNight,
            modifier = Modifier.fillMaxSize()
        )
        PixiPetEventBadge(eventBadge, isNight)
    }
}

// -------------------------------------------------------------------
//  Canvas drawing — procedural port of PixiView.kt
// -------------------------------------------------------------------

@Composable
private fun PixiPetCanvas(
    mood: PixiPetMood,
    frameNanos: State<Long>,
    moodStartNanos: Long,
    downloadProgress: Int,
    nightMode: Boolean,
    modifier: Modifier
) {
    val textPaint = remember(nightMode) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            color = if (nightMode) android.graphics.Color.rgb(100, 115, 160) else android.graphics.Color.rgb(70, 110, 220)
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        val frameTime = frameNanos.value
        val t = frameTime / 1_000_000_000f
        val dt = (frameTime - moodStartNanos).coerceAtLeast(0L) / 1_000_000_000f

        // --- Animation offsets per mood ---
        var dx = 0f
        var dy = 0f
        var sy = 1f
        when (mood) {
            PixiPetMood.ALERT -> {
                val damping = exp(-dt * 3.2f)
                dx = sin(dt * 26f) * w * 0.055f * damping
                dy = abs(sin(dt * 18f)) * h * 0.025f * damping
            }
            PixiPetMood.HAPPY -> {
                dy = -abs(sin(min(dt * 6f, 3.14f))) * h * 0.22f
                sy = 1f + 0.08f * abs(sin(min(dt * 6f, 3.14f)))
            }
            PixiPetMood.DRAG -> sy = 0.88f
            PixiPetMood.DIZZY -> {
                val damping = exp(-dt * 1.8f)
                dx = sin(dt * 12f) * w * 0.065f * damping
                dy = cos(dt * 10f) * h * 0.035f * damping
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
        // Night mode uses muted, low-saturation colors that are easier on the eyes in the dark.
        val bodyColor = if (nightMode) {
            when (mood) {
                PixiPetMood.ALERT -> Color(120, 90, 60)
                PixiPetMood.ANNOYED -> Color(110, 80, 100)
                PixiPetMood.DIZZY -> Color(80, 110, 90)
                PixiPetMood.CHARGE -> Color(130, 110, 60)
                PixiPetMood.BATTERY_LOW -> Color(115, 55, 58)
                PixiPetMood.SLEEP -> Color(50, 65, 100)
                PixiPetMood.HAPPY -> Color(70, 110, 100)
                else -> Color(60, 80, 120)
            }
        } else {
            when (mood) {
                PixiPetMood.ALERT -> Color(255, 170, 100)
                PixiPetMood.ANNOYED -> Color(220, 150, 190)
                PixiPetMood.DIZZY -> Color(160, 230, 160)
                PixiPetMood.CHARGE -> Color(255, 220, 100)
                PixiPetMood.BATTERY_LOW -> Color(255, 105, 105)
                PixiPetMood.SLEEP -> Color(100, 140, 200)
                PixiPetMood.HAPPY -> Color(140, 220, 180)
                else -> Color(120, 180, 255)
            }
        }
        val strokeColor = if (nightMode) Color(180, 190, 210, 200) else Color(40, 50, 70)
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
            color = strokeColor,
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
            val cheekColor = if (nightMode) Color(180, 120, 130, 90) else Color(255, 160, 170, 160)
            drawCircle(
                color = cheekColor,
                radius = cheekR,
                center = Offset(cx - bw * 0.28f, bodyTop + bh * 0.62f)
            )
            drawCircle(
                color = cheekColor,
                radius = cheekR,
                center = Offset(cx + bw * 0.28f, bodyTop + bh * 0.62f)
            )
        }

        // --- Eyes ---
        val blink = (frameTime / 1_000_000L % 4200) < 140
        val er = bw * if (mood == PixiPetMood.ALERT || mood == PixiPetMood.DRAG) 0.15f else 0.11f
        val ey = bodyTop + bh * 0.40f

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
                    val eyeWhite = if (nightMode) Color(200, 205, 220) else Color.White
                    val pupilColor = if (nightMode) Color(40, 45, 60) else Color(30, 35, 50)
                    drawCircle(
                        color = eyeWhite,
                        radius = er,
                        center = Offset(ex, ey)
                    )
                    drawCircle(
                        color = pupilColor,
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

        // Download: progress bar at bottom
        if (mood == PixiPetMood.DOWNLOAD) {
            val bl = w * 0.12f
            val br = w * 0.88f
            val by = h * 0.96f
            val barWidth = h * 0.055f
            val barBg = if (nightMode) Color(100, 100, 110, 50) else Color(128, 128, 128, 70)
            val barFg = if (nightMode) Color(80, 110, 160) else Color(70, 110, 220)
            drawLine(
                color = barBg,
                start = Offset(bl, by),
                end = Offset(br, by),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
            if (downloadProgress in 0..100) {
                drawLine(
                    color = barFg,
                    start = Offset(bl, by),
                    end = Offset(bl + (br - bl) * downloadProgress / 100f, by),
                    strokeWidth = barWidth,
                    cap = StrokeCap.Round
                )
            } else {
                val s0 = bl + (br - bl) * ((t * 1.3f) % 1f) * 0.65f
                drawLine(
                    color = barFg,
                    start = Offset(s0, by),
                    end = Offset(s0 + (br - bl) * 0.28f, by),
                    strokeWidth = barWidth,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}


@Composable
private fun BoxScope.PixiPetEventBadge(badge: PixiPetBadge?, nightMode: Boolean) {
    if (badge == null) return

    val pulseTransition = rememberInfiniteTransition(label = "petChargingBadge")
    val chargingPulse by pulseTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chargingBadgePulse"
    )
    val accent = when (badge) {
        PixiPetBadge.Charging -> Color(0xFFFFC857)
        PixiPetBadge.BatteryLow -> Color(0xFFFF6666)
        is PixiPetBadge.Download -> Color(0xFF69B7FF)
        is PixiPetBadge.App -> Color(0xFFB7C9FF)
    }
    val background = if (nightMode) Color(0xFF242833) else Color(0xFF17202A)

    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(x = 2.dp, y = 2.dp)
            .size(28.dp)
            .then(
                if (badge == PixiPetBadge.Charging) {
                    Modifier.graphicsLayer {
                        scaleX = chargingPulse
                        scaleY = chargingPulse
                    }
                } else Modifier
            )
            .clip(CircleShape)
            .background(background)
            .border(1.5.dp, accent, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        when (badge) {
            is PixiPetBadge.App -> {
                val image = badge.icon?.asImageBitmap()
                if (badge.mode == IslandMode.Notification && image != null) {
                    Image(
                        bitmap = image,
                        contentDescription = "${badge.appName} notification",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp)
                            .clip(CircleShape)
                    )
                } else {
                    val glyph = badge.mode.petGlyph()
                    if (glyph != null) {
                        Icon(
                            imageVector = glyph,
                            contentDescription = "${badge.appName} activity",
                            tint = accent,
                            modifier = Modifier.size(16.dp)
                        )
                    } else if (image != null) {
                        Image(
                            bitmap = image,
                            contentDescription = "${badge.appName} notification",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(2.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Text(
                            text = badge.appName.firstOrNull()?.uppercase() ?: "S",
                            color = accent,
                            maxLines = 1
                        )
                    }
                }
            }
            is PixiPetBadge.Download -> {
                Canvas(Modifier.fillMaxSize().padding(1.dp)) {
                    drawArc(
                        color = accent,
                        startAngle = -90f,
                        sweepAngle = 360f * (badge.progress.coerceIn(0, 100) / 100f),
                        useCenter = false,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.ArrowDownward,
                    contentDescription = "Download ${badge.progress}%",
                    tint = accent,
                    modifier = Modifier.size(15.dp)
                )
            }
            PixiPetBadge.Charging -> Icon(
                imageVector = Icons.Rounded.Bolt,
                contentDescription = "Charging",
                tint = accent,
                modifier = Modifier.size(17.dp)
            )
            PixiPetBadge.BatteryLow -> Icon(
                imageVector = Icons.Rounded.BatteryAlert,
                contentDescription = "Low battery",
                tint = accent,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

private fun IslandMode.petGlyph(): ImageVector? = when (this) {
    IslandMode.IncomingCall -> Icons.Rounded.Call
    IslandMode.Music -> Icons.Rounded.MusicNote
    IslandMode.Battery -> Icons.Rounded.Bolt
    IslandMode.DownloadUpload -> Icons.Rounded.ArrowDownward
    IslandMode.Navigation -> Icons.Rounded.Navigation
    IslandMode.Hotspot -> Icons.Rounded.WifiTethering
    IslandMode.Bluetooth -> Icons.Rounded.BluetoothConnected
    IslandMode.Flashlight -> Icons.Rounded.FlashlightOn
    IslandMode.Timer -> Icons.Rounded.Timer
    IslandMode.Stopwatch -> Icons.Rounded.AvTimer
    else -> null
}
