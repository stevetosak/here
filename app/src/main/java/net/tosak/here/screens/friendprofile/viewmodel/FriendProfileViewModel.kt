package net.tosak.here.screens.friendprofile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.tosak.here.shared.events.Event
import net.tosak.here.shared.events.EventBus
import net.tosak.here.shared.model.Friend
import net.tosak.here.shared.ping.PingSettingsRepository
import net.tosak.here.shared.state.AppStateRepository
import net.tosak.here.shared.storage.FriendRepository
import javax.inject.Inject

/** Memento of the friendship — where/when you connected. */
data class Memento(
    val location: String,
    val connectedAt: Long?,   // null for demo/sample friends not persisted in Room
    val fromHandshake: Boolean,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FriendProfileViewModel @Inject constructor(
    private val eventBus: EventBus,
    private val friendRepository: FriendRepository,
    private val pingSettingsRepository: PingSettingsRepository,
    appStateRepository: AppStateRepository,
) : ViewModel() {

    val friend: StateFlow<Friend?> = appStateRepository.activeFriend

    val autoPingEnabled: StateFlow<Boolean> = friend
        .flatMapLatest { f ->
            if (f == null) flowOf(false) else pingSettingsRepository.autoPingEnabled(f.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _memento = MutableStateFlow<Memento?>(null)
    val memento: StateFlow<Memento?> = _memento.asStateFlow()

    /** Global active-hours label, shown in the reciprocity note. */
    val activeHoursLabel: String get() = pingSettingsRepository.formatActiveHours()

    init {
        viewModelScope.launch {
            friend.collect { f -> _memento.value = f?.let { buildMemento(it) } }
        }
    }

    private suspend fun buildMemento(f: Friend): Memento {
        val entity = friendRepository.byId(f.id)
        return if (entity != null) {
            Memento(location = entity.location, connectedAt = entity.addedAt, fromHandshake = true)
        } else {
            Memento(location = f.post?.place ?: "Debar Maalo", connectedAt = null, fromHandshake = false)
        }
    }

    fun onToggleAutoPing(enabled: Boolean) {
        val f = friend.value ?: return
        viewModelScope.launch { pingSettingsRepository.setAutoPing(f.id, enabled) }
    }

    fun onUnfriend() {
        val f = friend.value ?: return
        viewModelScope.launch {
            friendRepository.remove(f.id)
            pingSettingsRepository.clear(f.id)
        }
        eventBus.emit(Event.Toast.Show("Unfriended @${f.id}"))
        eventBus.emit(Event.Nav.GoBack) // leave profile
        eventBus.emit(Event.Nav.GoBack) // leave DM → back to FRIENDS
    }

    fun onBack() {
        eventBus.emit(Event.Nav.GoBack)
    }
}
