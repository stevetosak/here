package net.tosak.here.screens.presence.viewmodel

import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.tosak.here.shared.events.Event
import net.tosak.here.shared.events.EventBus
import net.tosak.here.shared.storage.AppStorage
import net.tosak.here.shared.storage.AppStorage.Companion.KEY_PRESENCE_ENABLED
import javax.inject.Inject

@HiltViewModel
class PresenceViewModel @Inject constructor(
    private val appStorage: AppStorage,
    private val eventBus: EventBus,
) : ViewModel() {

    private val _presenceOn = MutableStateFlow(
        appStorage.prefs.getBoolean(KEY_PRESENCE_ENABLED, false)
    )
    val presenceOn: StateFlow<Boolean> = _presenceOn.asStateFlow()

    fun setPresence(on: Boolean) {
        appStorage.prefs.edit { putBoolean(KEY_PRESENCE_ENABLED, on) }
        _presenceOn.value = on
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    fun onActivated(on: Boolean) {
        setPresence(on)
        val toast = if (on) "PRESENCE ON · 400M · 2H" else "PRESENCE OFF · NOTHING REMAINS"
        eventBus.emit(Event.Toast.Show(toast))
        eventBus.emit(Event.Nav.GoBack)
    }

    fun onCancel() {
        eventBus.emit(Event.Nav.GoBack)
    }
}