package net.tosak.here.shared.ping

import net.tosak.here.shared.model.Friend
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends manual pings. No backend — "sending" only records the anti-spam
 * cooldown locally. The [intent] is part of the contract a real backend would
 * deliver to the recipient.
 */
@Singleton
class PingRepository @Inject constructor(
    private val pingSettings: PingSettingsRepository,
) {
    enum class SendResult { SENT, ON_COOLDOWN }

    suspend fun sendManualPing(friend: Friend, intent: String?): SendResult {
        if (!pingSettings.canManualPing(friend.id)) return SendResult.ON_COOLDOWN
        pingSettings.recordManualPing(friend.id)
        return SendResult.SENT
    }
}
