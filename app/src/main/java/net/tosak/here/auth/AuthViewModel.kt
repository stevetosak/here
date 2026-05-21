package net.tosak.here.auth

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
) : ViewModel() {

    // Backed by a StateFlow so ProximityApp can react to sign-out without
    // polling.  Initial value is read synchronously from SharedPreferences
    // (fast, no I/O on main thread) before the first frame is drawn.
    private val _isAuthenticated = MutableStateFlow(repository.isAuthenticated)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    /** The handle stored on disk; reflects the most recent saveSession call. */
    val savedHandle: String get() = repository.handle

    /** Persist a new session and flip the auth state. */
    fun saveSession(handle: String) {
        repository.saveSession(handle)
        _isAuthenticated.value = true
    }

    /** Wipe the local session; triggers navigation reset in ProximityApp. */
    fun signOut() {
        repository.clearSession()
        _isAuthenticated.value = false
    }
}