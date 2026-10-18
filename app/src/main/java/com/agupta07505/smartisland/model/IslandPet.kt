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

/**
 * Data class holding the pet's current state.
 */
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
        val duration = when (mood) {
            PetMood.Dragging -> 0L
            PetMood.Tapped -> 1500L
            PetMood.Music, PetMood.Calling, PetMood.Charging,
            PetMood.Installing, PetMood.BluetoothConnected,
            PetMood.Hotspot, PetMood.Flashlight, PetMood.LowBattery -> 0L
            else -> 4000L
        }
        return duration > 0L && (now - moodTimestamp) > duration
    }
}
