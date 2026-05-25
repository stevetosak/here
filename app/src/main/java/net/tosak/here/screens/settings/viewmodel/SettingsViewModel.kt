package net.tosak.here.screens.settings.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import net.tosak.here.shared.auth.AuthRepository
import net.tosak.here.shared.events.Event
import net.tosak.here.shared.events.EventBus
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val eventBus: EventBus,
) : ViewModel() {

    /** Current handle — read directly from the repository (SharedPreferences). */
    val handle: String get() = authRepository.handle

    fun onClose() {
        eventBus.emit(Event.Nav.GoBack)
    }

    /** Wipe the local session; NavigationViewModel reacts via the auth flow. */
    fun onSignOut() {
        authRepository.clearSession()
    }
}