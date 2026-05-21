package net.tosak.here.auth

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

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
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** True when a token is present on disk. */
    val isAuthenticated: Boolean
        get() = prefs.getString(KEY_TOKEN, null) != null

    /** The handle saved during onboarding; falls back to "you" if absent. */
    val handle: String
        get() = prefs.getString(KEY_HANDLE, "you") ?: "you"

    /**
     * Persist a new session.  Generates a fresh UUID token each time so a
     * re-onboard creates a distinct session rather than reusing the old one.
     */
    fun saveSession(handle: String) {
        prefs.edit()
            .putString(KEY_TOKEN, UUID.randomUUID().toString())
            .putString(KEY_HANDLE, handle.ifBlank { "you" })
            .apply()
    }

    /** Wipe everything — equivalent to signing out. */
    fun clearSession() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "here_auth"
        private const val KEY_TOKEN  = "auth_token"
        private const val KEY_HANDLE = "user_handle"
    }
}