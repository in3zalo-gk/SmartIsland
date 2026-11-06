/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL v3 License Section 4 & Section 5
 */
package com.agupta07505.smartisland.ui.sections

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.PetMood
import com.agupta07505.smartisland.ui.IslandPetComposable
import com.agupta07505.smartisland.ui.bounceClick
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val PET_PRESET_COLORS = listOf(
    0xFFFF7043L to "Coral", 0xFF10B981L to "Emerald", 0xFF38BDF8L to "Sky",
    0xFF8B5CF6L to "Purple", 0xFFF59E0BL to "Amber", 0xFFEF4444L to "Red",
    0xFFEC4899L to "Pink", 0xFF6366F1L to "Indigo"
)
private val PET_PREVIEW_MOODS = listOf(
    PetMood.Idle, PetMood.Happy, PetMood.Excited, PetMood.Sleeping,
    PetMood.Charging, PetMood.Music, PetMood.LowBattery
)
private const val PET_PREVIEW_TAP_DURATION_MS = 900L

@Composable
fun PetSection(settings: SmartIslandSettings, repository: SmartIslandSettingsRepository) {
    val scope = rememberCoroutineScope()
    var selectedMood by remember { mutableStateOf(PetMood.Idle) }
    var displayedMood by remember { mutableStateOf(PetMood.Idle) }
    var resetJob by remember { mutableStateOf<Job?>(null) }
    val displayedLabel = stringResource(previewMoodLabel(displayedMood))

    fun showTapReaction() {
        resetJob?.cancel()
        displayedMood = PetMood.Tapped
        resetJob = scope.launch {
            delay(PET_PREVIEW_TAP_DURATION_MS)
            displayedMood = selectedMood
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PetPreviewCard(
            petColor = settings.petColor,
            displayedMood = displayedMood,
            displayedLabel = displayedLabel,
            selectedMood = selectedMood,
            onMoodSelected = { mood ->
                resetJob?.cancel()
                selectedMood = mood
                displayedMood = mood
            },
            onPetTapped = ::showTapReaction
        )

        PetPreferencesCard(
            enabled = settings.enablePet,
            petInsideIsland = settings.petInsideIsland,
            onEnabledChange = { value -> scope.launch { repository.setEnablePet(value) } },
            onInsideIslandChange = { value -> scope.launch { repository.setPetInsideIsland(value) } }
        )

        if (settings.enablePet) {
            PetColorCard(
                selectedColor = settings.petColor,
                onColorSelected = { value -> scope.launch { repository.setPetColor(value) } }
            )
        }
    }
}

@Composable
private fun PetPreviewCard(
    petColor: Long,
    displayedMood: PetMood,
    displayedLabel: String,
    selectedMood: PetMood,
    onMoodSelected: (PetMood) -> Unit,
    onPetTapped: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val previewDescription = stringResource(R.string.pet_preview_content_description, displayedLabel)
            // Fixed stage prevents state changes from moving the controls below it.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 128.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(Color(petColor).copy(alpha = .14f))
                    .clickable(
                        onClickLabel = stringResource(R.string.pet_preview_tap_action),
                        onClick = onPetTapped
                    )
                    .semantics { stateDescription = previewDescription },
                contentAlignment = Alignment.Center
            ) {
                IslandPetComposable(
                    mood = displayedMood,
                    petColor = petColor,
                    modifier = Modifier.size(64.dp),
                    onTap = onPetTapped
                )
            }
            Text(
                text = stringResource(R.string.pet_preview_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.pet_preview_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center
            )
            AssistChip(
                onClick = {},
                enabled = false,
                label = { Text(stringResource(R.string.pet_preview_showing_state, displayedLabel)) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = .12f))
            Text(
                text = stringResource(R.string.pet_preview_choose_state),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PET_PREVIEW_MOODS.forEach { mood ->
                    FilterChip(
                        selected = selectedMood == mood,
                        onClick = { onMoodSelected(mood) },
                        label = { Text(stringResource(previewMoodLabel(mood))) },
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                }
            }
            Text(
                text = stringResource(R.string.pet_preview_local_only),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = .8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PetPreferencesCard(
    enabled: Boolean,
    petInsideIsland: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onInsideIslandChange: (Boolean) -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            PetSettingRow(
                title = "Enable Pet",
                description = "Show your companion with the island",
                checked = enabled,
                onCheckedChange = onEnabledChange,
                showIcon = true
            )
            HorizontalDivider(modifier = Modifier.padding(start = 68.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f))
            PetSettingRow(
                title = "Floating pet",
                description = "Show a draggable pet outside the island (requires the overlay touch hook)",
                checked = !petInsideIsland,
                enabled = enabled,
                onCheckedChange = { floating -> onInsideIslandChange(!floating) }
            )
        }
    }
}

@Composable
private fun PetSettingRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean = true,
    showIcon: Boolean = false,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showIcon) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Pets, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PetColorCard(selectedColor: Long, onColorSelected: (Long) -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("PET COLOR", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
            Text("Choose a color that remains clear over your island.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PET_PRESET_COLORS.forEach { (colorValue, colorName) ->
                    val selected = selectedColor == colorValue
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .semantics {
                                contentDescription = colorName
                                this.selected = selected
                            }
                            .then(
                                if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            )
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(Color(colorValue))
                            .bounceClick { onColorSelected(colorValue) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) Text("✓", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun previewMoodLabel(mood: PetMood): Int = when (mood) {
    PetMood.Idle -> R.string.pet_preview_mood_idle
    PetMood.Happy -> R.string.pet_preview_mood_happy
    PetMood.Excited -> R.string.pet_preview_mood_excited
    PetMood.Sleeping -> R.string.pet_preview_mood_sleeping
    PetMood.Charging -> R.string.pet_preview_mood_charging
    PetMood.Music -> R.string.pet_preview_mood_music
    PetMood.LowBattery -> R.string.pet_preview_mood_low_battery
    PetMood.Tapped -> R.string.pet_preview_mood_tapped
    else -> R.string.pet_preview_mood_idle
}
