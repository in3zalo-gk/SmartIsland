/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL v3 License Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetScreenBoundsTest {
    private val bounds = PetScreenBounds(width = 1080f, height = 2400f, petSize = 144f)

    @Test fun defaultPositionIsVisibleOnScreen() {
        val position = bounds.defaultPosition()

        assertTrue(position.x >= 0f && position.x <= 936f)
        assertTrue(position.y >= 0f && position.y <= 2256f)
    }

    @Test fun clampKeepsPetCompletelyInsideScreen() {
        val position = bounds.clamp(PetScreenPosition(x = -50f, y = 9_999f))

        assertEquals(0f, position.x)
        assertEquals(2256f, position.y)
    }

    @Test fun clampUsesDefaultForUninitializedPosition() {
        assertEquals(bounds.defaultPosition(), bounds.clamp(PetScreenPosition()))
    }

    @Test fun clampHandlesScreenSmallerThanPet() {
        val small = PetScreenBounds(width = 60f, height = 40f, petSize = 144f)

        assertEquals(PetScreenPosition(0f, 0f), small.clamp(PetScreenPosition(20f, 20f)))
    }
}
