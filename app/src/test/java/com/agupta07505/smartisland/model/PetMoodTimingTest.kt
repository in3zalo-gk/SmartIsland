package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PetMoodTimingTest {
    @Test fun tapped_is_a_short_reaction() {
        assertEquals(1500L, PetMood.Tapped.transientDurationMillis)
        assertTrue(PetMood.Tapped.isTransientReaction)
    }

    @Test fun dragging_has_a_failsafe_timeout() {
        assertEquals(1200L, PetMood.Dragging.transientDurationMillis)
        assertTrue(PetMood.Dragging.isTransientReaction)
    }

    @Test fun ongoing_system_moods_do_not_expire() {
        assertNull(PetMood.Music.transientDurationMillis)
        assertNull(PetMood.LowBattery.transientDurationMillis)
        assertFalse(PetMood.Music.isTransientReaction)
    }

    @Test fun drag_finish_restores_active_mood() {
        assertEquals(PetMood.Music, PetMood.Dragging.afterDrag(PetMood.Music))
        assertEquals(PetMood.Idle, PetMood.Dragging.afterDrag(PetMood.Tapped))
    }
}
