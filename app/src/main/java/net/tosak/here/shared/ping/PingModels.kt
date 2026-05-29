package net.tosak.here.shared.ping

import net.tosak.here.shared.model.Friend

enum class PingType { AUTO, MANUAL }

/**
 * A ping. Transient — never persisted (ping history is intentionally hidden).
 * Built for the in-app overlay + notification and as the shape a real backend
 * would exchange.
 */
data class PingEvent(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val type: PingType,
    val intentMessage: String?,   // MANUAL pings only
    val timestamp: Long,
    val location: String,         // reverse-geocoded venue/street (mocked)
)

data class FriendProximityState(
    val friendId: String,
    val isInRadius: Boolean,
    val distanceMeters: Float?,   // shown when outside radius
)

/** What the receiver sees — drives the overlay + notification. */
data class IncomingPing(
    val friend: Friend,
    val type: PingType,
    val intentMessage: String?,
    val groupedCount: Int = 1,
)

sealed interface PingUiState {
    data object Idle : PingUiState
    data object Composing : PingUiState   // intent input visible
    data object Sending : PingUiState
    data object Sent : PingUiState
    data class Error(val message: String) : PingUiState
}
