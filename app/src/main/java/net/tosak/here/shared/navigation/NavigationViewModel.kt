package net.tosak.here.shared.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import net.tosak.here.auth.AuthRepository
import net.tosak.here.model.AppScreen
import javax.inject.Inject

/**
 * Single source of truth for the app's navigation back-stack.
 *
 * Scoped to the Activity via Hilt — every call to [hiltViewModel] inside the
 * same Activity returns the **same instance**, so any composable or ViewModel
 * can drive navigation without passing callbacks up the tree.
 *
 * [backStack] is a [SnapshotStateList] so Compose automatically recomposes
 * any composable that reads from it when the stack changes.
 */
@HiltViewModel
class NavigationViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val backStack: SnapshotStateList<AppScreen> = mutableStateListOf(
        if (authRepository.isAuthenticated) AppScreen.MAP else AppScreen.ONBOARDING
    )

    /** The screen currently on top of the stack. */
    val current: AppScreen get() = backStack.last()

    init {
        // Mirror sign-out events from the repo: whenever auth is lost, reset
        // the entire stack to ONBOARDING so no protected screen stays visible.
        viewModelScope.launch {
            authRepository.isAuthenticatedFlow.collect { authed ->
                if (!authed) reset(AppScreen.ONBOARDING)
            }
        }
    }

    /** Push [screen] onto the stack. */
    fun navigate(screen: AppScreen) {
        backStack.add(screen)
    }

    /**
     * Pop the top screen. No-op when already at the root so the app never
     * ends up with an empty stack.
     */
    fun goBack() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    /**
     * Replace the top entry without leaving a back-stack entry behind.
     * Use this for in-flow transitions where the user should not be able to
     * swipe back to the previous screen (e.g. HANDSHAKE → MEMENTO).
     */
    fun replaceTop(screen: AppScreen) {
        if (backStack.isNotEmpty()) backStack.removeAt(backStack.lastIndex)
        backStack.add(screen)
    }

    /**
     * Discard the entire stack and set [screen] as the sole root entry.
     * Use this after onboarding completes or after sign-out so the user
     * cannot navigate back to a previous flow.
     */
    fun reset(screen: AppScreen) {
        backStack.clear()
        backStack.add(screen)
    }
}