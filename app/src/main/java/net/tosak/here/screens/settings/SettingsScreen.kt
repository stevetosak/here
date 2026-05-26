package net.tosak.here.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import net.tosak.here.screens.settings.viewmodel.SettingsViewModel
import net.tosak.here.shared.components.*
import net.tosak.here.ui.theme.*

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val handle = viewModel.handle
    var haptic by remember { mutableStateOf(true) }
    var sound  by remember { mutableStateOf(false) }
    var pingsPaused by remember { mutableStateOf(viewModel.pingsPaused) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EmberBg)
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Top bar with back button ───────────────────────────────────────────
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackButton(onClick = viewModel::onClose)
            Spacer(Modifier.weight(1f))
            Mono(
                text          = "SETTINGS",
                size          = 9.sp,
                color         = EmberMuted,
                letterSpacing = 0.3.sp,
                modifier      = Modifier.padding(end = 16.dp),
            )
        }

        Rule()

        // Profile
        Column(modifier = Modifier.padding(horizontal = 22.dp).padding(top = 24.dp, bottom = 14.dp)) {
            Mono("YOU", size = 10.sp, color = EmberMuted, letterSpacing = 0.3.sp)
            Spacer(Modifier.height(4.dp))
            Text("@${handle.ifBlank { "you" }}", style = TextStyle(fontFamily = JetBrainsMono, fontSize = 26.sp, color = EmberFg))
            Spacer(Modifier.height(4.dp))
            Mono("NO EMAIL ON FILE · NO PHONE ON FILE", size = 9.sp, color = EmberMuted, letterSpacing = 0.18.sp)
        }

        Rule()

        Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            // Visibility
            Mono("VISIBILITY", size = 10.sp, color = EmberMuted, letterSpacing = 0.3.sp)
            Spacer(Modifier.height(8.dp))
            SettingToggle("Auto-go-live when home", "off — go live is always manual", false, {}, disabled = true)
            SettingToggle("Friends-of-friends can see me", "off — only direct friends, ever", false, {}, disabled = true)
            SettingRow("Default presence duration", "2h", "how long a session stays live before auto-off")
            SettingRow("Radius", "400m", "content visible inside this circle")

            Spacer(Modifier.height(20.dp))
            // Pings
            Mono("PINGS", size = 10.sp, color = EmberMuted, letterSpacing = 0.3.sp)
            Spacer(Modifier.height(8.dp))
            SettingToggle(
                "Pause all pings",
                "silences auto + incoming pings everywhere",
                pingsPaused,
                { pingsPaused = it; viewModel.setPingsPaused(it) },
            )
            SettingToggle("Haptic on ping", null, haptic, { haptic = it })
            SettingToggle("Sound on ping", null, sound, { sound = it })
            SettingRow("Active hours", viewModel.activeHoursLabel, "auto pings only fire inside this window")

            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, EmberAccent)
                    .clickable(
                        indication        = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick           = viewModel::simulateIncomingPing,
                    )
                    .padding(14.dp),
            ) {
                Mono("↳ SIMULATE INCOMING PING", size = 11.sp, color = EmberAccent, letterSpacing = 0.22.sp)
            }

            Spacer(Modifier.height(20.dp))
            // Session
            Mono("SESSION", size = 10.sp, color = EmberMuted, letterSpacing = 0.3.sp)
            Spacer(Modifier.height(8.dp))
            SettingRow("Auth token", "on device", "uuid stored in shared prefs. never leaves the device.")
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, EmberFg)
                    .clickable(
                        indication        = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick           = viewModel::onSignOut,
                    )
                    .padding(14.dp),
            ) {
                Mono("↳ SIGN OUT", size = 11.sp, letterSpacing = 0.22.sp)
            }

            Spacer(Modifier.height(20.dp))
            // Data
            Mono("DATA", size = 10.sp, color = EmberMuted, letterSpacing = 0.3.sp)
            Spacer(Modifier.height(8.dp))
            SettingRow("Stored on this device", "3 KB", "handle, friend list. no posts.")
            SettingRow("Stored on server", "0 KB", "posts vanish at expiry. nothing else kept.")
            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, EmberFg)
                    .clickable(
                        indication        = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) {}
                    .padding(14.dp),
            ) {
                Mono("↳ DELETE EVERYTHING ABOUT ME", size = 11.sp, letterSpacing = 0.22.sp)
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}
