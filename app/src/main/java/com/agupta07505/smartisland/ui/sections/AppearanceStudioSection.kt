/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.ui.LocalSmartIslandIconShape
import com.agupta07505.smartisland.ui.SliderSettingItem
import kotlinx.coroutines.launch

private val APP_ACCENTS = listOf(
    0xFF6750A4L, 0xFF3867B2L, 0xFF007AFFL, 0xFF0D9488L,
    0xFFF59E0BL, 0xFFDA4D32L, 0xFF8B5CF6L, 0xFFE84393L
)

@Composable
fun AppearanceStudioSection(settings: SmartIslandSettings, repository: SmartIslandSettingsRepository) {
    val scope = rememberCoroutineScope()
    var localFontScale by remember(settings.fontScale) { mutableFloatStateOf(settings.fontScale) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.appearance_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.appearance_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.appearance_theme_label), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        SmartIslandSettings.THEME_MATERIAL_YOU to R.string.theme_material_you,
                        SmartIslandSettings.THEME_ONE_UI to R.string.theme_one_ui,
                        SmartIslandSettings.THEME_IOS to R.string.theme_ios,
                        SmartIslandSettings.THEME_CAVE to R.string.theme_cave
                    ).chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { (preset, label) ->
                                FilterChip(
                                    selected = settings.appearancePreset == preset,
                                    onClick = { scope.launch { repository.setAppearancePreset(preset) } },
                                    label = { Text(stringResource(label)) }
                                )
                            }
                        }
                    }
                }
                SettingSwitchRow(
                    title = stringResource(R.string.dynamic_colors),
                    subtitle = stringResource(R.string.dynamic_colors_desc),
                    checked = settings.dynamicColorsEnabled,
                    enabled = settings.appearancePreset == SmartIslandSettings.THEME_MATERIAL_YOU,
                    onCheckedChange = { scope.launch { repository.setDynamicColorsEnabled(it) } }
                )
                SettingSwitchRow(
                    title = stringResource(R.string.custom_accent),
                    subtitle = stringResource(R.string.accent_color_label),
                    checked = settings.useCustomAccentColor,
                    onCheckedChange = { scope.launch { repository.setUseCustomAccentColor(it) } }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    APP_ACCENTS.forEach { color ->
                        val selected = settings.appAccentColor == color
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                                .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                .clickable {
                                    scope.launch {
                                        repository.setAppAccentColor(color)
                                        repository.setUseCustomAccentColor(true)
                                    }
                                }
                        )
                    }
                }
                Text(stringResource(R.string.font_style_label), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        SmartIslandSettings.FONT_SYSTEM to R.string.font_system,
                        SmartIslandSettings.FONT_SERIF to R.string.font_serif,
                        SmartIslandSettings.FONT_MONO to R.string.font_mono
                    ).forEach { (style, label) ->
                        FilterChip(
                            selected = settings.fontStyle == style,
                            onClick = { scope.launch { repository.setFontStyle(style) } },
                            label = { Text(stringResource(label)) }
                        )
                    }
                }
                SliderSettingItem(
                    label = stringResource(R.string.font_scale_label),
                    value = localFontScale * 100f,
                    range = SmartIslandSettings.MIN_FONT_SCALE * 100f..SmartIslandSettings.MAX_FONT_SCALE * 100f,
                    suffix = "%",
                    step = 5f,
                    onValueChange = { localFontScale = (it / 100f).coerceIn(SmartIslandSettings.MIN_FONT_SCALE, SmartIslandSettings.MAX_FONT_SCALE) },
                    onValueChangeFinished = { scope.launch { repository.setFontScale(localFontScale) } }
                )
                Text(stringResource(R.string.icon_shape_label), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        SmartIslandSettings.ICON_ROUNDED to R.string.icon_rounded,
                        SmartIslandSettings.ICON_CIRCLE to R.string.icon_circle,
                        SmartIslandSettings.ICON_SQUARE to R.string.icon_square
                    ).forEach { (shape, label) ->
                        FilterChip(
                            selected = settings.iconShape == shape,
                            onClick = { scope.launch { repository.setIconShape(shape) } },
                            label = { Text(stringResource(label)) }
                        )
                    }
                }
                IconShapePreview(settings.iconShape)
            }
        }

        // Existing island color and opacity controls live here so appearance settings stay together.
        CustomizationsSection(settings = settings, repository = repository)
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun IconShapePreview(style: String) {
    val frameShape = LocalSmartIslandIconShape.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.size(36.dp).clip(frameShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text("SI", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        Text("${stringResource(R.string.icon_shape_label)}: ${stringResource(when (style) {
            SmartIslandSettings.ICON_CIRCLE -> R.string.icon_circle
            SmartIslandSettings.ICON_SQUARE -> R.string.icon_square
            else -> R.string.icon_rounded
        })}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
