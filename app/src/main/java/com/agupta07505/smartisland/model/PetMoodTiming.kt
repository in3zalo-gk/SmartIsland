/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL v3 License Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/** A null duration represents an ongoing system state, not a transient reaction. */
internal val PetMood.transientDurationMillis: Long?
    get() = when (this) {
        PetMood.Tapped -> 1500L
        // Dragging is owned by the pointer lifecycle. Keeping this finite also
        // prevents a cancelled gesture from leaving the pet enlarged forever.
        PetMood.Dragging -> 1200L
        PetMood.Happy, PetMood.Excited -> 4000L
        else -> null
    }

internal val PetMood.isTransientReaction: Boolean
    get() = transientDurationMillis != null

/** Restore ongoing activity after a gesture without restarting a transient reaction. */
internal fun PetMood.afterReaction(): PetMood =
    if (isTransientReaction) PetMood.Idle else this

/**
 * The gesture ending is intentionally independent from the timeout: an active
 * system mood should resume as soon as a drag finishes.
 */
internal fun PetMood.afterDrag(activeMood: PetMood): PetMood =
    if (this == PetMood.Dragging) activeMood.afterReaction() else this
