package net.tosak.here

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.tosak.here.shared.events.Event
import net.tosak.here.shared.events.EventBus
import net.tosak.here.shared.model.AppScreen
import net.tosak.here.shared.notifications.PingNotifier
import net.tosak.here.shared.ping.ProximityRepository
import net.tosak.here.shared.storage.ChatRepository
import net.tosak.here.ui.ProximityApp
import net.tosak.here.ui.theme.HereTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var eventBus: EventBus
    @Inject lateinit var chatRepository: ChatRepository
    @Inject lateinit var proximityRepository: ProximityRepository

    // Instantiated so it begins posting notifications from launch.
    @Inject lateinit var pingNotifier: PingNotifier

    private val requestNotifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()
        setContent {
            HereTheme {
                ProximityApp()
            }
        }
        handlePingIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePingIntent(intent)
    }

    /** "I'm on my way" from a notification → reply + open the coordination thread. */
    private fun handlePingIntent(intent: Intent?) {
        val friendId = intent?.getStringExtra(EXTRA_OPEN_DM_FRIEND) ?: return
        val friend = proximityRepository.nearbyFriends.value.firstOrNull { it.id == friendId } ?: return

        if (intent.getBooleanExtra(EXTRA_SEND_ON_MY_WAY, false)) {
            lifecycleScope.launch { chatRepository.sendText(friendId, "On my way!") }
        }
        eventBus.emit(Event.AppState.ActiveFriendChanged(friend))
        eventBus.emit(Event.Nav.NavigateTo(AppScreen.DM))

        // Clear so configuration changes don't replay it.
        intent.removeExtra(EXTRA_OPEN_DM_FRIEND)
        intent.removeExtra(EXTRA_SEND_ON_MY_WAY)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    companion object {
        const val EXTRA_OPEN_DM_FRIEND = "open_dm_friend"
        const val EXTRA_SEND_ON_MY_WAY = "send_on_my_way"
    }
}
