package net.tosak.here.shared.events

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import net.tosak.here.shared.model.AppScreen
import net.tosak.here.shared.model.Friend
import net.tosak.here.screens.handshake.viewmodel.MementoData
import javax.inject.Inject
import javax.inject.Singleton

sealed interface Event {
    sealed interface Nav : Event {
        data object GoBack : Nav
        data class NavigateTo(val screen: AppScreen) : Nav
        data class Reset(val screen: AppScreen) : Nav
        /** Replace the top stack entry without leaving a back entry behind. */
        data class ReplaceTop(val screen: AppScreen) : Nav
    }

    sealed interface Toast : Event {
        data class Show(val message: String) : Toast
        data class ShowError(val message: String) : Toast
    }

    sealed interface Auth : Event {
        data object SignedOut : Auth
        data object SessionExpired : Auth
    }

    /**
     * Cross-screen state mutations.
     *
     * Screens emit these when they need to update data that another screen will
     * display. [NavigationViewModel] subscribes and stores the values as
     * Compose state so [ProximityApp] can read them without any lambdas.
     */
    sealed interface AppState : Event {
        data class ActiveFriendChanged(val friend: Friend) : AppState
        data class ChatSeedChanged(val seed: String?) : AppState
        data class PendingMementoChanged(val memento: MementoData?) : AppState
    }
}

@Singleton
class EventBus @Inject constructor() {
    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()
    fun emit(event: Event) {
        _events.tryEmit(event)
    }
}