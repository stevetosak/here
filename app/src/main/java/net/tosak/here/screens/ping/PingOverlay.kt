package net.tosak.here.screens.ping

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tosak.here.shared.components.*
import net.tosak.here.shared.ping.IncomingPing
import net.tosak.here.shared.ping.PingType
import net.tosak.here.ui.theme.*

/**
 * Modal overlay for an incoming ping — centred card with ember accent shadow and
 * a blinking badge. AUTO pings read "@x is nearby"; MANUAL pings read "@x is
 * pinging you" and show the intent message. Actions: "I'm on my way" / "Ignore".
 */
@Composable
fun PingOverlay(
    incoming: IncomingPing,
    onOnMyWay: () -> Unit,
    onIgnore: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pingBlink")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue  = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1400
                1f at 0; 1f at 700; 0f at 701; 0f at 1400
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "blinkAlpha",
    )

    val manual = incoming.type == PingType.MANUAL
    val accent = if (manual) EmberAccent else EmberFg
    val badge = if (manual) "● PING" else "○ AUTO"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EmberBg.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
                .drawBehind {
                    drawRect(
                        color   = accent,
                        topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                        size    = size,
                    )
                }
                .border(1.dp, EmberFg)
                .background(EmberBg)
                .padding(22.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text(
                        badge,
                        style = TextStyle(
                            fontFamily    = JetBrainsMono,
                            fontSize      = 9.sp,
                            color         = accent.copy(alpha = blinkAlpha),
                            letterSpacing = 0.3.sp,
                        ),
                    )
                    Mono("${incoming.friend.dist}M AWAY", size = 9.sp, color = EmberMuted)
                }

                // Headline
                val headline = when {
                    incoming.groupedCount > 1 -> "@${incoming.friend.id} +${incoming.groupedCount - 1} more"
                    manual                    -> "@${incoming.friend.id} is pinging you"
                    else                      -> "@${incoming.friend.id} is nearby"
                }
                Text(
                    headline,
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 22.sp, color = EmberFg, lineHeight = 30.sp),
                )

                // Manual intent message, or the auto sub-line
                if (manual && !incoming.intentMessage.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "“${incoming.intentMessage}”",
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 18.sp, color = EmberAccent, lineHeight = 24.sp),
                    )
                } else if (!manual) {
                    Text(
                        if (incoming.groupedCount > 1) "are nearby." else "and is live.",
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 22.sp, color = EmberMuted, lineHeight = 30.sp),
                    )
                }

                Spacer(Modifier.height(16.dp))
                Rule()
                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PxButton("IGNORE", onClick = onIgnore)
                    PxButton("I'M ON MY WAY", onClick = onOnMyWay, modifier = Modifier.weight(1f), primary = true)
                }
            }
        }
    }
}
