package net.tosak.here.shared.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import net.tosak.here.screens.handshake.viewmodel.MementoData
import net.tosak.here.shared.events.Event
import net.tosak.here.shared.events.EventBus
import net.tosak.here.shared.model.Friend
import net.tosak.here.shared.storage.ChatRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for ephemeral cross-screen state.
 *
 * Each piece of state here bridges two screens that are never on screen
 * simultaneously:
 *  - [activeFriend] — set by MapScreen when a friend is tapped; read by PostViewScreen
 *  - [chatSeed] — set by PostViewScreen when a quick-reply chip is tapped; read by ChatScreen
 *  - [pendingMemento] — set by HandshakeScreen on BLE confirmation; read by MementoScreen
 *
 * Mutations arrive via [EventBus] ([Event.AppState] events), keeping screen
 * ViewModels decoupled from each other. The repository holds its own
 * [CoroutineScope] tied to the Application lifetime so it survives activity
 * recreation without needing to be a ViewModel.
 */
@Singleton
class AppStateRepository @Inject constructor(
    eventBus: EventBus,
    chatRepository: ChatRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _activeFriend  = MutableStateFlow<Friend?>(null)
    private val _chatSeed      = MutableStateFlow<String?>(null)
    private val _pendingMemento = MutableStateFlow<MementoData?>(null)

    val activeFriend:   StateFlow<Friend?>      = _activeFriend.asStateFlow()
    val chatSeed:       StateFlow<String?>      = _chatSeed.asStateFlow()
    val pendingMemento: StateFlow<MementoData?> = _pendingMemento.asStateFlow()

    init {
        scope.launch {
            eventBus.events
                .filterIsInstance<Event.AppState>()
                .collect { event ->
                    when (event) {
                        is Event.AppState.ActiveFriendChanged   -> _activeFriend.value  = event.friend
                        is Event.AppState.ChatSeedChanged       -> _chatSeed.value      = event.seed
                        is Event.AppState.PendingMementoChanged -> _pendingMemento.value = event.memento
                    }
                }
        }
        scope.launch { chatRepository.seedIfEmpty() }
    }
}
