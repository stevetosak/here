package net.tosak.here.shared.ping

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Shell-level ping state for [net.tosak.here.ui.ProximityApp]. Exposes the
 * current incoming ping for the overlay and eagerly starts the [PingEngine]
 * arrival watcher at app launch.
 */
@HiltViewModel
class PingShellViewModel @Inject constructor(
    private val incomingPingController: IncomingPingController,
    private val pingEngine: PingEngine,
) : ViewModel() {

    init { pingEngine.start() }

    val incomingPing: StateFlow<IncomingPing?> = incomingPingController.current

    fun onOnMyWay() = incomingPingController.onOnMyWay()
    fun onIgnore() = incomingPingController.onIgnore()
}
