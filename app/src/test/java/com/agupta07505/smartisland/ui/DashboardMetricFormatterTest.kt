package com.agupta07505.smartisland.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardMetricFormatterTest {
    @Test fun percentage_returnsZero_withoutUsableCounts() {
        assertEquals(0, DashboardMetricFormatter.percentage(4, 0))
        assertEquals(0, DashboardMetricFormatter.percentage(0, 8))
        assertEquals(0, DashboardMetricFormatter.percentage(-1, 8))
        assertEquals(0, DashboardMetricFormatter.percentage(1, -8))
    }
    @Test fun percentage_roundsToNearestInteger() {
        assertEquals(33, DashboardMetricFormatter.percentage(1, 3))
        assertEquals(67, DashboardMetricFormatter.percentage(2, 3))
    }
    @Test fun percentage_clampsInvalidInput_andFormatsLabel() {
        assertEquals(100, DashboardMetricFormatter.percentage(9, 3))
        assertEquals("25%", DashboardMetricFormatter.percentageLabel(1, 4))
    }
}
