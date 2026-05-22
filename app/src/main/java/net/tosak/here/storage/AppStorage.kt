package net.tosak.here.storage

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AppStorage @Inject constructor(@ApplicationContext context: Context) {

    val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "here_storage"
        const val KEY_TOKEN = "auth_token"
        const val KEY_HANDLE = "user_handle"
        const val KEY_PRESENCE = "presence"
    }
}