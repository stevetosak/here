package net.tosak.here.screens

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
import net.tosak.here.ui.components.*
import net.tosak.here.ui.theme.*

@Composable
fun SettingsScreen(
    handle: String,
    onClose: () -> Unit,
) {
    var haptic by remember { mutableStateOf(true) }
    var sound  by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EmberBg)
            .verticalScroll(rememberScrollState()),
    ) {
        HudStrip(presenceOn = true, minimal = true)

        // Profile
        Column(modifier = Modifier.padding(horizontal = 22.dp).padding(top = 52.dp, bottom = 14.dp)) {
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
            SettingToggle("Haptic on ping", null, haptic, { haptic = it })
            SettingToggle("Sound on ping", null, sound, { sound = it })
            SettingRow("Quiet hours", "00:00 – 08:00", "pings silenced while you sleep")

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

        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp).fillMaxWidth()) {
            PxButton("← BACK TO MAP", onClick = onClose, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SettingRow(label: String, value: String, hint: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(label, style = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp, color = EmberFg))
            hint?.let { Spacer(Modifier.height(2.dp)); Mono(it, size = 9.sp, color = EmberMuted, letterSpacing = 0.10.sp) }
        }
        Mono(value, size = 11.sp, color = EmberAccent, letterSpacing = 0.18.sp)
    }
    Rule(dashed = true, color = EmberFg.copy(alpha = 0.13f))
}

@Composable
private fun SettingToggle(
    label: String,
    hint: String?,
    value: Boolean,
    onChange: (Boolean) -> Unit,
    disabled: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .then(if (disabled) Modifier else Modifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp).then(if (disabled) Modifier else Modifier)) {
            Text(label, style = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp, color = if (disabled) EmberMuted else EmberFg))
            hint?.let { Spacer(Modifier.height(2.dp)); Mono(it, size = 9.sp, color = EmberMuted, letterSpacing = 0.10.sp) }
        }
        // Minimal toggle
        Box(
            modifier = Modifier
                .size(width = 50.dp, height = 22.dp)
                .background(if (value && !disabled) EmberAccent else Color.Transparent)
                .border(1.dp, if (disabled) EmberMuted else EmberFg)
                .then(if (!disabled) Modifier.clickable(
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { onChange(!value) } else Modifier),
        ) {
            Box(
                modifier = Modifier
                    .size(width = 19.dp, height = 18.dp)
                    .padding(1.dp)
                    .offset(x = if (value) 29.dp else 1.dp)
                    .background(if (value) EmberBg else if (disabled) EmberMuted else EmberFg),
            )
        }
    }
    Rule(dashed = true, color = EmberFg.copy(alpha = 0.13f))
}