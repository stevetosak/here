package net.tosak.here.shared.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Per-friend ping configuration. Active-hours and the global pause live in
 * [AppStorage] (they are app-wide, not per-friend); only the auto-ping toggle
 * and cooldown timestamps are stored here.
 */
@Entity(tableName = "friend_ping_settings")
data class FriendPingSettingsEntity(
    @PrimaryKey val friendId: String,
    val autoPingEnabled: Boolean,
    val lastAutoPingAt: Long?,    // cooldown anchor for AUTO pings
    val lastManualPingAt: Long?,  // anti-spam anchor for MANUAL pings
)
