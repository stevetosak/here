package net.tosak.here.shared.ping

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.tosak.here.shared.location.LocationRepository
import net.tosak.here.shared.model.Friend
import net.tosak.here.shared.model.YOU_LAT
import net.tosak.here.shared.model.YOU_LNG
import net.tosak.here.shared.model.anchoredSampleFriends
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks which friends are currently nearby/live. No backend — derived from the
 * mocked [anchoredSampleFriends] anchored to the device's location.
 *
 * Also drives the demo "arrival" simulation: [triggerArrivalSimulation] moves a
 * sample friend ([SIM_FRIEND_ID]) from just-outside the radius to inside it, so
 * the auto-ping path can be exercised end-to-end.
 */
@Singleton
class ProximityRepository @Inject constructor(
    locationRepository: LocationRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _simulateArrival = MutableStateFlow(false)

    /** All known live friends, anchored to the current location. */
    val nearbyFriends: StateFlow<List<Friend>> = combine(
        locationRepository.lastLocation,
        _simulateArrival,
    ) { loc, arrived ->
        val base = anchoredSampleFriends(loc?.latitude ?: YOU_LAT, loc?.longitude ?: YOU_LNG)
        if (!arrived) base
        else base.map { if (it.id == SIM_FRIEND_ID) it.copy(dist = 120) else it }
    }.stateIn(scope, SharingStarted.Eagerly, anchoredSampleFriends(YOU_LAT, YOU_LNG))

    val proximity: StateFlow<List<FriendProximityState>> = nearbyFriends
        .map { list ->
            list.map { FriendProximityState(it.id, it.dist <= RADIUS_M, it.dist.toFloat()) }
        }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun triggerArrivalSimulation() { _simulateArrival.value = true }
    fun resetArrivalSimulation() { _simulateArrival.value = false }

    companion object {
        const val RADIUS_M = 400
        const val SIM_FRIEND_ID = "noa"
    }
}
