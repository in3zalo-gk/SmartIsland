/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL v3 License Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.model.PetMood

/**
 * A screen-coordinate pet layer used only when the accessibility overlay has a full-screen
 * touchable region. Keeping this outside [IslandOverlayView] means it is never clipped by the
 * pill's compose bounds.
 */
@Composable
internal fun FloatingPetOverlay(
    viewModel: IslandViewModel,
    mood: PetMood,
    petColor: Long,
    modifier: Modifier = Modifier
) {
    val position by viewModel.petPosition.collectAsState()
    val density = LocalDensity.current
    val hitSize = 48.dp

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val hitSizePx = with(density) { hitSize.toPx() }
        LaunchedEffect(widthPx, heightPx, hitSizePx) {
            viewModel.setPetScreenBounds(widthPx, heightPx, hitSizePx)
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(hitSize)
                .graphicsLayer {
                    translationX = position.x
                    translationY = position.y
                },
            contentAlignment = Alignment.Center
        ) {
            IslandPetComposable(
                mood = mood,
                petColor = petColor,
                onTap = { viewModel.setPetMood(PetMood.Tapped) },
                onDragStart = viewModel::startPetDrag,
                onDrag = viewModel::dragPetBy,
                onDragEnd = viewModel::endPetDrag,
                onDragCancel = viewModel::cancelPetDrag
            )
        }
    }
}
