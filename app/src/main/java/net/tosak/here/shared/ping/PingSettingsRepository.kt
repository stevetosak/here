package net.tosak.here.shared.ping

import androidx.core.content.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.tosak.here.shared.storage.AppStorage
import net.tosak.here.shared.storage.FriendPingSettingsDao
import net.tosak.here.shared.storage.FriendPingSettingsEntity
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads/writes ping configuration. Per-friend auto-ping flag + cooldown
 * timestamps live in Room ([FriendPingSettingsDao]); the global pause and the
 * active-hours window live in [AppStorage] (they apply app-wide).
 *
 * No backend — everything is local/mocked.
 */
@Singleton
class PingSettingsRepository @Inject constructor(
    private val dao: FriendPingSettingsDao,
    private val appStorage: AppStorage,
) {

    // ── Per-friend auto-ping toggle ────────────────────────────────────────────

    fun autoPingEnabled(friendId: String): Flow<Boolean> =
        dao.observe(friendId).map { it?.autoPingEnabled ?: false }

    suspend fun setAutoPing(friendId: String, enabled: Boolean) {
        val current = dao.get(friendId)
        dao.upsert(
            (current ?: FriendPingSettingsEntity(friendId, false, null, null))
                .copy(autoPingEnabled = enabled),
        )
    }

    suspend fun clear(friendId: String) = dao.deleteById(friendId)

    // ── Cooldowns ──────────────────────────────────────────────────────────────

    suspend fun canAutoPing(friendId: String): Boolean {
        val s = dao.get(friendId) ?: return false
        if (!s.autoPingEnabled) return false
        val last = s.lastAutoPingAt ?: return true
        return System.currentTimeMillis() - last >= AUTO_COOLDOWN_MS
    }

    suspend fun recordAutoPing(friendId: String) {
        val s = dao.get(friendId) ?: FriendPingSettingsEntity(friendId, true, null, null)
        dao.upsert(s.copy(lastAutoPingAt = System.currentTimeMillis()))
    }

    suspend fun canManualPing(friendId: String): Boolean {
        val s = dao.get(friendId) ?: return true
        val last = s.lastManualPingAt ?: return true
        return System.currentTimeMillis() - last >= MANUAL_COOLDOWN_MS
    }

    suspend fun recordManualPing(friendId: String) {
        val s = dao.get(friendId) ?: FriendPingSettingsEntity(friendId, false, null, null)
        dao.upsert(s.copy(lastManualPingAt = System.currentTimeMillis()))
    }

    // ── Global pause + active-hours window ─────────────────────────────────────

    var pingsPaused: Boolean
        get() = appStorage.prefs.getBoolean(AppStorage.KEY_PINGS_PAUSED, false)
        set(value) = appStorage.prefs.edit { putBoolean(AppStorage.KEY_PINGS_PAUSED, value) }

    val activeHoursStartMin: Int
        get() = appStorage.prefs.getInt(AppStorage.KEY_PING_HOURS_START, 0)

    val activeHoursEndMin: Int
        get() = appStorage.prefs.getInt(AppStorage.KEY_PING_HOURS_END, 24 * 60)

    fun setActiveHours(startMin: Int, endMin: Int) {
        appStorage.prefs.edit {
            putInt(AppStorage.KEY_PING_HOURS_START, startMin)
            putInt(AppStorage.KEY_PING_HOURS_END, endMin)
        }
    }

    /** True when [nowMin] (minutes-of-day) falls inside the active window. */
    fun withinActiveHours(nowMin: Int = currentMinuteOfDay()): Boolean {
        val start = activeHoursStartMin
        val end = activeHoursEndMin
        if (start == end) return true // 0..1440 (or equal) == all hours
        return if (start < end) nowMin in start until end
        else nowMin >= start || nowMin < end // window wraps past midnight
    }

    fun formatActiveHours(): String {
        val start = activeHoursStartMin
        val end = activeHoursEndMin
        return if (start == 0 && end == 24 * 60) "all hours"
        else "${formatMin(start)} – ${formatMin(end)}"
    }

    companion object {
        const val AUTO_COOLDOWN_MS = 2 * 60 * 60 * 1000L   // 2 hours
        const val MANUAL_COOLDOWN_MS = 10 * 60 * 1000L     // 10 minutes

        fun currentMinuteOfDay(): Int {
            val c = Calendar.getInstance()
            return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
        }

        private fun formatMin(min: Int): String {
            val h = (min / 60) % 24
            val m = min % 60
            return "%02d:%02d".format(h, m)
        }
    }
}
