/* Smart Island (2026) © Animesh Gupta — GPL-3.0-or-later */
package com.agupta07505.smartisland.community

import android.content.Context
import com.agupta07505.smartisland.model.CommunitySkinParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

class CommunitySkinRepository(context: Context) {
    private val dir = File(context.filesDir, "community_skins").also { it.mkdirs() }
    private val assets = context.assets

    /**
     * Copies the first-party catalogue once. The marker is intentionally separate from the JSON files:
     * a skin removed by its owner must stay removed, and an imported skin with the same id wins.
     */
    suspend fun installBundledSkinsIfNeeded(): Result<Int> = withContext(Dispatchers.IO) { runCatching {
        val marker = File(dir, BUNDLED_MARKER)
        if (marker.exists()) return@runCatching 0
        var installed = 0
        BUNDLED_SKIN_FILES.forEach { fileName ->
            val json = assets.open("$BUNDLED_ASSET_DIRECTORY/$fileName").use { input ->
                val bytes = input.readBoundedBytes(CommunitySkinParser.MAX_BYTES + 1)
                require(bytes.size <= CommunitySkinParser.MAX_BYTES) { "Skin integrada grande demais: $fileName" }
                bytes.toString(Charsets.UTF_8)
            }
            val skin = CommunitySkinParser.parse(json).getOrElse { error("Skin integrada inválida: $fileName") }
            val target = File(dir, "${skin.id}.json")
            if (!target.exists()) {
                writeAtomically(target, json)
                installed++
            }
        }
        // Write only after every asset is valid and considered, so a failed upgrade can be retried safely.
        marker.outputStream().use { it.write("1".toByteArray()) }
        installed
    } }

    suspend fun import(json: String, replace: Boolean = false): Result<File> = withContext(Dispatchers.IO) {
        CommunitySkinParser.parse(json).mapCatching { skin ->
            val target = File(dir, "${skin.id}.json")
            require(!target.exists() || replace) { "Já existe uma skin com este id" }
            require(target.exists() || (dir.listFiles()?.count { it.extension == "json" } ?: 0) < MAX_SKINS) { "Limite de $MAX_SKINS skins atingido" }
            writeAtomically(target, json)
            target
        }
    }

    suspend fun list(): List<File> = withContext(Dispatchers.IO) {
        dir.listFiles()?.filter { it.extension == "json" && it.length() in 1..CommunitySkinParser.MAX_BYTES.toLong() }
            ?.sortedBy { it.name } ?: emptyList()
    }

    suspend fun read(id: String): Result<String> = withContext(Dispatchers.IO) { runCatching {
        require(Regex("[a-z0-9][a-z0-9_-]{0,63}").matches(id))
        val file = File(dir, "$id.json")
        require(file.length() in 1..CommunitySkinParser.MAX_BYTES.toLong()) { "Arquivo inválido ou grande demais" }
        file.inputStream().use { input ->
            val bytes = input.readBoundedBytes(CommunitySkinParser.MAX_BYTES + 1)
            require(bytes.size <= CommunitySkinParser.MAX_BYTES) { "Arquivo grande demais" }
            bytes.toString(Charsets.UTF_8)
        }
    } }

    suspend fun remove(id: String): Boolean = withContext(Dispatchers.IO) {
        Regex("[a-z0-9][a-z0-9_-]{0,63}").matches(id) && File(dir, "$id.json").delete()
    }

    private fun writeAtomically(target: File, json: String) {
        val temp = File(dir, ".${target.name}.tmp")
        temp.outputStream().bufferedWriter(Charsets.UTF_8).use { it.write(json) }
        if (!temp.renameTo(target)) {
            temp.delete()
            error("Não foi possível salvar a skin")
        }
    }

    companion object {
        const val BUNDLED_ASSET_DIRECTORY = "community_skins"
        internal const val BUNDLED_MARKER = ".bundled-v1-installed"
        internal val BUNDLED_SKIN_FILES = listOf(
            "obsidian.json", "titanium.json", "black-hole.json", "eclipse.json", "lightspeed.json", "saturn.json"
        )
        private const val MAX_SKINS = 64
    }
}

/** Reads at most [limit] bytes without relying on Java 9's InputStream.readNBytes. */
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
