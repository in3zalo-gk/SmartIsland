/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFD84315),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEDE6),
    onPrimaryContainer = Color(0xFFBF360C),
    secondary = Color(0xFFBF360C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFCEBE6),
    onSecondaryContainer = Color(0xFF3E1204),
    tertiary = Color(0xFF0D9488),
    onTertiary = Color.White,
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF121212),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF121212),
    surfaceVariant = Color(0xFFF1F1F1),
    onSurfaceVariant = Color(0xFF5A5A5A),
    outline = Color(0xFFE0E0E0),
    outlineVariant = Color(0xFFEDEDED)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF8A65),
    onPrimary = Color(0xFF2B0B02),
    primaryContainer = Color(0xFF3D1307),
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = Color(0xFFFFAB91),
    onSecondary = Color(0xFF2B0B02),
    secondaryContainer = Color(0xFF2A1008),
    onSecondaryContainer = Color(0xFFFFCCBC),
    tertiary = Color(0xFF5EEAD4),
    onTertiary = Color(0xFF042F2E),
    background = Color(0xFF101113),
    onBackground = Color(0xFFF6F4F2),
    surface = Color(0xFF191A1D),
    onSurface = Color(0xFFF6F4F2),
    surfaceVariant = Color(0xFF292A2E),
    onSurfaceVariant = Color(0xFFC5C6CA),
    outline = Color(0xFF77787C),
    outlineVariant = Color(0xFF414247)
)

// Softer Material 3 shape/type scales inspired by CaveUI's component system.
private val SmartIslandShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val SmartIslandTypography = Typography(
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp)
)

@Composable
fun SmartIslandTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SmartIslandTypography,
        shapes = SmartIslandShapes,
        content = content
    )
}
