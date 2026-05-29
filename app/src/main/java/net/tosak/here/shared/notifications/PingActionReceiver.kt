package net.tosak.here.shared.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import net.tosak.here.MainActivity

/**
 * Handles ping notification actions. "I'm on my way" relaunches the app into the
 * coordination thread (and sends the reply there); "Ignore" silently cancels.
 * Both dismiss the notification.
 */
class PingActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, 0)
        NotificationManagerCompat.from(context).cancel(notifId)

        when (intent.action) {
            ACTION_ON_MY_WAY -> {
                val friendId = intent.getStringExtra(EXTRA_FRIEND_ID) ?: return
                context.startActivity(
                    Intent(context, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        putExtra(MainActivity.EXTRA_OPEN_DM_FRIEND, friendId)
                        putExtra(MainActivity.EXTRA_SEND_ON_MY_WAY, true)
                    }
                )
            }
            ACTION_IGNORE -> Unit // silent — notification already cancelled
        }
    }

    companion object {
        const val ACTION_ON_MY_WAY = "net.tosak.here.action.PING_ON_MY_WAY"
        const val ACTION_IGNORE = "net.tosak.here.action.PING_IGNORE"
        const val EXTRA_FRIEND_ID = "friend_id"
        const val EXTRA_NOTIF_ID = "notif_id"

        fun onMyWayIntent(context: Context, friendId: String, notifId: Int): Intent =
            Intent(context, PingActionReceiver::class.java).apply {
                action = ACTION_ON_MY_WAY
                putExtra(EXTRA_FRIEND_ID, friendId)
                putExtra(EXTRA_NOTIF_ID, notifId)
            }

        fun ignoreIntent(context: Context, notifId: Int): Intent =
            Intent(context, PingActionReceiver::class.java).apply {
                action = ACTION_IGNORE
                putExtra(EXTRA_NOTIF_ID, notifId)
            }
    }
}
