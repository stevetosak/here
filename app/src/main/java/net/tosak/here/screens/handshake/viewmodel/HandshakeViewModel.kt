package net.tosak.here.screens.handshake.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.tosak.here.shared.events.Event
import net.tosak.here.shared.events.EventBus
import net.tosak.here.shared.model.AppScreen
import javax.inject.Inject

/**
 * Coordinates the BLE handshake state machine.
 *
 * Responsibilities:
 * - Start / stop [BleHandshakeManager] in response to button hold events
 * - Poll discovered devices and advance the state to [HandshakeState.LockOn]
 * - Simulate server confirmation (replace with WebSocket push in production)
 * - Handle group mode (connect all mutual detections in one gesture)
 * - Enforce a hold timeout so the user isn't left hanging forever
 */
@HiltViewModel
class HandshakeViewModel @Inject constructor(
    private val ble: BleHandshakeManager,
    private val eventBus: EventBus,
) : ViewModel() {

    companion object {
        /** Maximum time (ms) a scan attempt may run before auto-cancelling. */
        const val HOLD_TIMEOUT_MS = 30_000L

        /** Simulated server round-trip (ms). Replace with real WebSocket await. */
        private const val SIMULATED_SERVER_MS = 2_300L

        /** Polling interval (ms) while checking for BLE lock-on candidates. */
        private const val CANDIDATE_POLL_MS = 500L

        /** Delay (ms) before the first candidate check — lets RSSI readings accumulate. */
        private const val SCAN_WARMUP_MS = 1_500L

        // ── Demo data (used until a real backend is wired) ────────────────────

        private val SAMPLE_NICKNAMES = listOf(
            "phantom", "drift", "nova", "sol", "crest", "veil", "ash", "tide",
            "echo", "haze", "wren", "flint",
        )
        private val SAMPLE_LOCATIONS = listOf(
            "Debar Maalo", "Caffe Vinoteka", "City Park · Skopje",
            "Korzo", "Debarca", "Čair", "Bulevar Partizanski",
        )
    }

    // ── Public state ──────────────────────────────────────────────────────────

    private val _state = MutableStateFlow<HandshakeState>(HandshakeState.Idle)
    val state: StateFlow<HandshakeState> = _state.asStateFlow()

    /** `true` → connect all mutual detections in one gesture (group mode). */
    private var groupMode = false

    // ── Internal jobs ─────────────────────────────────────────────────────────

    private var pollJob: Job? = null
    private var serverJob: Job? = null
    private var timeoutJob: Job? = null

    // ── BLE capability ────────────────────────────────────────────────────────

    val isBleAvailable: Boolean get() = ble.isAvailable

    // ── User actions ──────────────────────────────────────────────────────────

    fun setGroupMode(enabled: Boolean) { groupMode = enabled }

    /**
     * Call when the user begins holding the handshake button.
     * Idempotent — safe to call even if already scanning.
     */
    fun onButtonPressed() {
        val current = _state.value
        if (current is HandshakeState.Scanning ||
            current is HandshakeState.LockOn ||
            current is HandshakeState.Confirmed) return

        ble.reset()
        _state.value = HandshakeState.Scanning

        startBle()
        startCandidatePoller()
        startTimeout()
    }

    /**
     * Call when the user releases the handshake button.
     * A confirmed handshake is never cancelled by a release.
     */
    fun onButtonReleased() {
        if (_state.value is HandshakeState.Confirmed) return
        cancelScan()
        if (_state.value !is HandshakeState.Confirmed) {
            _state.value = HandshakeState.Idle
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    fun onConfirmed(memento: MementoData) {
        eventBus.emit(Event.AppState.PendingMementoChanged(memento))
        eventBus.emit(Event.Nav.ReplaceTop(AppScreen.MEMENTO))
    }

    fun onBack() {
        eventBus.emit(Event.Nav.GoBack)
    }

    /** Reset to Idle — call after an Error or after navigating away from Memento. */
    fun reset() {
        cancelScan()
        ble.stop()
        _state.value = HandshakeState.Idle
    }

    // ── Private scan logic ────────────────────────────────────────────────────

    private fun startBle() {
        ble.startAdvertising()
        ble.startScanning()
    }

    /**
     * Polls [BleHandshakeManager.getBestCandidate] every [CANDIDATE_POLL_MS] after
     * an initial warmup. When a candidate clears the RSSI threshold, advances to
     * LockOn and fires the (simulated) server confirmation.
     */
    private fun startCandidatePoller() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            delay(SCAN_WARMUP_MS)

            while (_state.value == HandshakeState.Scanning) {
                if (!groupMode) {
                    val best = ble.getBestCandidate()
                    if (best != null) {
                        _state.value = HandshakeState.LockOn(best.sessionToken, best.rssi)
                        pollJob = null
                        confirmWithServer(listOf(best))
                        return@launch
                    }
                } else {
                    // Group mode: collect all candidates above threshold
                    val all = ble.getAllCandidates()
                    if (all.isNotEmpty()) {
                        // Show lock-on for the closest one; server handles the full list
                        val primary = all.first()
                        _state.value = HandshakeState.LockOn(primary.sessionToken, primary.rssi)
                        pollJob = null
                        confirmWithServer(all)
                        return@launch
                    }
                }
                delay(CANDIDATE_POLL_MS)
            }
        }
    }

    /**
     * Simulates a server round-trip for mutual confirmation.
     *
     * In production, replace with:
     * 1. POST `/handshake/report` with our session token + candidate token(s)
     * 2. Await WebSocket push `handshake.confirmed` with friend nickname + location
     */
    private fun confirmWithServer(candidates: List<DiscoveredDevice>) {
        serverJob?.cancel()
        serverJob = viewModelScope.launch {
            delay(SIMULATED_SERVER_MS)

            // Pick a plausible-looking mock result
            val memento = MementoData(
                friendNickname = SAMPLE_NICKNAMES.random(),
                location       = SAMPLE_LOCATIONS.random(),
                timestamp      = System.currentTimeMillis(),
            )
            _state.value = HandshakeState.Confirmed(memento)
            ble.stop()
            cancelTimers()
        }
    }

    private fun startTimeout() {
        timeoutJob?.cancel()
        timeoutJob = viewModelScope.launch {
            delay(HOLD_TIMEOUT_MS)
            val s = _state.value
            if (s is HandshakeState.Scanning || s is HandshakeState.LockOn) {
                _state.value = HandshakeState.Error("No one found nearby. Try holding again.")
                cancelScan()
            }
        }
    }

    // ── Cleanup helpers ───────────────────────────────────────────────────────

    private fun cancelScan() {
        ble.stop()
        cancelTimers()
    }

    private fun cancelTimers() {
        pollJob?.cancel(); pollJob = null
        serverJob?.cancel(); serverJob = null
        timeoutJob?.cancel(); timeoutJob = null
    }

    override fun onCleared() {
        super.onCleared()
        ble.stop()
        cancelTimers()
    }
}
