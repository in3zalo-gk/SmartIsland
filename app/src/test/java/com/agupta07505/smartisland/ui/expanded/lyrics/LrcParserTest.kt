package com.agupta07505.smartisland.ui.expanded.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LrcParserTest {
    @Test
    fun parse_expandsMultipleTimestamps_andSortsLines() {
        val lines = LrcParser.parse(
            """
            [00:10.50][00:20.500]Refrão
            [00:05.2]Verso
            """.trimIndent()
        )

        assertEquals(
            listOf(
                LyricLine(5_200, "Verso"),
                LyricLine(10_500, "Refrão"),
                LyricLine(20_500, "Refrão")
            ),
            lines
        )
    }

    @Test
    fun parse_ignoresMetadataAndInvalidTimestamps() {
        val lines = LrcParser.parse(
            """
            [ar:Artista]
            [99:99.00]Inválida
            sem timestamp
            [01:02]Válida
            """.trimIndent()
        )

        assertEquals(listOf(LyricLine(62_000, "Válida")), lines)
    }

    @Test
    fun activeLineIndex_selectsLatestLineAtOrBeforePlayback() {
        val lines = listOf(
            LyricLine(1_000, "um"),
            LyricLine(2_000, "dois"),
            LyricLine(3_000, "três")
        )

        assertNull(LrcParser.activeLineIndex(lines, null))
        assertNull(LrcParser.activeLineIndex(lines, 500))
        assertEquals(1, LrcParser.activeLineIndex(lines, 2_750))
        assertEquals(2, LrcParser.activeLineIndex(lines, 3_000))
    }
}
