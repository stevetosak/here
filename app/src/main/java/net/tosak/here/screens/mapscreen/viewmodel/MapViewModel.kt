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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
import net.tosak.here.shared.ping.PingRepository
import net.tosak.here.shared.ping.PingUiState
import net.tosak.here.shared.ping.ProximityRepository
import net.tosak.here.shared.storage.PostEntity
import net.tosak.here.shared.storage.PostRepository
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val fusedLocationClient: FusedLocationProviderClient,
    private val locationRepository: LocationRepository,
    private val postRepository: PostRepository,
    private val proximityRepository: ProximityRepository,
    private val pingRepository: PingRepository,
    private val eventBus: EventBus,
) : ViewModel() {

    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation: StateFlow<Location?> = _userLocation.asStateFlow()

    /** Live friends currently inside the radius — rendered as map markers. */
    val friends: StateFlow<List<Friend>> = proximityRepository.nearbyFriends
        .map { list -> list.filter { it.dist <= ProximityRepository.RADIUS_M } }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val activePost: StateFlow<PostEntity?> = postRepository.activePosts
        .map { it.firstOrNull() }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    // ── Friend quick-action sheet ───────────────────────────────────────────────

    private val _selectedFriend = MutableStateFlow<Friend?>(null)
    val selectedFriend: StateFlow<Friend?> = _selectedFriend.asStateFlow()

    private val _pingUiState = MutableStateFlow<PingUiState>(PingUiState.Idle)
    val pingUiState: StateFlow<PingUiState> = _pingUiState.asStateFlow()

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

    /** Tapping a friend marker opens the quick-action sheet (chat / ping). */
    fun onFriend(friend: Friend) {
        _selectedFriend.value = friend
        _pingUiState.value = PingUiState.Idle
    }

    fun dismissSheet() {
        _selectedFriend.value = null
        _pingUiState.value = PingUiState.Idle
    }

    fun onChat() {
        val f = _selectedFriend.value ?: return
        eventBus.emit(Event.AppState.ActiveFriendChanged(f))
        eventBus.emit(Event.Nav.NavigateTo(AppScreen.DM))
        dismissSheet()
    }

    fun onComposePing() { _pingUiState.value = PingUiState.Composing }

    fun onSendPing(intent: String) {
        val f = _selectedFriend.value ?: return
        _pingUiState.value = PingUiState.Sending
        viewModelScope.launch {
            when (pingRepository.sendManualPing(f, intent.trim().ifBlank { null })) {
                PingRepository.SendResult.SENT -> {
                    _pingUiState.value = PingUiState.Sent
                    eventBus.emit(Event.Toast.Show("Ping sent"))
                    dismissSheet()
                }
                PingRepository.SendResult.ON_COOLDOWN -> {
                    _pingUiState.value = PingUiState.Error("On cooldown")
                    eventBus.emit(Event.Toast.Show("Ping on cooldown"))
                }
            }
        }
    }

    // ── Demo: timed auto-ping arrival simulation ────────────────────────────────

    private var simJob: Job? = null

    /** Moves a sample friend into radius ~10s after the map opens (auto-ping demo). */
    fun startArrivalSimulation() {
        if (simJob?.isActive == true) return
        simJob = viewModelScope.launch {
            delay(10_000)
            proximityRepository.triggerArrivalSimulation()
        }
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