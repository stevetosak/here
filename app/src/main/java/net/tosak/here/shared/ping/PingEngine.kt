package net.tosak.here.shared.ping

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.tosak.here.shared.events.Event
import net.tosak.here.shared.events.EventBus
import net.tosak.here.shared.model.Friend
import net.tosak.here.shared.storage.AppStorage
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Drives AUTO pings. Watches [ProximityRepository] for friends entering the
 * radius and fires an auto ping when all rules pass:
 *  - the friend has auto-ping enabled
 *  - the receiver is present (presence ON)
 *  - inside the global active-hours window
 *  - the 2h per-friend cooldown has elapsed
 *  - pings aren't globally paused
 *
 * Arrivals within [GROUP_WINDOW_MS] of each other are coalesced into a single
 * notification. Also exposes a debug hook to simulate an incoming MANUAL ping.
 */
@Singleton
class PingEngine @Inject constructor(
    private val proximityRepository: ProximityRepository,
    private val pingSettings: PingSettingsRepository,
    private val appStorage: AppStorage,
    private val eventBus: EventBus,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var started = false
    private var seeded = false
    private var knownInRadius: Set<String> = emptySet()

    private val mutex = Mutex()
    private val pendingAuto = mutableListOf<Friend>()
    private var groupJob: Job? = null

    /** Idempotent — starts the arrival watcher. Called once at app start. */
    fun start() {
        if (started) return
        started = true
        scope.launch {
            proximityRepository.nearbyFriends.collect { friends ->
                val inRadiusNow = friends.filter { it.dist <= ProximityRepository.RADIUS_M }
                val ids = inRadiusNow.map { it.id }.toSet()
                if (!seeded) {
                    // Seed the baseline so friends already nearby at launch don't fire.
                    knownInRadius = ids
                    seeded = true
                    return@collect
                }
                val entered = inRadiusNow.filter { it.id !in knownInRadius }
                knownInRadius = ids
                entered.forEach { handleArrival(it) }
            }
        }
    }

    private suspend fun handleArrival(friend: Friend) {
        if (pingSettings.pingsPaused) return
        if (!receiverPresent()) return
        if (!pingSettings.withinActiveHours()) return
        if (!pingSettings.canAutoPing(friend.id)) return
        pingSettings.recordAutoPing(friend.id)
        bufferAutoPing(friend)
    }

    private suspend fun bufferAutoPing(friend: Friend) {
        mutex.withLock {
            pendingAuto.add(friend)
            groupJob?.cancel()
            groupJob = scope.launch {
                delay(GROUP_WINDOW_MS)
                flushGroup()
            }
        }
    }

    private suspend fun flushGroup() {
        mutex.withLock {
            if (pendingAuto.isEmpty()) return
            val first = pendingAuto.first()
            val count = pendingAuto.size
            pendingAuto.clear()
            emitIncoming(first, PingType.AUTO, null, count)
        }
    }

    /** Debug: surface an incoming MANUAL ping immediately, bypassing the rules. */
    fun simulateIncomingManualPing() {
        val friends = proximityRepository.nearbyFriends.value
        val friend = friends.firstOrNull { it.dist <= ProximityRepository.RADIUS_M }
            ?: friends.firstOrNull()
            ?: return
        emitIncoming(friend, PingType.MANUAL, "coffee?", 1)
    }

    private fun emitIncoming(friend: Friend, type: PingType, intent: String?, count: Int) {
        val event = PingEvent(
            id = UUID.randomUUID().toString(),
            senderId = friend.id,
            receiverId = "you",
            type = type,
            intentMessage = intent,
            timestamp = System.currentTimeMillis(),
            location = friend.post.place ?: "nearby",
        )
        eventBus.emit(Event.Ping.Incoming(event, friend, count))
    }

    private fun receiverPresent(): Boolean =
        appStorage.prefs.getBoolean(AppStorage.KEY_PRESENCE_ENABLED, false)

    companion object {
        const val GROUP_WINDOW_MS = 1500L
    }
}
