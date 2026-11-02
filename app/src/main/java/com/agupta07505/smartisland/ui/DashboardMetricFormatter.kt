/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 */

package com.agupta07505.smartisland.ui

import kotlin.math.roundToInt

/** Pure, locale-neutral values for the Statistics & Insights dashboard. */
internal object DashboardMetricFormatter {
    /** Returns 0 when there is no activity, and always clamps the result to 0..100. */
    fun percentage(numerator: Int, denominator: Int): Int {
        if (denominator <= 0 || numerator <= 0) return 0
        return ((numerator.toDouble() / denominator) * 100)
            .roundToInt()
            .coerceIn(0, 100)
    }

    fun percentageLabel(numerator: Int, denominator: Int): String =
        "${percentage(numerator, denominator)}%"
}
