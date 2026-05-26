package net.tosak.here.shared.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import net.tosak.here.shared.events.Event
import net.tosak.here.shared.events.EventBus
import net.tosak.here.shared.ping.PingType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts a system notification for every incoming ping. AUTO and MANUAL pings are
 * visually distinguished (accent colour + title); both carry "I'm on my way" /
 * "Ignore" actions handled by [PingActionReceiver].
 */
@Singleton
class PingNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    eventBus: EventBus,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        createChannel()
        scope.launch {
            eventBus.events.filterIsInstance<Event.Ping.Incoming>().collect { notify(it) }
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pings",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "Friends nearby and direct pings" }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun notify(e: Event.Ping.Incoming) {
        if (!hasPermission()) return

        val manual = e.ping.type == PingType.MANUAL
        val title = when {
            e.groupedCount > 1 -> "@${e.friend.id} and ${e.groupedCount - 1} others are nearby"
            manual             -> "@${e.friend.id} is pinging you"
            else               -> "@${e.friend.id} is nearby"
        }
        val body = if (manual) (e.ping.intentMessage ?: "wants to meet up")
                   else "tap to see who's around"

        val notifId = e.friend.id.hashCode()

        val onMyWay = PendingIntent.getBroadcast(
            context, notifId * 2,
            PingActionReceiver.onMyWayIntent(context, e.friend.id, notifId),
            PI_FLAGS,
        )
        val ignore = PendingIntent.getBroadcast(
            context, notifId * 2 + 1,
            PingActionReceiver.ignoreIntent(context, notifId),
            PI_FLAGS,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setColor(if (manual) ACCENT else FOREGROUND)
            .setColorized(manual)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setAutoCancel(true)
            .addAction(0, "I'm on my way", onMyWay)
            .addAction(0, "Ignore", ignore)
            .build()

        NotificationManagerCompat.from(context).notify(notifId, notification)
    }

    private fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    companion object {
        const val CHANNEL_ID = "pings"
        private const val ACCENT = 0xFFFF5A3C.toInt()      // EmberAccent
        private const val FOREGROUND = 0xFFF4F4F4.toInt()  // EmberFg
        private val PI_FLAGS =
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    }
}
