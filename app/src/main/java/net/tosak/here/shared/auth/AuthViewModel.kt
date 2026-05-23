package net.tosak.here.shared.auth

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
) : ViewModel() {

    // Delegate directly to the repository's flow — single source of truth,
    // no duplicated state that could drift out of sync.
    val isAuthenticated: StateFlow<Boolean> = repository.isAuthenticatedFlow

    /** The handle stored on disk; reflects the most recent saveSession call. */
    val savedHandle: String get() = repository.handle

    /** Persist a new session and flip the auth state. */
    fun saveSession(handle: String) = repository.saveSession(handle)

    /** Wipe the local session; NavigationViewModel reacts via the shared flow. */
    fun signOut() = repository.clearSession()
}