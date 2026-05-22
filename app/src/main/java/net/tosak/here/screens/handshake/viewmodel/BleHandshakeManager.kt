package net.tosak.here.screens.handshake.viewmodel

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.nio.ByteBuffer
import java.util.UUID
import javax.inject.Inject

/**
 * Manages BLE advertising and scanning for the proximity handshake feature.
 *
 * ## Advertisement layout
 * - **Primary data** — app service UUID (filter anchor)
 * - **Scan response** — manufacturer data: [APP_MAGIC (2 bytes)] + [session token (16 bytes)]
 *
 * No username or persistent identifier is included in the raw BLE payload.
 * Identity is resolved server-side after mutual session-token confirmation.
 *
 * ## iOS compatibility note
 * iOS randomises MAC addresses; matching must rely on the session token embedded in the
 * manufacturer data, not the device address. This class already uses token-based tracking.
 *
 * Caller is responsible for verifying BLE permissions before calling [startAdvertising] /
 * [startScanning]. Both methods are annotated @SuppressLint to make the intent explicit.
 */
public class BleHandshakeManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    // ── Constants ─────────────────────────────────────────────────────────────

    companion object {
        /**
         * 128-bit service UUID advertised by every "here" device during a handshake.
         * Chosen to be memorable without colliding with any registered Bluetooth SIG UUID.
         * "feed" as the 16-bit shorthand; full 128-bit form used in advertisement.
         */
        val APP_SERVICE_UUID: UUID = UUID.fromString("0000feed-0000-1000-8000-00805f9b34fb")
        val APP_PARCEL_UUID: ParcelUuid = ParcelUuid(APP_SERVICE_UUID)

        /** 2-byte manufacturer ID. Chosen arbitrarily for "here·open". */
        const val MANUFACTURER_ID = 0x484F  // 'H', 'O'

        /**
         * Magic prefix in manufacturer data payload.
         * Lets the scan callback quickly discard non-"here" manufacturer records
         * even when the UUID filter lets something through.
         */
        val APP_MAGIC = byteArrayOf(0x48, 0x52)  // 'H', 'R'

        /**
         * Ultra-low TX power intentionally limits effective range to ≈ 1–2 m.
         * Connection requires physical closeness.
         */
        const val TX_POWER = AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW

        /**
         * Minimum averaged RSSI (dBm) to qualify as a lock-on candidate.
         * At 1–2 m with ultra-low power, typical range is –70 to –50 dBm.
         * –78 is a deliberately permissive threshold to handle body occlusion.
         */
        const val LOCK_ON_RSSI_THRESHOLD = -78

        /** Minimum RSSI readings accumulated before a device is eligible for lock-on. */
        const val MIN_READINGS_FOR_LOCK_ON = 3
    }

    // ── State ─────────────────────────────────────────────────────────────────

    /** Fresh UUID generated for every new scan session; never reused. */
    private var sessionToken: UUID = UUID.randomUUID()

    /** Per-token RSSI history. Keys are remote session-token strings. */
    private val rssiReadings = mutableMapOf<String, MutableList<Int>>()

    // ── BLE handles ───────────────────────────────────────────────────────────

    private val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val btAdapter = btManager?.adapter

    private var advertiser: BluetoothLeAdvertiser? = null
    private var scanner: BluetoothLeScanner? = null
    private var advertiseCallback: AdvertiseCallback? = null
    private var scanCallback: ScanCallback? = null

    // ── Public API ────────────────────────────────────────────────────────────

    /** `true` if the device has BLE hardware and Bluetooth is currently enabled. */
    val isAvailable: Boolean get() = btAdapter?.isEnabled == true

    /** The session token this device is currently advertising. */
    val mySessionToken: String get() = sessionToken.toString()

    private val _discoveredFlow = MutableSharedFlow<DiscoveredDevice>(
        replay = 0,
        extraBufferCapacity = 128,
    )

    /**
     * Emits a [DiscoveredDevice] every time a new RSSI reading is recorded for a peer.
     * The [DiscoveredDevice.rssi] is the running average of the last 10 readings.
     */
    val discoveredFlow: SharedFlow<DiscoveredDevice> = _discoveredFlow

    /**
     * Resets session state. Call before each new handshake attempt so a fresh
     * session token is generated and old RSSI readings are cleared.
     */
    fun reset() {
        sessionToken = UUID.randomUUID()
        rssiReadings.clear()
    }

    /** Start BLE advertising. Caller must hold BLUETOOTH_ADVERTISE (API 31+). */
    @SuppressLint("MissingPermission")
    fun startAdvertising() {
        val adapter = btAdapter ?: return
        if (!adapter.isEnabled) return
        advertiser = adapter.bluetoothLeAdvertiser ?: return

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(TX_POWER)
            .setConnectable(false)
            .setTimeout(0)  // Lifecycle controlled externally via stop()
            .build()

        // Primary packet: service UUID for easy scan filtering
        val primaryData = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .addServiceUuid(APP_PARCEL_UUID)
            .build()

        // Scan response: session token (so it fits the 31-byte primary packet)
        val scanResponseData = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addManufacturerData(MANUFACTURER_ID, APP_MAGIC + sessionToken.toByteArray())
            .build()

        val cb = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) = Unit
            override fun onStartFailure(errorCode: Int) {
                _discoveredFlow.tryEmit(
                    DiscoveredDevice(sessionToken = "", rssi = errorCode, address = "advertise_error")
                )
            }
        }

        advertiseCallback = cb
        advertiser?.startAdvertising(settings, primaryData, scanResponseData, cb)
    }

    /** Start BLE scanning. Caller must hold BLUETOOTH_SCAN (API 31+). */
    @SuppressLint("MissingPermission")
    fun startScanning() {
        val adapter = btAdapter ?: return
        if (!adapter.isEnabled) return
        scanner = adapter.bluetoothLeScanner ?: return

        // Filter to only our app's service UUID
        val filter = ScanFilter.Builder()
            .setServiceUuid(APP_PARCEL_UUID)
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)  // Immediate delivery
            .build()

        val cb = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) =
                processScanResult(result)
            override fun onBatchScanResults(results: List<ScanResult>) =
                results.forEach(::processScanResult)
        }

        scanCallback = cb
        scanner?.startScan(listOf(filter), settings, cb)
    }

    /** Stop both advertiser and scanner. Safe to call multiple times. */
    @SuppressLint("MissingPermission")
    fun stop() {
        advertiseCallback?.let { advertiser?.stopAdvertising(it) }
        scanCallback?.let { scanner?.stopScan(it) }
        advertiser = null
        scanner = null
        advertiseCallback = null
        scanCallback = null
    }

    /**
     * Returns the single best peer for lock-on (highest average RSSI above threshold),
     * or `null` if no candidate has accumulated enough readings yet.
     * Use in single-device mode.
     */
    fun getBestCandidate(): DiscoveredDevice? =
        rssiReadings
            .filter { (_, readings) -> readings.size >= MIN_READINGS_FOR_LOCK_ON }
            .maxByOrNull { (_, readings) -> readings.average() }
            ?.let { (token, readings) ->
                val avg = readings.average().toInt()
                if (avg >= LOCK_ON_RSSI_THRESHOLD)
                    DiscoveredDevice(sessionToken = token, rssi = avg, address = "")
                else null
            }

    /**
     * Returns all peers eligible for lock-on (above threshold + enough readings).
     * Use in group mode.
     */
    fun getAllCandidates(): List<DiscoveredDevice> =
        rssiReadings
            .filter { (_, readings) -> readings.size >= MIN_READINGS_FOR_LOCK_ON }
            .mapNotNull { (token, readings) ->
                val avg = readings.average().toInt()
                if (avg >= LOCK_ON_RSSI_THRESHOLD)
                    DiscoveredDevice(sessionToken = token, rssi = avg, address = "")
                else null
            }
            .sortedByDescending { it.rssi }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private fun processScanResult(result: ScanResult) {
        val record = result.scanRecord ?: return

        // Extract manufacturer data from scan response
        val mfData = record.getManufacturerSpecificData(MANUFACTURER_ID) ?: return
        if (mfData.size < APP_MAGIC.size + 16) return

        // Verify magic prefix
        if (!mfData.sliceArray(APP_MAGIC.indices).contentEquals(APP_MAGIC)) return

        // Decode session token UUID
        val tokenBytes = mfData.copyOfRange(APP_MAGIC.size, APP_MAGIC.size + 16)
        val token = tokenBytes.toUuid().toString()

        // Ignore echoes of our own advertisement
        if (token == mySessionToken) return

        // Accumulate RSSI (keep last 10 per token to track movement)
        val readings = rssiReadings.getOrPut(token) { mutableListOf() }
        readings.add(result.rssi)
        if (readings.size > 10) readings.removeAt(0)

        _discoveredFlow.tryEmit(
            DiscoveredDevice(
                sessionToken = token,
                rssi = readings.average().toInt(),
                address = result.device?.address ?: "unknown",
            )
        )
    }

    // ── UUID ↔ ByteArray helpers ──────────────────────────────────────────────

    private fun UUID.toByteArray(): ByteArray =
        ByteBuffer.allocate(16).also { buf ->
            buf.putLong(mostSignificantBits)
            buf.putLong(leastSignificantBits)
        }.array()

    private fun ByteArray.toUuid(): UUID {
        val buf = ByteBuffer.wrap(this)
        return UUID(buf.long, buf.long)
    }
}
