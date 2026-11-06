/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IslandSkinTest {
    @Test fun publicIdleCatalogueHasClassicAndSixNamedAlternatives() {
        assertEquals(
            listOf("Classic", "Obsidian", "Titanium", "Black hole", "Eclipse", "Lightspeed", "Saturn"),
            IslandSkin.entries.map { it.displayName }
        )
    }

    @Test fun unknownStoredValuesFallBackToSafeDefaults() {
        assertEquals(IslandSkin.Default, IslandSkin.fromStorage("removed_skin"))
        assertEquals(IslandSkinPalette.Default, IslandSkinPalette.fromStorage(null))
    }

    @Test fun publicGlanceMetadataKeepsAllFourPagesDiscoverable() {
        assertEquals(4, IslandSkin.publicGlanceStyleNames.size)
        assertTrue(IslandSkin.publicGlanceStyleNames.values.all { it.size == 4 })
    }
}
