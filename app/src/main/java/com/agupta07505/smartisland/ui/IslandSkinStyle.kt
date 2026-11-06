/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.agupta07505.smartisland.model.IslandSkin
import com.agupta07505.smartisland.model.IslandSkinPalette
import kotlin.math.min

/**
 * Draws Smart Island's original local skin treatment over the pill.
 *
 * Apply this *after* the overlay's base background/clip modifier so its Canvas layer remains
 * visible, for example: `Modifier.clip(shape).background(base).islandSkin(skin, palette, true)`.
 * Pass `animated = false` when system/app motion is disabled; static colour and contrast remain.
 */
/** Basic integration API for overlays that do not expose a palette selector. */
@Composable
fun Modifier.islandSkin(
    skin: IslandSkin,
    animated: Boolean = true
): Modifier = islandSkin(skin, IslandSkinPalette.Default, animated)

/** Palette-aware integration API used by the settings catalogue. */
@Composable
fun Modifier.islandSkin(
    skin: IslandSkin,
    palette: IslandSkinPalette,
    animated: Boolean = true,
    customPalette: List<Color>? = null
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "islandSkin")
    val motion = if (animated) {
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "islandSkinMotion"
        ).value
    } else {
        0.35f
    }
    val colors = customPalette?.take(4)?.takeIf { it.isNotEmpty() } ?: if (palette == IslandSkinPalette.Wallpaper) {
        val scheme = androidx.compose.material3.MaterialTheme.colorScheme
        listOf(scheme.primary, scheme.secondary, scheme.tertiary)
    } else palette.colors.map(::Color)

    drawWithContent {
        val radius = size.height / 2f
        val primary = colors.first()
        val secondary = colors.getOrElse(1) { primary }
        val tertiary = colors.getOrElse(2) { secondary }
        val quaternary = colors.getOrElse(3) { tertiary }
        val center = Offset(size.width / 2f, size.height / 2f)

        when (skin) {
            IslandSkin.Classic -> {
                drawRoundRect(
                    color = primary.copy(alpha = 0.22f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
                    style = Stroke(width = 1.1f)
                )
            }
            IslandSkin.Obsidian -> {
                drawRoundRect(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF090A10), Color(0xFF171126), Color(0xFF08080CL)),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
                )
                drawRoundRect(
                    color = primary.copy(alpha = 0.30f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
                    style = Stroke(width = 1.2f)
                )
            }
            IslandSkin.Titanium -> {
                drawRoundRect(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF17191DL), Color(0xFF5A6068L), Color(0xFF15171BL)),
                        start = Offset(0f, 0f), end = Offset(0f, size.height)
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.38f),
                    start = Offset(radius, 1f), end = Offset(size.width - radius, 1f),
                    strokeWidth = 1f, cap = StrokeCap.Round
                )
            }
            IslandSkin.BlackHole -> {
                // A slow, local drift keeps the central well alive without ever moving its hit area.
                val wellCenter = center.copy(x = center.x + (motion - 0.5f) * size.width * 0.10f)
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(primary.copy(alpha = 0.45f), Color(0xFF030305), Color.Black),
                        center = wellCenter, radius = size.width * (0.60f + motion * 0.08f)
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
                )
                drawCircle(Color.Black.copy(alpha = 0.46f), radius = min(size.width, size.height) * (0.22f + motion * 0.05f), center = wellCenter)
            }
            IslandSkin.Eclipse -> {
                drawRoundRect(
                    color = Color(0xFF07070AL),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
                )
                // The rim travels only over its own lower arc; content and the pill silhouette stay still.
                drawArc(
                    color = secondary.copy(alpha = 0.56f + motion * 0.24f), startAngle = 190f + motion * 10f, sweepAngle = 150f,
                    useCenter = false, topLeft = Offset(1f, 1f), size = Size(size.width - 2f, size.height - 2f),
                    style = Stroke(width = 1.8f, cap = StrokeCap.Round)
                )
            }
            IslandSkin.Lightspeed -> {
                drawRoundRect(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF080A11), primary.copy(alpha = 0.7f), tertiary.copy(alpha = 0.62f), quaternary.copy(alpha = 0.62f), secondary.copy(alpha = 0.7f), Color(0xFF080A11)),
                        start = Offset(-size.width * 0.2f + motion * size.width, 0f),
                        end = Offset(size.width * 0.55f + motion * size.width, size.height)
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
                )
            }
            IslandSkin.Saturn -> {
                drawRoundRect(
                    color = Color(0xFF11100EL),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
                )
                drawOval(
                    brush = Brush.linearGradient(listOf(primary, tertiary, secondary)),
                    topLeft = Offset(size.width * 0.12f, size.height * 0.28f),
                    size = Size(size.width * 0.76f, size.height * 0.44f),
                    style = Stroke(width = 1.5f)
                )
                drawCircle(Color(0xFF0A0908), radius = size.height * 0.20f, center = center, blendMode = BlendMode.SrcOver)
            }
        }
        drawContent()
    }
}

/** Adds a community palette and animation over its selected native base style. */
@Composable
fun Modifier.communitySkin(skin: com.agupta07505.smartisland.model.CommunitySkin?, state: String, reducedMotion: Boolean = false): Modifier = composed {
    if (skin == null) return@composed this
    val spec = skin.stateFor(state)
    val palette = spec.colors.map(::Color).take(4)
    val transition = rememberInfiniteTransition(label = "communitySkin")
    val phase = if (!reducedMotion && spec.animation.type != "none") {
        transition.animateFloat(0f, 1f, infiniteRepeatable(tween((1800 / spec.animation.speed).toInt(), easing = LinearEasing)), label = "communitySkinPhase").value
    } else 0.35f

    islandSkin(
        skin = skin.baseStyle,
        palette = IslandSkinPalette.Default,
        animated = !reducedMotion && spec.animation.type != "none",
        customPalette = palette.takeIf { it.isNotEmpty() }
    ).drawWithContent {
        val radius = size.height / 2f
        val decoration = (if (palette.isEmpty()) listOf(Color.White.copy(alpha = 0.28f)) else palette)
            .let { if (it.size == 1) it + it else it }
        val staticBrush = Brush.linearGradient(decoration.map { it.copy(alpha = 0.46f) })
        // Always keep this to a stroke: it preserves the native base and never covers island content.
        drawRoundRect(
            brush = staticBrush,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
            style = Stroke(width = 1.1f)
        )
        when (spec.animation.type) {
            "pulse" -> drawRoundRect(
                brush = Brush.linearGradient(decoration.map { it.copy(alpha = 0.18f + phase * 0.32f) }),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
                style = Stroke(width = 2f)
            )
            "flow" -> drawRoundRect(
                brush = Brush.linearGradient(
                    decoration.map { it.copy(alpha = 0.68f) },
                    start = Offset(-size.width + phase * size.width * 2f, 0f),
                    end = Offset(phase * size.width * 2f, size.height)
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
                style = Stroke(width = 1.8f)
            )
            "orbit" -> drawCircle(
                brush = Brush.radialGradient(decoration.map { it.copy(alpha = 0.7f) }),
                radius = 2.5f,
                center = Offset(size.width * (.15f + .7f * phase), size.height / 2f)
            )
        }
        drawContent()
    }
}
