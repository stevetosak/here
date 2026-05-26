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
import net.tosak.here.shared.events.Event
import net.tosak.here.shared.events.EventBus
import net.tosak.here.shared.location.LocationRepository
import net.tosak.here.shared.model.AppScreen
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
    private val eventBus: EventBus,
) : ViewModel() {

    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation: StateFlow<Location?> = _userLocation.asStateFlow()

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

    val activePost: StateFlow<PostEntity?> = postRepository.activePosts
        .map { it.firstOrNull() }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    init {
        viewModelScope.launch { postRepository.pruneExpired() }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    fun onActivate()  { eventBus.emit(Event.Nav.NavigateTo(AppScreen.PRESENCE)) }
    fun onCompose()   { eventBus.emit(Event.Nav.NavigateTo(AppScreen.COMPOSER)) }
    fun onOwnPost()   { eventBus.emit(Event.Nav.NavigateTo(AppScreen.OWN_POST)) }
    fun onSettings()  { eventBus.emit(Event.Nav.NavigateTo(AppScreen.SETTINGS)) }
    fun onFriends()   { eventBus.emit(Event.Nav.NavigateTo(AppScreen.FRIENDS)) }
    fun onHandshake() { eventBus.emit(Event.Nav.NavigateTo(AppScreen.HANDSHAKE)) }

    fun onFriend(friend: Friend) {
        eventBus.emit(Event.AppState.ActiveFriendChanged(friend))
        eventBus.emit(Event.Nav.NavigateTo(AppScreen.POST))
    }

    // ── Location updates ──────────────────────────────────────────────────────

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { loc ->
                _userLocation.value = loc
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