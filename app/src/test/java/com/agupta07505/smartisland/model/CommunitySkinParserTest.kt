package com.agupta07505.smartisland.model

import org.junit.Assert.*
import org.junit.Test

class CommunitySkinParserTest {
    private fun json(states: String = "\"idle\":{\"colors\":[\"#112233\"],\"animation\":{\"type\":\"none\",\"speed\":1}}") =
        """{"version":1,"id":"test-skin","name":"Test","author":"A","license":"MIT","baseStyle":"classic","states":{$states}}"""

    @Test fun parsesAndFallsBackToIdle() {
        val skin = CommunitySkinParser.parse(json()).getOrThrow()
        assertEquals("test-skin", skin.id)
        assertEquals(skin.stateFor("idle"), skin.stateFor("music"))
        assertEquals(IslandSkin.Classic, skin.baseStyle)
        assertEquals(0xFF112233L, skin.stateFor("idle").colors.single())
    }

    @Test fun rejectsInvalidSchemaAndColor() {
        assertTrue(CommunitySkinParser.parse(json().replace("\"version\":1", "\"version\":2")).isFailure)
        assertTrue(CommunitySkinParser.parse(json().replace("#112233", "red")).isFailure)
        assertTrue(CommunitySkinParser.parse(json("\"unknown\":{} ")).isFailure)
        assertTrue(CommunitySkinParser.parse(json().replace("\"speed\":1", "\"speed\":9")).isFailure)
    }

    @Test fun rejectsUnknownFieldsUrlsAndPaths() {
        assertTrue(CommunitySkinParser.parse(json().replace("\"states\":", "\"url\":\"https://bad.example\",\"states\":")).isFailure)
        assertTrue(CommunitySkinParser.parse(json().replace("\"name\":\"Test\"", "\"name\":\"https://bad.example\"")).isFailure)
        assertTrue(CommunitySkinParser.parse(json().replace("\"author\":\"A\"", "\"author\":\"../../files\"")).isFailure)
        assertTrue(CommunitySkinParser.parse(json("\"idle\":{\"script\":\"alert(1)\"}")).isFailure)
    }

    @Test fun requiresIdleAndNeverOverflows() {
        assertTrue(CommunitySkinParser.parse(json("\"music\":{}")).isFailure)
        assertTrue(CommunitySkinParser.parse(json().replace("\"speed\":1", "\"speed\":1e1000")).isFailure)
        assertTrue(CommunitySkinParser.parse("x".repeat(CommunitySkinParser.MAX_BYTES + 1)).isFailure)
    }

    @Test fun acceptsAllActivityStatesAndFourColors() {
        val names = listOf("idle", "music", "notification", "call", "battery", "navigation", "live_activity", "transfer", "hotspot", "bluetooth", "flashlight", "screen_recording", "timer", "stopwatch")
        val states = names.joinToString(",") { "\"$it\":{\"colors\":[\"#112233\",\"#80112233\",\"#FFFFFF\",\"#000000\"]}" }
        val skin = CommunitySkinParser.parse(json(states)).getOrThrow()
        assertEquals(names.toSet(), skin.states.keys)
        assertEquals(4, skin.stateFor("music").colors.size)
        assertEquals(0x80112233L, skin.stateFor("music").colors[1])
    }

    @Test fun rejectsWrongJsonTypesInsteadOfCoercing() {
        assertTrue(CommunitySkinParser.parse(json().replace("\"version\":1", "\"version\":\"1\"")).isFailure)
        assertTrue(CommunitySkinParser.parse(json().replace("\"name\":\"Test\"", "\"name\":42")).isFailure)
        assertTrue(CommunitySkinParser.parse(json().replace("\"colors\":[\"#112233\"]", "\"colors\":{} ")).isFailure)
        assertTrue(CommunitySkinParser.parse(json().replace("\"speed\":1", "\"speed\":\"1\"")).isFailure)
    }

    @Test fun rejectsUnsafeIdsAndInvalidPaletteLengths() {
        assertTrue(CommunitySkinParser.parse(json().replace("test-skin", "../escape")).isFailure)
        assertTrue(CommunitySkinParser.parse(json().replace("test-skin", "-starts-wrong")).isFailure)
        assertTrue(CommunitySkinParser.parse(json().replace("test-skin", "a".repeat(65))).isFailure)
        assertTrue(CommunitySkinParser.parse(json("\"idle\":{\"colors\":[]}")).isFailure)
        assertTrue(CommunitySkinParser.parse(json("\"idle\":{\"colors\":[\"#000000\",\"#000000\",\"#000000\",\"#000000\",\"#000000\"]}")).isFailure)
    }

    @Test fun acceptsSpeedBoundariesAndRejectsOutsideRange() {
        for (speed in listOf("0.25", "3.0")) {
            assertTrue(CommunitySkinParser.parse(json().replace("\"speed\":1", "\"speed\":$speed")).isSuccess)
        }
        for (speed in listOf("0", "-1", "0.249", "3.001")) {
            assertTrue(CommunitySkinParser.parse(json().replace("\"speed\":1", "\"speed\":$speed")).isFailure)
        }
    }
}
