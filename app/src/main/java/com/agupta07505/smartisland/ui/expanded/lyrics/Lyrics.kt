/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded.lyrics

/** A timestamped line supplied by an LRC-compatible lyrics provider. */
data class LyricLine(
    val timestampMs: Long,
    val text: String
)

sealed interface LyricsLoadState {
    data object Idle : LyricsLoadState
    data object Loading : LyricsLoadState
    data class Available(val lines: List<LyricLine>) : LyricsLoadState
    data object Instrumental : LyricsLoadState
    data object NotFound : LyricsLoadState
    data class Error(val cause: Throwable) : LyricsLoadState
}

/**
 * Pure LRC parsing utilities kept independent of Android/network code so timestamps remain JVM-testable.
 */
object LrcParser {
    private val timestampPattern = Regex("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]")

    fun parse(source: String): List<LyricLine> = source
        .lineSequence()
        .flatMap { rawLine ->
            val matches = timestampPattern.findAll(rawLine).toList()
            if (matches.isEmpty()) {
                emptySequence()
            } else {
                val text = rawLine.substring(matches.last().range.last + 1).trim()
                matches.asSequence().mapNotNull { match ->
                    timestampMs(match)?.let { timestamp -> LyricLine(timestamp, text) }
                }
            }
        }
        .sortedBy { it.timestampMs }
        .toList()

    fun activeLineIndex(lines: List<LyricLine>, playbackPositionMs: Long?): Int? {
        if (playbackPositionMs == null || lines.isEmpty()) return null
        val index = lines.indexOfLast { it.timestampMs <= playbackPositionMs }
        return index.takeIf { it >= 0 }
    }

    private fun timestampMs(match: MatchResult): Long? {
        val minutes = match.groupValues[1].toLongOrNull() ?: return null
        val seconds = match.groupValues[2].toLongOrNull() ?: return null
        if (seconds !in 0..59) return null
        val fraction = match.groupValues[3]
        val milliseconds = when (fraction.length) {
            0 -> 0L
            1 -> fraction.toLongOrNull()?.times(100) ?: return null
            2 -> fraction.toLongOrNull()?.times(10) ?: return null
            else -> fraction.take(3).toLongOrNull() ?: return null
        }
        return minutes * 60_000L + seconds * 1_000L + milliseconds
    }
}
