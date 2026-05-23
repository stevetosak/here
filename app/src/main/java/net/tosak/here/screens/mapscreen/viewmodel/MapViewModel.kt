package net.tosak.here.screens.mapscreen.viewmodel

import android.annotation.SuppressLint
import android.location.Location
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.tosak.here.shared.location.LocationRepository
import net.tosak.here.shared.model.Friend
import net.tosak.here.shared.model.YOU_LAT
import net.tosak.here.shared.model.YOU_LNG
import net.tosak.here.shared.model.anchoredSampleFriends
import net.tosak.here.shared.storage.PostEntity
import net.tosak.here.shared.storage.PostRepository
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val fusedLocationClient: FusedLocationProviderClient,
    private val locationRepository: LocationRepository,
    private val postRepository: PostRepository,
) : ViewModel() {

    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation: StateFlow<Location?> = _userLocation.asStateFlow()

    // Demo friends re-anchored to the live user position on every GPS fix.
    val friends: StateFlow<List<Friend>> = _userLocation
        .map { loc ->
            anchoredSampleFriends(
                userLat = loc?.latitude  ?: YOU_LAT,
                userLng = loc?.longitude ?: YOU_LNG,
            )
        }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = anchoredSampleFriends(YOU_LAT, YOU_LNG),
        )

    /** The most recent non-expired post the user has authored, or null if none. */
    val activePost: StateFlow<PostEntity?> = postRepository.activePosts
        .map { it.firstOrNull() }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    init {
        // Clean up any posts left over from a previous session on startup.
        viewModelScope.launch { postRepository.pruneExpired() }
    }

    // ── Continuous location updates ───────────────────────────────────────────

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { loc ->
                _userLocation.value = loc
                // Publish to the shared singleton so other ViewModels (e.g.
                // ComposerViewModel) can read the last known position.
                locationRepository.update(loc)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10_000L)
            .setMinUpdateIntervalMillis(5_000L)
            .setMinUpdateDistanceMeters(15f)
            .build()

        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper(),
        )
    }

    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }
}
