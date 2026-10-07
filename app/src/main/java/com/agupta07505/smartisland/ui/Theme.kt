/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository

val LocalSmartIslandIconShape = staticCompositionLocalOf<Shape> { RoundedCornerShape(12.dp) }

private val BrandLightColors = lightColorScheme(
    primary = Color(0xFFD84315), onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEDE6), onPrimaryContainer = Color(0xFFBF360C),
    secondary = Color(0xFFBF360C), onSecondary = Color.White,
    secondaryContainer = Color(0xFFFCEBE6), onSecondaryContainer = Color(0xFF3E1204),
    tertiary = Color(0xFF0D9488), onTertiary = Color.White,
    background = Color(0xFFFAFAFA), onBackground = Color(0xFF121212),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF121212),
    surfaceVariant = Color(0xFFF1F1F1), onSurfaceVariant = Color(0xFF5A5A5A),
    outline = Color(0xFFE0E0E0), outlineVariant = Color(0xFFEDEDED)
)

private val BrandDarkColors = darkColorScheme(
    primary = Color(0xFFFF8A65), onPrimary = Color(0xFF2B0B02),
    primaryContainer = Color(0xFF3D1307), onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = Color(0xFFFFAB91), onSecondary = Color(0xFF2B0B02),
    secondaryContainer = Color(0xFF2A1008), onSecondaryContainer = Color(0xFFFFCCBC),
    tertiary = Color(0xFF5EEAD4), onTertiary = Color(0xFF042F2E),
    background = Color(0xFF101113), onBackground = Color(0xFFF6F4F2),
    surface = Color(0xFF191A1D), onSurface = Color(0xFFF6F4F2),
    surfaceVariant = Color(0xFF292A2E), onSurfaceVariant = Color(0xFFC5C6CA),
    outline = Color(0xFF77787C), outlineVariant = Color(0xFF414247)
)

private val OneUiLightColors = lightColorScheme(
    primary = Color(0xFF3867B2), onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE7FF), onPrimaryContainer = Color(0xFF102F60),
    secondary = Color(0xFF536B91), onSecondary = Color.White,
    background = Color(0xFFF6F7F9), onBackground = Color(0xFF17191D),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF17191D),
    surfaceVariant = Color(0xFFE9EDF3), onSurfaceVariant = Color(0xFF454B55)
)
private val OneUiDarkColors = darkColorScheme(
    primary = Color(0xFFAFC6FF), onPrimary = Color(0xFF17345F),
    primaryContainer = Color(0xFF2A4774), onPrimaryContainer = Color(0xFFDCE7FF),
    secondary = Color(0xFFC0CBE0), onSecondary = Color(0xFF29364C),
    background = Color(0xFF111318), onBackground = Color(0xFFE4E7ED),
    surface = Color(0xFF191C22), onSurface = Color(0xFFE4E7ED),
    surfaceVariant = Color(0xFF2A2E36), onSurfaceVariant = Color(0xFFC1C7D1)
)

private val IosLightColors = lightColorScheme(
    primary = Color(0xFF007AFF), onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E9FF), onPrimaryContainer = Color(0xFF003A79),
    secondary = Color(0xFF5A8FD8), onSecondary = Color.White,
    background = Color(0xFFF5F5F7), onBackground = Color(0xFF151519),
    surface = Color.White, onSurface = Color(0xFF151519),
    surfaceVariant = Color(0xFFE9E9EE), onSurfaceVariant = Color(0xFF55555B)
)
private val IosDarkColors = darkColorScheme(
    primary = Color(0xFF62A4FF), onPrimary = Color(0xFF002F66),
    primaryContainer = Color(0xFF17477D), onPrimaryContainer = Color(0xFFD9E9FF),
    secondary = Color(0xFF9AC4FF), onSecondary = Color(0xFF12385F),
    background = Color(0xFF101014), onBackground = Color(0xFFF1F1F5),
    surface = Color(0xFF1B1B21), onSurface = Color(0xFFF1F1F5),
    surfaceVariant = Color(0xFF2A2A32), onSurfaceVariant = Color(0xFFC5C5CF)
)

private val CaveLightColors = lightColorScheme(
    primary = Color(0xFFF59E0B), onPrimary = Color(0xFF1C1917),
    primaryContainer = Color(0xFFFDE68A), onPrimaryContainer = Color(0xFF422006),
    secondary = Color(0xFF0D9488), onSecondary = Color.White,
    background = Color(0xFFFAFAF9), onBackground = Color(0xFF1C1917),
    surface = Color.White, onSurface = Color(0xFF1C1917),
    surfaceVariant = Color(0xFFF5F5F4), onSurfaceVariant = Color(0xFF57534E)
)
private val CaveDarkColors = darkColorScheme(
    primary = Color(0xFFFBBF24), onPrimary = Color(0xFF1C1917),
    primaryContainer = Color(0xFF5B3A08), onPrimaryContainer = Color(0xFFFDE68A),
    secondary = Color(0xFF5EEAD4), onSecondary = Color(0xFF042F2E),
    background = Color(0xFF1C1917), onBackground = Color(0xFFF5F5F4),
    surface = Color(0xFF292524), onSurface = Color(0xFFF5F5F4),
    surfaceVariant = Color(0xFF3B3633), onSurfaceVariant = Color(0xFFD6D3D1)
)

