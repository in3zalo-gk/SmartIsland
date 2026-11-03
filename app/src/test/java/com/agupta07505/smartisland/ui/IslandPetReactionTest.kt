/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL v3 License Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.lifecycle.ViewModelStore
import com.agupta07505.smartisland.data.SmartIslandNotificationRepository
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.PetMood
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** Exercises real ViewModel collectors with a virtual coroutine clock. */
@OptIn(ExperimentalCoroutinesApi::class)
class IslandPetReactionTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val config = MutableStateFlow(SmartIslandSettings(enablePet = true))
    private lateinit var notifications: SmartIslandNotificationRepository
    private lateinit var viewModel: IslandViewModel

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        val settings = mockk<SmartIslandSettingsRepository>()
        every { settings.settings } returns config
        notifications = SmartIslandNotificationRepository()
        viewModel = IslandViewModel(settings, notifications)
        store.put("pet", viewModel)
        dispatcher.scheduler.runCurrent()
    }

    @After fun tearDown() {
        store.clear()
        dispatcher.scheduler.runCurrent()
        Dispatchers.resetMain()
    }

    private fun advance(millis: Long) {
        dispatcher.scheduler.advanceTimeBy(millis)
        dispatcher.scheduler.runCurrent()
    }

    private fun show(mode: IslandMode, title: String = "Test activity") {
        notifications.postNotification(IslandNotification(
            key = "pet_test", packageName = "test.pet", appName = "Test",
            title = title, text = "Synthetic test notification", mode = mode,
            timeMillis = System.currentTimeMillis()
        ))
        dispatcher.scheduler.runCurrent()
    }

    private fun react(mood: PetMood = PetMood.Tapped) {
        viewModel.setPetMood(mood)
        dispatcher.scheduler.runCurrent()
    }

    private fun expect(mood: PetMood) = assertEquals(mood, viewModel.petState.value.mood)

    @Test fun tapExpiresAtItsActualDeadline() {
        react()
        advance(1499)
        expect(PetMood.Tapped)
        advance(1)
        expect(PetMood.Idle)
    }

    @Test fun newGestureCancelsTheOlderReactionTimer() {
        react()
        advance(1000)
        react(PetMood.Dragging)
        advance(500)
        expect(PetMood.Dragging)
        advance(699)
        expect(PetMood.Dragging)
        advance(1)
        expect(PetMood.Idle)
    }

    @Test fun sameMoodWithNewTimestampRestartsTheDeadline() {
        react()
        advance(1000)
        // Explicit identity avoids depending on wall-clock advancement in a virtual-time test.
        viewModel.petState.value = viewModel.petState.value.copy(
            moodTimestamp = viewModel.petState.value.moodTimestamp + 1
        )
        dispatcher.scheduler.runCurrent()
        advance(500)
        expect(PetMood.Tapped)
        advance(1000)
        expect(PetMood.Idle)
    }

    @Test fun musicResumesAndDoesNotExpireAfterATap() {
        show(IslandMode.Music)
        expect(PetMood.Music)
        react()
        advance(1500)
        expect(PetMood.Music)
        advance(10000)
        expect(PetMood.Music)
    }

    @Test fun ordinaryActivityDoesNotOverwriteAnActiveGesture() {
        react()
        advance(500)
        show(IslandMode.Battery, "Charging")
        expect(PetMood.Tapped)
        advance(1000)
        expect(PetMood.Charging)
    }

    @Test fun incomingCallInterruptsATapAndSurvivesTheOldDeadline() {
        react()
        advance(500)
        show(IslandMode.IncomingCall)
        expect(PetMood.Calling)
        advance(2000)
        expect(PetMood.Calling)
    }

    @Test fun lowBatteryTitleChangeWithinTheSameModeInterruptsATap() {
        show(IslandMode.Battery, "Charging")
        react()
        show(IslandMode.Battery, "Bateria baixa")
        expect(PetMood.LowBattery)
        advance(2000)
        expect(PetMood.LowBattery)
    }

    @Test fun disablingAndReenablingPetRestoresTheCurrentActivity() {
        show(IslandMode.Music)
        react()
        config.value = config.value.copy(enablePet = false)
        dispatcher.scheduler.runCurrent()
        expect(PetMood.Idle)
        advance(2000)
        expect(PetMood.Idle)
        config.value = config.value.copy(enablePet = true)
        dispatcher.scheduler.runCurrent()
        expect(PetMood.Music)
    }

    @Test fun happyReactionExpiresWithoutRestartingALoop() {
        show(IslandMode.Notification)
        expect(PetMood.Happy)
        advance(3999)
        expect(PetMood.Happy)
        advance(1)
        expect(PetMood.Idle)
        advance(20000)
        expect(PetMood.Idle)
    }
}
