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

private val PET_PRESET_COLORS = listOf(0xFFFF7043L to "Coral", 0xFF10B981L to "Emerald", 0xFF38BDF8L to "Sky", 0xFF8B5CF6L to "Purple", 0xFFF59E0BL to "Amber", 0xFFEF4444L to "Red", 0xFFEC4899L to "Pink", 0xFF6366F1L to "Indigo")
private val PET_PREVIEW_MOODS = listOf(PetMood.Idle, PetMood.Happy, PetMood.Excited, PetMood.Sleeping, PetMood.Charging, PetMood.Music, PetMood.LowBattery)
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
  resetJob = scope.launch { delay(PET_PREVIEW_TAP_DURATION_MS); displayedMood = selectedMood }
 }
 Column(Modifier.fillMaxWidth().animateContentSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
  Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .35f)), CardDefaults.cardElevation(defaultElevation = 0.dp)) {
   Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
    val previewDescription = stringResource(R.string.pet_preview_content_description, displayedLabel)
    Box(Modifier.size(80.dp).clip(RoundedCornerShape(20.dp)).background(Color(settings.petColor).copy(alpha = .1f)).clickable(onClickLabel = stringResource(R.string.pet_preview_tap_action)) { showTapReaction() }.semantics { stateDescription = previewDescription }, contentAlignment = Alignment.Center) {
     IslandPetComposable(displayedMood, settings.petColor, Modifier.size(48.dp), onTap = { showTapReaction() })
    }
    Text(stringResource(R.string.pet_preview_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Text(stringResource(R.string.pet_preview_description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer, shape = RoundedCornerShape(50)) { Text(stringResource(R.string.pet_preview_showing_state, displayedLabel), Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium) }
    Text(stringResource(R.string.pet_preview_choose_state), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
     PET_PREVIEW_MOODS.forEach { mood -> FilterChip(selectedMood == mood, { resetJob?.cancel(); selectedMood = mood; displayedMood = mood }, { Text(stringResource(previewMoodLabel(mood))) }, Modifier.heightIn(min = 48.dp)) }
    }
    Text(stringResource(R.string.pet_preview_local_only), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
   }
  }
  Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .35f)), CardDefaults.cardElevation(defaultElevation = 0.dp)) {
   Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
     Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = .12f)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Pets, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) }
      Column { Text("Enable Pet", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold); Text("Show the pet inside the island", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
     }
     Switch(settings.enablePet, { scope.launch { repository.setEnablePet(it) } }, colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = .3f)))
    }
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
     Column(Modifier.weight(1f)) { Text("Pet Inside Island", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold); Text("Show pet in the corner when expanded", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
     Switch(settings.petInsideIsland, settings.enablePet, { scope.launch { repository.setPetInsideIsland(it) } }, colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = .3f)))
    }
   }
  }
  if (settings.enablePet) Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .35f)), CardDefaults.cardElevation(defaultElevation = 0.dp)) {
   Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text("PET COLOR", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = .8.sp)
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { PET_PRESET_COLORS.forEach { (colorValue, _) ->
     val selected = settings.petColor == colorValue
     Box(Modifier.size(40.dp).then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier.border(.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .3f), CircleShape)).padding(2.dp).clip(CircleShape).background(Color(colorValue)).bounceClick { scope.launch { repository.setPetColor(colorValue) } }, contentAlignment = Alignment.Center) { if (selected) Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
    } }
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