private val BaseTypography = Typography(
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp)
)

private fun typographyFor(family: FontFamily): Typography = Typography(
    displayLarge = BaseTypography.displayLarge.copy(fontFamily = family),
    displayMedium = BaseTypography.displayMedium.copy(fontFamily = family),
    displaySmall = BaseTypography.displaySmall.copy(fontFamily = family),
    headlineLarge = BaseTypography.headlineLarge.copy(fontFamily = family),
    headlineMedium = BaseTypography.headlineMedium.copy(fontFamily = family),
    headlineSmall = BaseTypography.headlineSmall.copy(fontFamily = family),
    titleLarge = BaseTypography.titleLarge.copy(fontFamily = family),
    titleMedium = BaseTypography.titleMedium.copy(fontFamily = family),
    titleSmall = BaseTypography.titleSmall.copy(fontFamily = family),
    bodyLarge = BaseTypography.bodyLarge.copy(fontFamily = family),
    bodyMedium = BaseTypography.bodyMedium.copy(fontFamily = family),
    bodySmall = BaseTypography.bodySmall.copy(fontFamily = family),
    labelLarge = BaseTypography.labelLarge.copy(fontFamily = family),
    labelMedium = BaseTypography.labelMedium.copy(fontFamily = family),
    labelSmall = BaseTypography.labelSmall.copy(fontFamily = family)
)

@Composable
fun SmartIslandAppTheme(
    repository: SmartIslandSettingsRepository,
    content: @Composable () -> Unit
) {
    val settings by repository.settings.collectAsStateWithLifecycle(initialValue = SmartIslandSettings.Default)
    SmartIslandTheme(
        appearancePreset = settings.appearancePreset,
        dynamicColor = settings.dynamicColorsEnabled,
        useCustomAccentColor = settings.useCustomAccentColor,
        accentColor = settings.appAccentColor,
        fontStyle = settings.fontStyle,
        fontScale = settings.fontScale,
        iconShape = settings.iconShape,
        content = content
    )
}

@Composable
fun SmartIslandTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    appearancePreset: String = SmartIslandSettings.THEME_MATERIAL_YOU,
    dynamicColor: Boolean = true,
    useCustomAccentColor: Boolean = false,
    accentColor: Long = 0xFFD84315L,
    fontStyle: String = SmartIslandSettings.FONT_SYSTEM,
    fontScale: Float = 1f,
    iconShape: String = SmartIslandSettings.ICON_ROUNDED,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val baseScheme = when (appearancePreset) {
        SmartIslandSettings.THEME_ONE_UI -> if (darkTheme) OneUiDarkColors else OneUiLightColors
        SmartIslandSettings.THEME_IOS -> if (darkTheme) IosDarkColors else IosLightColors
        SmartIslandSettings.THEME_CAVE -> if (darkTheme) CaveDarkColors else CaveLightColors
        else -> when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> BrandDarkColors
            else -> BrandLightColors
        }
    }
    val scheme = if (useCustomAccentColor) {
        val accent = Color(accentColor)
        val foreground = if (accent.luminance() > 0.58f) Color.Black else Color.White
        baseScheme.copy(
            primary = accent,
            onPrimary = foreground,
            primaryContainer = accent.copy(alpha = 0.22f),
            onPrimaryContainer = accent
        )
    } else baseScheme

    val shapes = when (appearancePreset) {
        SmartIslandSettings.THEME_ONE_UI -> Shapes(
            extraSmall = RoundedCornerShape(6.dp), small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(24.dp), extraLarge = RoundedCornerShape(32.dp)
        )
        SmartIslandSettings.THEME_IOS -> Shapes(
            extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(14.dp),
            medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(28.dp), extraLarge = RoundedCornerShape(36.dp)
        )
        else -> Shapes(
            extraSmall = RoundedCornerShape(4.dp), small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(12.dp), large = RoundedCornerShape(20.dp), extraLarge = RoundedCornerShape(28.dp)
        )
    }
    val iconFrame = when (iconShape) {
        SmartIslandSettings.ICON_CIRCLE -> CircleShape
        SmartIslandSettings.ICON_SQUARE -> RoundedCornerShape(4.dp)
        else -> RoundedCornerShape(14.dp)
    }
    val fontFamily = when (fontStyle) {
        SmartIslandSettings.FONT_SERIF -> FontFamily.Serif
        SmartIslandSettings.FONT_MONO -> FontFamily.Monospace
        else -> FontFamily.Default
    }
    val density = LocalDensity.current
    val safeFontScale = fontScale.coerceIn(SmartIslandSettings.MIN_FONT_SCALE, SmartIslandSettings.MAX_FONT_SCALE)

    CompositionLocalProvider(
        LocalSmartIslandIconShape provides iconFrame,
        LocalDensity provides density.copy(fontScale = density.fontScale * safeFontScale)
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = typographyFor(fontFamily),
            shapes = shapes,
            content = content
        )
    }
}
