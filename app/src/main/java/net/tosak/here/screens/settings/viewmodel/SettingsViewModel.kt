package net.tosak.here.screens.settings.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import net.tosak.here.shared.auth.AuthRepository
import net.tosak.here.shared.events.Event
import net.tosak.here.shared.events.EventBus
import net.tosak.here.shared.ping.PingEngine
import net.tosak.here.shared.ping.PingSettingsRepository
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val eventBus: EventBus,
    private val pingSettings: PingSettingsRepository,
    private val pingEngine: PingEngine,
) : ViewModel() {

    /** Current handle — read directly from the repository (SharedPreferences). */
    val handle: String get() = authRepository.handle

    // ── Pings ───────────────────────────────────────────────────────────────────
    val pingsPaused: Boolean get() = pingSettings.pingsPaused
    val activeHoursLabel: String get() = pingSettings.formatActiveHours()

    fun setPingsPaused(paused: Boolean) { pingSettings.pingsPaused = paused }

    /** Debug-only: surface a mock incoming ping to exercise the overlay + notification. */
    fun simulateIncomingPing() { pingEngine.simulateIncomingManualPing() }

    fun onClose() {
        eventBus.emit(Event.Nav.GoBack)
    }

    /** Wipe the local session; NavigationViewModel reacts via the auth flow. */
    fun onSignOut() {
        authRepository.clearSession()
    }
}