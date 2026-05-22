package net.tosak.here.screens.handshake.viewmodel

/**
 * A discovered BLE peer while scanning.
 *
 * @param sessionToken  UUID the remote device embedded in its advertisement.
 * @param rssi          Running average of RSSI readings (dBm).
 * @param address       BLE device address (for de-duplication on Android; unused on iOS).
 */
data class DiscoveredDevice(
    val sessionToken: String,
    val rssi: Int,
    val address: String,
)

/**
 * Data embedded in the final Memento receipt screen.
 */
data class MementoData(
    val friendNickname: String,
    val location: String,
    val timestamp: Long,
)

/**
 * State machine for the handshake flow.
 *
 * ```
 * Idle → Scanning → LockOn → Confirmed
 *           ↓                    ↑
 *          Error ←──────────────╯ (on timeout / server error)
 * ```
 */
sealed class HandshakeState {

    /** Button not held; nothing happening. */
    data object Idle : HandshakeState()

    /** Button is held; BLE advertise + scan running. Haptic pulses slow. */
    data object Scanning : HandshakeState()

    /**
     * A nearby peer has been detected at sufficient RSSI.
     * Awaiting server confirmation of mutual selection.
     * Haptic pulses quicken.
     */
    data class LockOn(
        val sessionToken: String,
        val rssi: Int,
    ) : HandshakeState()

    /**
     * Server confirmed mutual handshake.
     * Triggers confirmation wave + Memento screen.
     */
    data class Confirmed(val memento: MementoData) : HandshakeState()

    /**
     * Unrecoverable error — BLE unavailable, permission denied, or timeout.
     * Shown as an inline error; user taps to reset.
     */
    data class Error(val message: String) : HandshakeState()
}
