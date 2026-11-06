package com.agupta07505.smartisland.ui.glance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlanceCatalogTest {
    @Test fun eachPageOffersClassicAndThreePremiumSkins() {
        assertEquals(4, GlancePage.entries.size)
        GlancePage.entries.forEach { page ->
            val styles = GlanceStyle.forPage(page)
            assertEquals("${page.label} needs Classic + three premium skins", 4, styles.size)
            assertTrue(styles.contains(GlanceLocalStore.defaultStyle(page)))
        }
    }

    @Test fun defaultsBelongToTheirPage() {
        GlancePage.entries.forEach { page ->
            assertEquals(page, GlanceLocalStore.defaultStyle(page).page)
        }
    }
}
