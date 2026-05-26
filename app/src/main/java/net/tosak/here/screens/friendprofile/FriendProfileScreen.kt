package net.tosak.here.screens.friendprofile

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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.tosak.here.screens.friendprofile.viewmodel.FriendProfileViewModel
import net.tosak.here.shared.components.*
import net.tosak.here.shared.model.FriendStatus
import net.tosak.here.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FriendProfileScreen(
    viewModel: FriendProfileViewModel = hiltViewModel(),
) {
    val friend by viewModel.friend.collectAsStateWithLifecycle()
    val autoPing by viewModel.autoPingEnabled.collectAsStateWithLifecycle()
    val memento by viewModel.memento.collectAsStateWithLifecycle()

    var confirmUnfriend by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EmberBg)
            .verticalScroll(rememberScrollState()),
    ) {
        // Top bar
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackButton(onClick = viewModel::onBack)
            Spacer(Modifier.weight(1f))
            Mono("PROFILE", size = 9.sp, color = EmberMuted, letterSpacing = 0.3.sp, modifier = Modifier.padding(end = 16.dp))
        }

        Rule()

        val f = friend
        if (f == null) {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                Mono("NO FRIEND SELECTED.", size = 10.sp, color = EmberMuted)
            }
            return@Column
        }

        // Identity
        Column(modifier = Modifier.padding(horizontal = 22.dp).padding(top = 24.dp, bottom = 14.dp)) {
            Mono("FRIEND", size = 10.sp, color = EmberMuted, letterSpacing = 0.3.sp)
            Spacer(Modifier.height(4.dp))
            Text("@${f.id}", style = TextStyle(fontFamily = JetBrainsMono, fontSize = 26.sp, color = EmberFg))
            Spacer(Modifier.height(4.dp))
            val presence = if (f.status == FriendStatus.JUST_POSTED) "JUST POSTED" else "LIVE"
            Mono("$presence · ${f.dist}M AWAY", size = 9.sp, color = EmberAccent, letterSpacing = 0.18.sp)
        }

        Rule()

        // Memento
        Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 22.dp)) {
            Mono("MEMENTO", size = 10.sp, color = EmberMuted, letterSpacing = 0.3.sp)
            Spacer(Modifier.height(10.dp))
            memento?.let { m ->
                Text(
                    "Connected near ${m.location}.",
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 14.sp, color = EmberFg, lineHeight = 20.sp),
                )
                Spacer(Modifier.height(4.dp))
                Mono(
                    if (m.connectedAt != null) "HANDSHAKE · ${formatDate(m.connectedAt)}" else "HANDSHAKE · A WHILE AGO",
                    size = 9.sp, color = EmberMuted, letterSpacing = 0.14.sp,
                )
            }
        }

        Rule()

        // Pings
        Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 22.dp)) {
            Mono("PINGS", size = 10.sp, color = EmberMuted, letterSpacing = 0.3.sp)
            Spacer(Modifier.height(8.dp))
            SettingToggle(
                label    = "Notify me when @${f.id} is nearby",
                hint     = null,
                value    = autoPing,
                onChange = viewModel::onToggleAutoPing,
            )
            if (autoPing) {
                Spacer(Modifier.height(10.dp))
                Mono("ACTIVE HOURS: ${viewModel.activeHoursLabel} · CHANGE IN SETTINGS", size = 9.sp, color = EmberMuted, letterSpacing = 0.10.sp)
                Spacer(Modifier.height(4.dp))
                Mono("@${f.id} CAN SEE THIS IS ON", size = 9.sp, color = EmberMuted, letterSpacing = 0.10.sp)
            }
        }

        Rule()

        // Unfriend
        Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 22.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (confirmUnfriend) EmberAccent else EmberFg)
                    .clickable(
                        indication        = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) {
                        if (confirmUnfriend) viewModel.onUnfriend() else confirmUnfriend = true
                    }
                    .padding(14.dp),
            ) {
                Mono(
                    if (confirmUnfriend) "↳ TAP AGAIN TO UNFRIEND @${f.id}" else "↳ UNFRIEND @${f.id}",
                    size = 11.sp,
                    color = if (confirmUnfriend) EmberAccent else EmberFg,
                    letterSpacing = 0.22.sp,
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

private val dateFmt = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
private fun formatDate(ts: Long): String = dateFmt.format(Date(ts))
