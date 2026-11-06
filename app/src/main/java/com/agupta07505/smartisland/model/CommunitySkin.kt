/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */
package com.agupta07505.smartisland.model

import org.json.JSONArray
import org.json.JSONObject

/** A deliberately small, declarative community skin format. No external content is executed or loaded. */
data class CommunitySkin(val id: String, val name: String, val author: String, val license: String, val baseStyle: IslandSkin, val states: Map<String, CommunitySkinState>) {
    fun stateFor(state: String) = states[state] ?: states["idle"] ?: CommunitySkinState()
}
data class CommunitySkinState(val colors: List<Long> = emptyList(), val animation: CommunitySkinAnimation = CommunitySkinAnimation())
data class CommunitySkinAnimation(val type: String = "none", val speed: Float = 1f)
object CommunitySkinParser {
    const val MAX_BYTES = 24 * 1024
    private val states = setOf("idle", "music", "notification", "call", "battery", "navigation", "live_activity", "transfer", "hotspot", "bluetooth", "flashlight", "screen_recording", "timer", "stopwatch")
    private val animations = setOf("none", "pulse", "flow", "orbit")
    private val idPattern = Regex("[a-z0-9][a-z0-9_-]{0,63}")

    fun parse(json: String): Result<CommunitySkin> = runCatching {
        require(json.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "O arquivo excede 24 KB" }
        val root = JSONObject(json)
        require(root.keys().asSequence().toSet() == setOf("version", "id", "name", "author", "license", "baseStyle", "states")) { "Campos não permitidos ou ausentes" }
        require(root.get("version") is Int && root.getInt("version") == 1) { "Apenas schema versão 1 é aceito" }
        fun text(key: String): String = (root.get(key) as? String)?.also {
            require(it.isNotBlank() && it.length <= 80) { "$key inválido" }
            // Metadata is display-only: reject URL/path-like payloads instead of merely choosing not to open them.
            require(!it.contains(Regex("(?i)(https?://|www\\.|[\\\\/]|\\.\\.)"))) { "$key não pode conter URL ou caminho" }
        } ?: error("$key deve ser texto")

        val id = text("id").lowercase()
        require(idPattern.matches(id)) { "id inválido" }
        val base = IslandSkin.entries.firstOrNull { it.storageValue == text("baseStyle") } ?: error("baseStyle inválido")
        val sourceStates = root.get("states") as? JSONObject ?: error("states deve ser um objeto")
        require(sourceStates.length() in 1..states.size) { "states inválido" }
        val parsed = buildMap {
            sourceStates.keys().forEach { key ->
                require(key in states) { "Estado não permitido: $key" }
                val item = sourceStates.get(key) as? JSONObject ?: error("$key deve ser um objeto")
                require(item.keys().asSequence().toSet().all { it == "colors" || it == "animation" }) { "Campo de estado inválido" }
                val colors = if (item.has("colors")) {
                    val array = item.get("colors") as? JSONArray ?: error("colors deve ser uma lista")
                    require(array.length() in 1..4) { "colors deve ter 1 a 4 cores" }
                    List(array.length()) { index -> parseColor(array.get(index) as? String ?: error("Cor deve ser texto")) }
                } else emptyList()
                val animation = if (item.has("animation")) {
                    val animationObject = item.get("animation") as? JSONObject ?: error("animation deve ser um objeto")
                    require(animationObject.keys().asSequence().toSet() == setOf("type", "speed")) { "animation inválida" }
                    val type = animationObject.get("type") as? String ?: error("Tipo de animação inválido")
                    require(type in animations) { "Tipo de animação inválido" }
                    val speedNumber = animationObject.get("speed") as? Number ?: error("Velocidade inválida")
                    val speed = speedNumber.toFloat()
                    require(speed.isFinite() && speed in 0.25f..3f) { "Velocidade deve ser 0.25–3" }
                    CommunitySkinAnimation(type, speed)
                } else CommunitySkinAnimation()
                put(key, CommunitySkinState(colors, animation))
            }
        }
        require("idle" in parsed) { "O estado idle é obrigatório" }
        CommunitySkin(id, text("name"), text("author"), text("license"), base, parsed)
    }

    private fun parseColor(value: String): Long {
        require(Regex("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?").matches(value)) { "Cor deve usar #RRGGBB ou #AARRGGBB" }
        return value.drop(1).let { if (it.length == 6) ("FF$it").toLong(16) else it.toLong(16) }
    }
}
