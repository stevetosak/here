package net.tosak.here.screens.friends.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import net.tosak.here.shared.events.Event
import net.tosak.here.shared.events.EventBus
import net.tosak.here.shared.location.LocationRepository
import net.tosak.here.shared.model.AppScreen
import net.tosak.here.shared.model.Friend
import net.tosak.here.shared.model.YOU_LAT
import net.tosak.here.shared.model.YOU_LNG
import net.tosak.here.shared.model.anchoredSampleFriends
import net.tosak.here.shared.storage.ChatRepository
import net.tosak.here.shared.storage.DmMessageEntity
import javax.inject.Inject

data class FriendRow(
    val friend: Friend,
    val lastMessage: DmMessageEntity?,
)

@HiltViewModel
class FriendsListViewModel @Inject constructor(
    private val eventBus: EventBus,
    private val chatRepository: ChatRepository,
    locationRepository: LocationRepository,
) : ViewModel() {

    val rows: StateFlow<List<FriendRow>> = combine(
        locationRepository.lastLocation,
        chatRepository.lastPerFriend,
    ) { loc, lasts ->
        val friends = anchoredSampleFriends(
            userLat = loc?.latitude  ?: YOU_LAT,
            userLng = loc?.longitude ?: YOU_LNG,
        )
        val byFriend = lasts.associateBy { it.friendId }
        friends.map { f -> FriendRow(f, byFriend[f.id]) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun onFriend(friend: Friend) {
        eventBus.emit(Event.AppState.ActiveFriendChanged(friend))
        eventBus.emit(Event.Nav.NavigateTo(AppScreen.DM))
    }

    fun onBack() {
        eventBus.emit(Event.Nav.GoBack)
    }
}
