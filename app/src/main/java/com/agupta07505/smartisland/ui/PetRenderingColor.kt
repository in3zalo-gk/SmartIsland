/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL v3 License Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.ui.graphics.Color

/**
 * Keeps pet details visually tied to the configured pet color.
 *
 * This helper deliberately has no Compose-local or lifecycle dependency so callers
 * can use it from drawing code without changing the public pet composable API.
 */
internal fun petDetailColor(petColor: Long): Color = Color(petColor)
