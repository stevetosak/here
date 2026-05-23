package net.tosak.here.screens.presence.viewmodel

import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.tosak.here.shared.storage.AppStorage
import net.tosak.here.shared.storage.AppStorage.Companion.KEY_PRESENCE_ENABLED
import javax.inject.Inject

/**
 * Single source of truth for presence state.
 *
 * Reads the persisted value from [AppStorage] on construction so presence
 * survives process death, then exposes a [StateFlow] for the UI to collect.
 * All writes go through [setPresence] so the pref and the flow stay in sync.
 */
@HiltViewModel
class PresenceViewModel @Inject constructor(
    private val appStorage: AppStorage,
) : ViewModel() {

    private val _presenceOn = MutableStateFlow(
        appStorage.prefs.getBoolean(KEY_PRESENCE_ENABLED, false)
    )
    val presenceOn: StateFlow<Boolean> = _presenceOn.asStateFlow()

    fun setPresence(on: Boolean) {
        appStorage.prefs.edit { putBoolean(KEY_PRESENCE_ENABLED, on) }
        _presenceOn.value = on
    }
}
