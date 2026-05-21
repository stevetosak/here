package net.tosak.here.viewmodel

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
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.tosak.here.model.Friend
import net.tosak.here.model.YOU_LAT
import net.tosak.here.model.YOU_LNG
import net.tosak.here.model.anchoredSampleFriends
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val fusedLocationClient: FusedLocationProviderClient,
) : ViewModel() {

    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation: StateFlow<Location?> = _userLocation.asStateFlow()

    // Demo friends re-anchored to the live user position every time a new GPS
    // fix arrives. Falls back to the hardcoded Skopje coordinates until the
    // first fix so the map is never empty during development.
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

    // ── Continuous updates ────────────────────────────────────────────────────
    // Used while the map screen is visible; stopped when the screen leaves
    // composition via the DisposableEffect in MapScreen.

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { _userLocation.value = it }
        }
    }

    // Permission is verified by the caller (MapScreen) before this is invoked.
    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3_000L)
            .setMinUpdateIntervalMillis(1_500L)
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

    // ── One-shot fetch (kept as per the requested pattern) ────────────────────
    @SuppressLint("MissingPermission")
    fun fetchLocation(simulateDelay: Boolean = false) {
        if (simulateDelay) {
            viewModelScope.launch {
                delay(3_000)
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    CancellationTokenSource().token,
                ).addOnSuccessListener { location ->
                    _userLocation.value = location
                }
            }
        } else {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                CancellationTokenSource().token,
            ).addOnSuccessListener { location ->
                _userLocation.value = location
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }
}