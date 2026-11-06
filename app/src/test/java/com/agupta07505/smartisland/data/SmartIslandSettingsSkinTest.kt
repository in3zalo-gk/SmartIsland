/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import com.agupta07505.smartisland.model.IslandSkin
import com.agupta07505.smartisland.model.IslandSkinPalette
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartIslandSettingsSkinTest {
    @Test fun skinSettingsRoundTripThroughBackupWithoutChangingExistingShape() {
        val original = SmartIslandSettings(
            islandSkin = IslandSkin.Saturn,
            islandSkinPalette = IslandSkinPalette.Aurora,
            islandSkinAnimationsEnabled = false
        )

        val backup = original.toJson("1.0")
        val settings = JSONObject(backup).getJSONObject("settings")
        val restored = SmartIslandSettings.fromJson(backup)

        assertEquals("saturn", settings.getString("islandSkin"))
        assertEquals("aurora", settings.getString("islandSkinPalette"))
        assertEquals(IslandSkin.Saturn, restored.islandSkin)
        assertEquals(IslandSkinPalette.Aurora, restored.islandSkinPalette)
        assertEquals(false, restored.islandSkinAnimationsEnabled)
    }

    @Test fun legacyBackupWithoutSkinFieldsUsesMigrationDefaults() {
        val restored = SmartIslandSettings.fromJson("""{"settings":{"enabled":true,"width":120}}""")

        assertTrue(restored.enabled)
        assertEquals(120f, restored.width)
        assertEquals(IslandSkin.Default, restored.islandSkin)
        assertEquals(IslandSkinPalette.Default, restored.islandSkinPalette)
        assertTrue(restored.islandSkinAnimationsEnabled)
    }

    @Test fun unrecognisedBackupSkinValuesNeverBreakRestore() {
        val restored = SmartIslandSettings.fromJson(
            """{"settings":{"islandSkin":"future","islandSkinPalette":"future"}}"""
        )

        assertEquals(IslandSkin.Default, restored.islandSkin)
        assertEquals(IslandSkinPalette.Default, restored.islandSkinPalette)
    }
}
