/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * Locally drawn idle-pill skins. These are original Compose treatments, not Vision assets
 * or implementation. The names make the public style catalogue easy to recognise.
 */
enum class IslandSkin(
    val storageValue: String,
    val displayName: String,
    val description: String
) {
    Classic("classic", "Classic", "Clean dark pill"),
    Obsidian("obsidian", "Obsidian", "Deep glass with a violet edge"),
    Titanium("titanium", "Titanium", "Brushed silver highlight"),
    BlackHole("black_hole", "Black hole", "Soft center glow"),
    Eclipse("eclipse", "Eclipse", "Dark arc with a bright rim"),
    Lightspeed("lightspeed", "Lightspeed", "Diagonal colour trail"),
    Saturn("saturn", "Saturn", "Warm orbit ring");

    companion object {
        val Default: IslandSkin = Classic

        /** Unknown or legacy values safely retain the original Smart Island appearance. */
        fun fromStorage(value: String?): IslandSkin =
            entries.firstOrNull { it.storageValue == value } ?: Default

        /**
         * Publicly exposed Glance labels found on vision.kitostudio.pro.
         * They are metadata only: Smart Island does not copy or render Vision's Glance UI.
         */
        val publicGlanceStyleNames: Map<String, List<String>> = mapOf(
            "Summary" to listOf("Classic", "Dot matrix", "Chronograph", "Bento"),
            "Calendar" to listOf("Classic", "24-hour", "Glass", "Big date"),
            "Water" to listOf("Classic", "Liquid", "Expressive", "Bento"),
            "Prayer" to listOf("Classic", "Sky", "Countdown", "Timeline")
        )
    }
}

enum class IslandSkinPalette(
    val storageValue: String,
    val displayName: String,
    val colors: List<Long>
) {
    Wallpaper("wallpaper", "Wallpaper", listOf(0xFF6B59FFL, 0xFF2EC5FFL, 0xFFFF599EL)),
    Sunset("sunset", "Sunset", listOf(0xFFFF7A59L, 0xFFFFC857L, 0xFFEF5DA8L)),
    Ocean("ocean", "Ocean", listOf(0xFF2EC5FFL, 0xFF2EF2B0L, 0xFF386BFFL)),
    Aurora("aurora", "Aurora", listOf(0xFF2EF2B0L, 0xFF2EC5FFL, 0xFF8B5CFFL)),
    Mint("mint", "Mint", listOf(0xFF6EF2B8L, 0xFF9BF0E1L, 0xFF4FD1A5L)),
    Mono("mono", "Mono", listOf(0xFFF5F5F5L, 0xFFC9C9C9L, 0xFFA8A8A8L));

    companion object {
        val Default: IslandSkinPalette = Wallpaper
        fun fromStorage(value: String?): IslandSkinPalette =
            entries.firstOrNull { it.storageValue == value } ?: Default
    }
}
