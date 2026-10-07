/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 */
package com.agupta07505.smartisland.ui.sections

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.data.ai.AiChatTurn
import com.agupta07505.smartisland.data.ai.AiKeyVault
import com.agupta07505.smartisland.data.ai.AiProvider
import com.agupta07505.smartisland.data.ai.AiProviderClient
import com.agupta07505.smartisland.ui.SliderSettingItem
import kotlinx.coroutines.launch

@Composable
fun IslandStudioSection(settings: SmartIslandSettings, repository: SmartIslandSettingsRepository) {
    var expanded by remember { mutableStateOf(false) }
    var widthScale by remember(settings.expandedWidthScale) { mutableFloatStateOf(settings.expandedWidthScale) }
    var radius by remember(settings.expandedCornerRadius) { mutableFloatStateOf(settings.expandedCornerRadius) }
    val scope = rememberCoroutineScope()
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Live island preview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Preview both compact and expanded states. This is a visual sample; actual content depends on notifications and active modes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(Modifier.fillMaxWidth().height(148.dp).clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.TopCenter) {
                    Box(Modifier.padding(top = 16.dp).size(width = if (expanded) 260.dp else 132.dp, height = if (expanded) 92.dp else 36.dp).clip(RoundedCornerShape(if (expanded) radius.dp else settings.cornerRadius.dp)).background(Color(settings.pillColor).copy(alpha = settings.opacity)), contentAlignment = Alignment.Center) {
                        Text(if (expanded) "♪  Now playing     02:34" else "●  Smart Island", color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !expanded, onClick = { expanded = false }, label = { Text("Compact") })
                    FilterChip(selected = expanded, onClick = { expanded = true }, label = { Text("Expanded") })
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Expanded shape", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                SliderSettingItem("Expanded width", widthScale * 100f, 72f..100f, { widthScale = it / 100f }, suffix = "%", step = 2f, onValueChangeFinished = { scope.launch { repository.setExpandedWidthScale(widthScale) } })
                SliderSettingItem("Expanded corner radius", radius, SmartIslandSettings.MIN_EXPANDED_CORNER_RADIUS..SmartIslandSettings.MAX_EXPANDED_CORNER_RADIUS, { radius = it }, suffix = "dp", step = 1f, onValueChangeFinished = { scope.launch { repository.setExpandedCornerRadius(radius) } })
            }
        }
        PositionsSection(settings = settings, repository = repository)
    }
}

@Composable
fun AnimationStudioSection(settings: SmartIslandSettings, repository: SmartIslandSettingsRepository) {
    val scope = rememberCoroutineScope()
    var duration by remember(settings.animationDurationMs) { mutableFloatStateOf(settings.animationDurationMs.toFloat()) }
    var speed by remember(settings.animationSpeed) { mutableFloatStateOf(settings.animationSpeed) }
    var demoExpanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Motion studio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Choose the transition feel, then adjust its speed and duration.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(SmartIslandSettings.ANIMATION_STYLE_GENTLE to "Gentle", SmartIslandSettings.ANIMATION_STYLE_SPRING to "Spring", SmartIslandSettings.ANIMATION_STYLE_BOUNCY to "Bouncy").forEach { (style, label) ->
                        FilterChip(selected = settings.animationStyle == style, onClick = { scope.launch { repository.setAnimationStyle(style) } }, label = { Text(label) })
                    }
                }
                SliderSettingItem("Duration", duration, SmartIslandSettings.MIN_ANIMATION_DURATION_MS.toFloat()..SmartIslandSettings.MAX_ANIMATION_DURATION_MS.toFloat(), { duration = it }, suffix = "ms", step = 20f, onValueChangeFinished = { scope.launch { repository.setAnimationDurationMs(duration.toInt()) } })
                SliderSettingItem("Speed", speed * 100f, SmartIslandSettings.MIN_ANIMATION_SPEED * 100f..SmartIslandSettings.MAX_ANIMATION_SPEED * 100f, { speed = it / 100f }, suffix = "%", step = 5f, onValueChangeFinished = { scope.launch { repository.setAnimationSpeed(speed) } })
                Box(Modifier.fillMaxWidth().height(112.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(width = if (demoExpanded) 220.dp else 110.dp, height = if (demoExpanded) 68.dp else 34.dp).clip(RoundedCornerShape(if (demoExpanded) settings.expandedCornerRadius.dp else settings.cornerRadius.dp)).background(Color(settings.pillColor)), contentAlignment = Alignment.Center) { Text(if (demoExpanded) "Expanded" else "Compact", color = Color.White) }
                }
                OutlinedButton(onClick = { demoExpanded = !demoExpanded }, modifier = Modifier.fillMaxWidth()) { Text("Replay preview") }
            }
        }
    }
}

@Composable
fun AiStudioSection(settings: SmartIslandSettings, onProviderChange: (AiProvider) -> Unit, onModelChange: (String) -> Unit, context: Context) {
    val provider = AiProvider.fromId(settings.aiProvider)
    val scope = rememberCoroutineScope()
    val vault = remember(context) { AiKeyVault(context) }
    var keyInput by remember(provider.id) { mutableStateOf("") }
    var keySaved by remember(provider.id) { mutableStateOf(vault.hasKey(provider.id)) }
    var prompt by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    val turns = remember { mutableStateListOf<AiChatTurn>() }
    var error by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("AI mode", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Free-form chat with your chosen provider. Enter your API key here in the app; it is encrypted on this device and never stored in the settings backup. Only the messages you type are sent. Providers may charge for API use.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    AiProvider.entries.forEach { item -> FilterChip(selected = item == provider, onClick = { onProviderChange(item); keyInput = ""; keySaved = vault.hasKey(item.id) }, label = { Text(item.label.substringAfter("/ ")) }) }
                }
                OutlinedTextField(value = settings.aiModel, onValueChange = onModelChange, label = { Text("Model ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = keyInput, onValueChange = { keyInput = it }, label = { Text(if (keySaved) "API key saved · enter to replace" else "API key") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None), modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { runCatching { vault.save(provider.id, keyInput) }.onSuccess { keyInput = ""; keySaved = true }.onFailure { error = "Could not save key: ${it.message}" } }, enabled = keyInput.isNotBlank()) { Text("Save key") }
                    OutlinedButton(onClick = { vault.clear(provider.id); keyInput = ""; keySaved = false; error = null }) { Text("Remove key") }
                    Text(if (keySaved) "Key stored" else "No key", style = MaterialTheme.typography.labelMedium, modifier = Modifier.align(Alignment.CenterVertically), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Chat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (turns.isEmpty()) Text("Conversation is temporary and cleared when you leave this screen.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                turns.forEach { turn ->
                    Text(if (turn.role == "user") "You: ${turn.text}" else "${provider.label}: ${turn.text}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(10.dp))
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                OutlinedTextField(value = prompt, onValueChange = { prompt = it }, label = { Text("Message") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    val message = prompt.trim()
                    if (message.isNotEmpty() && !sending) {
                        val apiKey = vault.load(provider.id)
                        if (apiKey.isNullOrBlank()) { error = "Save an API key for ${provider.label} first." }
                        else {
                            prompt = ""; error = null
                            turns.add(AiChatTurn("user", message)); sending = true
                            scope.launch {
                                try { turns.add(AiChatTurn("assistant", AiProviderClient.send(provider, settings.aiModel, turns.toList(), apiKey))) }
                                catch (e: Exception) { error = e.message ?: "AI request failed." }
                                finally { sending = false }
                            }
                        }
                    }
                }, enabled = !sending && prompt.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(if (sending) "Sending…" else "Send") }
            }
        }
    }
}
