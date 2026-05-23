package net.tosak.here.shared.location

import android.location.Location
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton that bridges the continuous GPS updates from [MapViewModel] to any
 * other ViewModel that needs the last known position (e.g. [ComposerViewModel]).
 *
 * [MapViewModel] calls [update] on every location fix.
 * Other components read [lastLocation] without needing a separate permission flow.
 */
@Singleton
class LocationRepository @Inject constructor() {
    private val _lastLocation = MutableStateFlow<Location?>(null)
    val lastLocation: StateFlow<Location?> = _lastLocation.asStateFlow()

    fun update(location: Location) { _lastLocation.value = location }
}
