/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL v3 License Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * Represents the current mood/state of the virtual pet.
 * The pet reacts to system events and user interactions.
 */
enum class PetMood {
    Idle,
    Happy,
    Excited,
    Sleeping,
    Charging,
    Installing,
    BluetoothConnected,
    Tapped,
    Dragging,
    Music,
    Calling,
    LowBattery,
    Hotspot,
    Flashlight
}

/** Top-left screen coordinates for the 48dp external pet hit target. */
data class PetScreenPosition(
    val x: Float = Float.NaN,
    val y: Float = Float.NaN
)

/**
 * Pure pixel-space geometry for the external overlay. This keeps movement independent of the
 * pill's bounds and makes its safety clamp testable without Android or Compose.
 */
data class PetScreenBounds(
    val width: Float,
    val height: Float,
    val petSize: Float
) {
    val isUsable: Boolean get() = width > 0f && height > 0f && petSize > 0f

    fun defaultPosition(): PetScreenPosition = PetScreenPosition(
        x = ((width - petSize) / 2f).coerceAtLeast(0f),
        y = (height * 0.12f).coerceIn(0f, (height - petSize).coerceAtLeast(0f))
    )

    fun clamp(position: PetScreenPosition): PetScreenPosition {
        if (!isUsable) return position
        val fallback = defaultPosition()
        return PetScreenPosition(
            x = position.x.takeIf { it.isFinite() }?.coerceIn(0f, (width - petSize).coerceAtLeast(0f)) ?: fallback.x,
            y = position.y.takeIf { it.isFinite() }?.coerceIn(0f, (height - petSize).coerceAtLeast(0f)) ?: fallback.y
        )
    }
}

data class IslandPetState(
    val mood: PetMood = PetMood.Idle,
    val moodTimestamp: Long = System.currentTimeMillis()
) {
    /**
     * Returns true if the current mood has expired and should revert to Idle.
     * Most moods last 3-5 seconds before reverting.
     */
    fun shouldRevertToIdle(now: Long = System.currentTimeMillis()): Boolean {
        if (mood == PetMood.Idle || mood == PetMood.Sleeping) return false
        val duration = mood.transientDurationMillis ?: return false
        return (now - moodTimestamp) >= duration
    }
}
