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
        // Dragging is owned by pointer lifecycle; it must never expire under a held finger.
        PetMood.Dragging -> null
        PetMood.Happy, PetMood.Excited -> 4000L
        else -> null
    }

/** Restore ongoing activity after a gesture without restarting a transient reaction.
 * Dragging has no timer, but is still a gesture rather than an ongoing system state.
 */
internal fun PetMood.afterReaction(): PetMood =
    if (this == PetMood.Dragging || transientDurationMillis != null) PetMood.Idle else this
