/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL v3 License Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PetMoodTimingTest {
    @Test fun gesturesHaveFiniteDurations() {
        assertEquals(1500L, PetMood.Tapped.transientDurationMillis)
        assertEquals(1200L, PetMood.Dragging.transientDurationMillis)
    }

    @Test fun happyAndExcitedLastFourSeconds() {
        assertEquals(4000L, PetMood.Happy.transientDurationMillis)
        assertEquals(4000L, PetMood.Excited.transientDurationMillis)
    }

    @Test fun ongoingActivitiesNeverExpireOnTheirOwn() {
        val ongoing = listOf(PetMood.Music, PetMood.Calling, PetMood.Charging,
            PetMood.Installing, PetMood.BluetoothConnected, PetMood.Hotspot,
            PetMood.Flashlight, PetMood.LowBattery, PetMood.Idle, PetMood.Sleeping)
        ongoing.forEach { mood -> assertNull(mood.transientDurationMillis) }
    }

    @Test fun restoringActivityDoesNotStartAnotherTemporaryReaction() {
        listOf(PetMood.Happy, PetMood.Excited, PetMood.Tapped, PetMood.Dragging)
            .forEach { assertEquals(PetMood.Idle, it.afterReaction()) }
    }

    @Test fun gesturesRestoreTheCurrentOngoingActivity() {
        listOf(PetMood.Music, PetMood.Calling, PetMood.Charging, PetMood.LowBattery, PetMood.Idle)
            .forEach { assertEquals(it, it.afterReaction()) }
    }
}
