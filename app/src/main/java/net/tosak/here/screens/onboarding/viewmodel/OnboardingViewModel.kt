package net.tosak.here.screens.onboarding.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import net.tosak.here.shared.auth.AuthRepository
import net.tosak.here.shared.events.Event
import net.tosak.here.shared.events.EventBus
import net.tosak.here.shared.model.AppScreen
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val eventBus: EventBus,
) : ViewModel() {

    /**
     * Called when the user confirms their handle and finishes onboarding.
     * Persists the session, shows a welcome toast, and resets the stack to MAP.
     */
    fun onDone(handle: String) {
        authRepository.saveSession(handle.ifBlank { "you" })
        eventBus.emit(Event.Toast.Show("READY · YOU ARE INVISIBLE"))
        eventBus.emit(Event.Nav.Reset(AppScreen.MAP))
    }
}