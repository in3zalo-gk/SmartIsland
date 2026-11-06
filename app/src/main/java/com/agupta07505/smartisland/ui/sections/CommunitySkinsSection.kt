/* Smart Island (2026) © Animesh Gupta — GPL-3.0-or-later */
package com.agupta07505.smartisland.ui.sections

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.community.CommunitySkinRepository
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.CommunitySkin
import com.agupta07505.smartisland.model.CommunitySkinParser
import com.agupta07505.smartisland.ui.communitySkin
import com.agupta07505.smartisland.util.SystemAnimScale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/** Self-contained section; parent Settings navigation supplies the tab. */
@Composable fun CommunitySkinsSection(settings: SmartIslandSettings, repository: SmartIslandSettingsRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val files = remember { mutableStateListOf<java.io.File>() }
    var candidate by remember { mutableStateOf<CommunitySkin?>(null) }
    var raw by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var replace by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    var previewState by remember { mutableStateOf("idle") }
    val library = remember { CommunitySkinRepository(context) }
    fun refresh() { scope.launch { files.clear(); files.addAll(library.list()) } }
    LaunchedEffect(Unit) {
        // This is the catalogue's first access. The repository marker makes it one-time and preserves removals.
        library.installBundledSkinsIfNeeded()
        files.clear()
        files.addAll(library.list())
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val text = try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        val bytes = input.readBoundedBytes(CommunitySkinParser.MAX_BYTES + 1)
                        if (bytes.size > CommunitySkinParser.MAX_BYTES) null else bytes.toString(Charsets.UTF_8)
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                message = "Não foi possível ler o arquivo selecionado"
                null
            }
            if (text == null) {
                if (message == null) message = "Não foi possível ler o arquivo ou ele excede 24 KB"
            } else {
                CommunitySkinParser.parse(text).onSuccess { candidate = it; raw = text; replace = false }
                    .onFailure { message = it.message ?: "JSON inválido" }
            }
        }
    }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Skins da comunidade", style = MaterialTheme.typography.titleLarge); Text("Formato local e declarativo; sem scripts, links, SVG ou arquivos externos.", style = MaterialTheme.typography.bodySmall) }
        item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { Button({ open.launch(arrayOf("application/json", "text/json", "text/plain")) }) { Text("Importar JSON") }; OutlinedButton({ scope.launch { repository.setCommunitySkinJson("") } }) { Text("Usar padrão integrado") } } }
        item {
            Text("Prévia visual", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("idle", "music", "notification", "call").forEach { state ->
                    FilterChip(selected = previewState == state, onClick = { previewState = state }, label = { Text(state) })
                }
            }
            SkinVisualPreview(candidate, previewState)
        }
        items(files.size) { i ->
            val file = files[i]
            Card { Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(file.nameWithoutExtension, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Row {
                    TextButton({ scope.launch { library.read(file.nameWithoutExtension).getOrNull()?.let { repository.setCommunitySkinJson(it) } } }) { Text("Aplicar") }
                    TextButton({ scope.launch {
                        library.remove(file.nameWithoutExtension)
                        val selectedId = CommunitySkinParser.parse(settings.communitySkinJson).getOrNull()?.id
                        if (selectedId == file.nameWithoutExtension) repository.setCommunitySkinJson("")
                        refresh()
                    } }) { Text("Remover") }
                }
            } }
        }
    }
    candidate?.let { skin -> AlertDialog(
        onDismissRequest = { if (!importing) candidate = null },
        title = { Text("Importar ${skin.name}?") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { SkinVisualPreview(skin, previewState); Text("Por ${skin.author} • ${skin.license}\nID: ${skin.id}") } },
        confirmButton = { TextButton(enabled = !importing, onClick = { importing = true; scope.launch { library.import(raw, replace).onSuccess { repository.setCommunitySkinJson(raw); candidate = null; refresh() }.onFailure { if (it.message?.contains("Já existe") == true) replace = true else message = it.message }; importing = false } }) { Text(if (importing) "Salvando…" else if (replace) "Substituir e aplicar" else "Salvar e aplicar") } },
        dismissButton = { TextButton(enabled = !importing, onClick = { candidate = null }) { Text("Cancelar") } }
    ) }
    message?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show(); message = null }
}

@Composable private fun SkinVisualPreview(skin: CommunitySkin?, state: String) {
    val available = skin?.states?.keys?.sorted().orEmpty()
    val resolved = state.takeIf { it in available } ?: "idle"
    Box(Modifier.fillMaxWidth().height(72.dp).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxWidth(.88f).height(48.dp).communitySkin(skin, resolved, reducedMotion = SystemAnimScale.animationsOff()).padding(horizontal = 18.dp), contentAlignment = Alignment.CenterStart) {
            Text(if (state == "call") "Chamada de exemplo" else "Atividade de exemplo", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = .82f)).padding(horizontal = 6.dp, vertical = 2.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Reads at most [limit] bytes without Java 9-only InputStream.readNBytes. */
private fun InputStream.readBoundedBytes(limit: Int): ByteArray {
    val output = ByteArrayOutputStream(limit)
    val buffer = ByteArray(minOf(4 * 1024, limit))
    var remaining = limit
    while (remaining > 0) {
        val read = read(buffer, 0, minOf(buffer.size, remaining))
        if (read < 0) break
        if (read == 0) continue
        output.write(buffer, 0, read)
        remaining -= read
    }
    return output.toByteArray()
}
