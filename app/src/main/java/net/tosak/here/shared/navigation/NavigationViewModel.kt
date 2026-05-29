package net.tosak.here.shared.navigation

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import net.tosak.here.screens.handshake.viewmodel.MementoData
import net.tosak.here.shared.auth.AuthRepository
import net.tosak.here.shared.events.Event
import net.tosak.here.shared.events.EventBus
import net.tosak.here.shared.model.AppScreen
import net.tosak.here.shared.model.Friend
import net.tosak.here.shared.state.AppStateRepository
import javax.inject.Inject

/**
 * Single source of truth for the navigation back-stack.
 *
 * Scoped to the Activity via Hilt — every [hiltViewModel] call inside the same
 * Activity returns the **same instance**.
 *
 * Navigation is fully event-driven: screens emit [Event.Nav] via [EventBus]
 * instead of calling lambdas passed from [ProximityApp].
 *
 * Cross-screen data ([activeFriend], [chatSeed], [pendingMemento]) is owned
 * and mutated by [AppStateRepository]; this ViewModel only exposes the flows
 * so [ProximityApp] has a single place to read all shell-level state.
 */
@HiltViewModel
class NavigationViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val eventBus: EventBus,
    appState: AppStateRepository,
) : ViewModel() {

    // ── Back-stack ────────────────────────────────────────────────────────────

    val backStack: SnapshotStateList<AppScreen> = mutableStateListOf(
        if (authRepository.isAuthenticated) AppScreen.MAP else AppScreen.ONBOARDING
    )

    /** The screen currently on top of the stack. */
    val current: AppScreen get() = backStack.last()

    // ── Cross-screen data (delegated to AppStateRepository) ───────────────────

    val activeFriend:   StateFlow<Friend?>      = appState.activeFriend
    val chatSeed:       StateFlow<String?>      = appState.chatSeed
    val pendingMemento: StateFlow<MementoData?> = appState.pendingMemento

    // ── Toast ─────────────────────────────────────────────────────────────────

    private val _toast = MutableStateFlow<String?>(null)
    /** Non-null while a toast banner should be visible. Auto-clears after 2.2 s. */
    val toast: StateFlow<String?> = _toast.asStateFlow()

    // ── Loading overlay ───────────────────────────────────────────────────────

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ── Event subscriptions ───────────────────────────────────────────────────

    init {
        // Reset the stack to ONBOARDING whenever auth is lost.
        viewModelScope.launch {
            authRepository.isAuthenticatedFlow.collect { authed ->
                if (!authed) reset(AppScreen.ONBOARDING)
            }
        }

        // Show toast banners emitted by any screen VM.
        viewModelScope.launch {
            eventBus.events
                .filterIsInstance<Event.Toast>()
                .collect { event ->
                    val msg = when (event) {
                        is Event.Toast.Show      -> event.message
                        is Event.Toast.ShowError -> event.message
                    }
                    _toast.value = msg
                    delay(2_200)
                    _toast.value = null
                }
        }

        // Drive the global loading overlay.
        viewModelScope.launch {
            eventBus.events
                .filterIsInstance<Event.Loading>()
                .collect { event -> _isLoading.value = event is Event.Loading.Show }
        }

        // Handle navigation events emitted by any screen VM.
        viewModelScope.launch {
            eventBus.events
                .filterIsInstance<Event.Nav>()
                .collect { event ->
                    when (event) {
                        is Event.Nav.GoBack             -> goBack()
                        is Event.Nav.NavigateTo         -> navigate(event.screen)
                        is Event.Nav.Reset              -> reset(event.screen)
                        is Event.Nav.ReplaceTop         -> replaceTop(event.screen)
                        is Event.Nav.AppendMultiple     -> appendMultiple(event.screens)
                        is Event.Nav.ReplaceTopAndAppend -> replaceTopAndAppend(event.screens)
                    }
                }
        }
    }

    // ── Stack operations ──────────────────────────────────────────────────────

    fun navigate(screen: AppScreen) { backStack.add(screen) }

    fun goBack() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    fun replaceTop(screen: AppScreen) {
        if (backStack.isNotEmpty()) backStack.removeAt(backStack.lastIndex)
        backStack.add(screen)
    }

    fun appendMultiple(screens: List<AppScreen>) {
        backStack.addAll(screens)
    }

    fun replaceTopAndAppend(screens: List<AppScreen>) {
        if (backStack.isNotEmpty()) backStack.removeAt(backStack.lastIndex)
        backStack.addAll(screens)
    }

    fun reset(screen: AppScreen) {
        backStack.clear()
        backStack.add(screen)
    }
}