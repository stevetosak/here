package net.tosak.here.screens.mapscreen.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tosak.here.shared.components.Mono
import net.tosak.here.shared.components.PxButton
import net.tosak.here.shared.components.Rule
import net.tosak.here.shared.model.Friend
import net.tosak.here.shared.model.FriendStatus
import net.tosak.here.shared.ping.PingUiState
import net.tosak.here.shared.ping.ProximityRepository
import net.tosak.here.ui.theme.*

private const val INTENT_MAX = 40

/**
 * Bottom sheet shown when a friend marker is tapped. In radius → chat + ping
 * (ping reveals an optional intent field); outside radius → chat only, with a
 * distance label instead of the ping button. When the friend has an active post,
 * a VIEW POST button is shown above the chat/ping row.
 */
@Composable
fun FriendActionSheet(
    friend: Friend,
    pingState: PingUiState,
    onChat: () -> Unit,
    onPing: () -> Unit,
    onSendPing: (String) -> Unit,
    onViewPost: () -> Unit,
    onDismiss: () -> Unit,
) {
    val inRadius = friend.dist <= ProximityRepository.RADIUS_M
    val composing = pingState is PingUiState.Composing
    var intent by remember(friend.id) { mutableStateOf("") }

    // Scrim — tap to dismiss
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EmberBg.copy(alpha = 0.7f))
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick           = onDismiss,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        // Sheet — absorbs taps so the scrim doesn't dismiss it
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(EmberBg)
                .border(1.dp, EmberFg)
                .clickable(
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {}
                .padding(22.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top,
            ) {
                Column {
                    Text(
                        "@${friend.id}",
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 22.sp, color = EmberFg),
                    )
                    Spacer(Modifier.height(2.dp))
                    val presence = if (friend.post != null) "JUST POSTED" else "LIVE"
                    Mono(presence, size = 9.sp, color = if (friend.post != null) EmberAccent else EmberMuted, letterSpacing = 0.18.sp)
                }
                Mono("${friend.dist}M AWAY", size = 9.sp, color = EmberMuted)
            }

            Spacer(Modifier.height(16.dp))
            Rule()
            Spacer(Modifier.height(16.dp))

            if (composing) {
                IntentComposer(
                    value         = intent,
                    onValueChange = { if (it.length <= INTENT_MAX) intent = it },
                    onSend        = { onSendPing(intent) },
                )
            } else {
                if (friend.post != null) {
                    PxButton(
                        text     = "⊡ VIEW POST",
                        onClick  = onViewPost,
                        modifier = Modifier.fillMaxWidth(),
                        primary  = true,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PxButton("CHAT", onClick = onChat, modifier = Modifier.weight(1f))
                    if (inRadius) {
                        PingButton(onClick = onPing, modifier = Modifier.weight(1f))
                    } else {
                        Box(
                            modifier = Modifier.weight(1f).border(1.dp, EmberBorder).padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Mono("${friend.dist}M · OUT OF RANGE", size = 10.sp, color = EmberMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PingButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    // Subtle pulse to draw attention to the in-the-moment action.
    val transition = rememberInfiniteTransition(label = "pingPulse")
    val alpha by transition.animateFloat(
        initialValue  = 0.45f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pingAlpha",
    )
    Box(
        modifier = modifier
            .border(1.dp, EmberAccent.copy(alpha = alpha))
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick           = onClick,
            )
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Mono("● PING", size = 12.sp, color = EmberAccent, letterSpacing = 0.22.sp)
    }
}

@Composable
private fun IntentComposer(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Column {
        Mono("ADD AN INTENT — OPTIONAL", size = 9.sp, color = EmberMuted, letterSpacing = 0.18.sp)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, EmberFg.copy(alpha = 0.33f))
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BasicTextField(
                value         = value,
                onValueChange = onValueChange,
                modifier      = Modifier.weight(1f),
                textStyle     = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp, color = EmberFg),
                cursorBrush   = SolidColor(EmberAccent),
                singleLine    = true,
                decorationBox = { inner ->
                    if (value.isEmpty()) Text(
                        "coffee? · come outside · at the park…",
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp, color = EmberMuted),
                        maxLines = 1,
                    )
                    inner()
                },
            )
            Mono("${value.length}/$INTENT_MAX", size = 8.sp, color = EmberMuted)
        }
        Spacer(Modifier.height(12.dp))
        PxButton("SEND PING →", onClick = onSend, modifier = Modifier.fillMaxWidth(), primary = true)
    }
}
