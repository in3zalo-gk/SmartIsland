/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded.lyrics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Small, stateless LRCLIB client. It only runs when the user explicitly requests a lyric search.
 * No query or response is persisted and LRCLIB does not require credentials for this endpoint.
 */
object LrcLibLyricsRepository {
    private const val CONNECT_TIMEOUT_MS = 5_000
    private const val READ_TIMEOUT_MS = 8_000
    private const val REQUEST_TIMEOUT_MS = 10_000L
    private const val USER_AGENT = "SmartIsland/7.0 (lyrics lookup)"

    suspend fun fetchSyncedLyrics(artist: String, track: String): LyricsLoadState {
        if (artist.isBlank() || track.isBlank()) return LyricsLoadState.NotFound

        return try {
            withTimeout(REQUEST_TIMEOUT_MS) {
                withContext(Dispatchers.IO) {
                    coroutineContext.ensureActive()
                    val response = get(artist.trim(), track.trim())
                    coroutineContext.ensureActive()
                    when (response.code) {
                        HttpURLConnection.HTTP_NOT_FOUND -> LyricsLoadState.NotFound
                        in 200..299 -> parseResponse(response.body)
                        else -> LyricsLoadState.Error(
                            IllegalStateException("LRCLIB respondeu HTTP ${response.code}")
                        )
                    }
                }
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            LyricsLoadState.Error(error)
        }
    }

    private fun parseResponse(body: String): LyricsLoadState {
        val json = JSONObject(body)
        if (json.optBoolean("instrumental", false)) return LyricsLoadState.Instrumental

        val syncedLyrics = json.optString("syncedLyrics").trim()
        if (syncedLyrics.isBlank()) return LyricsLoadState.NotFound

        val lines = LrcParser.parse(syncedLyrics)
        return if (lines.isEmpty()) LyricsLoadState.NotFound else LyricsLoadState.Available(lines)
    }

    private fun get(artist: String, track: String): HttpResponse {
        val url = URL(
            "https://lrclib.net/api/get?artist_name=${encode(artist)}&track_name=${encode(track)}"
        )
        val connection = (url.openConnection() as HttpURLConnection)
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", USER_AGENT)

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            HttpResponse(code, stream?.bufferedReader()?.use { it.readText() }.orEmpty())
        } finally {
            connection.disconnect()
        }
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private data class HttpResponse(val code: Int, val body: String)
}
