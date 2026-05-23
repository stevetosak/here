package net.tosak.here.shared.auth

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.tosak.here.shared.storage.AppStorage
import net.tosak.here.shared.storage.AppStorage.Companion.KEY_HANDLE
import net.tosak.here.shared.storage.AppStorage.Companion.KEY_TOKEN

/**
 * Thin SharedPreferences wrapper that simulates a server-issued token.
 *
 * On real auth the token would come from the backend; here we generate a UUID
 * locally so the rest of the app can treat "is token present = logged in" as
 * the single source of truth without any network dependency.
 *
 * Keys are intentionally minimal — only what the app actually needs to restore
 * a session:  a token (presence = authenticated) and the chosen handle.
 */
@Singleton
class AuthRepository @Inject constructor(
    val appStorage: AppStorage
) {
    /** True when a token is present on disk. */
    val isAuthenticated: Boolean
        get() = appStorage.prefs.getString(KEY_TOKEN, null) != null

    /** The handle saved during onboarding; falls back to "you" if absent. */
    val handle: String
        get() = appStorage.prefs.getString(KEY_HANDLE, "you") ?: "you"

    // Hot flow so both AuthViewModel and NavigationViewModel can independently
    // observe auth state changes without polling SharedPreferences.
    private val _isAuthenticatedFlow = MutableStateFlow(isAuthenticated)
    val isAuthenticatedFlow: StateFlow<Boolean> = _isAuthenticatedFlow.asStateFlow()

    /**
     * Persist a new session.  Generates a fresh UUID token each time so a
     * re-onboard creates a distinct session rather than reusing the old one.
     */
    fun saveSession(handle: String) {
        appStorage.prefs.edit {
            putString(KEY_TOKEN, UUID.randomUUID().toString())
                .putString(KEY_HANDLE, handle.ifBlank { "you" })
        }
        _isAuthenticatedFlow.value = true
    }

    /** Wipe everything — equivalent to signing out. */
    fun clearSession() {
        appStorage.prefs.edit { clear() }
        _isAuthenticatedFlow.value = false
    }
}