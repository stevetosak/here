package net.tosak.here.shared.ping

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import net.tosak.here.shared.events.Event
import net.tosak.here.shared.events.EventBus
import net.tosak.here.shared.model.AppScreen
import net.tosak.here.shared.storage.ChatRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the currently-displayed incoming ping for the in-app overlay and
 * handles its actions. Collects [Event.Ping.Incoming] from the [EventBus].
 */
@Singleton
class IncomingPingController @Inject constructor(
    private val eventBus: EventBus,
    private val chatRepository: ChatRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _current = MutableStateFlow<IncomingPing?>(null)
    val current: StateFlow<IncomingPing?> = _current.asStateFlow()

    init {
        scope.launch {
            eventBus.events
                .filterIsInstance<Event.Ping.Incoming>()
                .collect { e ->
                    _current.value = IncomingPing(
                        friend        = e.friend,
                        type          = e.ping.type,
                        intentMessage = e.ping.intentMessage,
                        groupedCount  = e.groupedCount,
                    )
                }
        }
    }

    /** "I'm on my way" — reply to the sender and open the coordination thread. */
    fun onOnMyWay() {
        val p = _current.value ?: return
        scope.launch { chatRepository.sendText(p.friend.id, "On my way!") }
        eventBus.emit(Event.AppState.ActiveFriendChanged(p.friend))
        eventBus.emit(Event.Nav.NavigateTo(AppScreen.DM))
        _current.value = null
    }

    /** "Ignore" — silent dismiss, no reply sent. */
    fun onIgnore() {
        _current.value = null
    }
}
