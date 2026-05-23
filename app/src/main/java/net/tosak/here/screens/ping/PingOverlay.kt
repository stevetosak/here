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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tosak.here.shared.model.Friend
import net.tosak.here.shared.components.*
import net.tosak.here.ui.theme.*

/**
 * Modal ping overlay — centred card with ember accent shadow and blinking "● PING" badge.
 * This is the "modal" style from the design (default / selected by user).
 */
@Composable
fun PingOverlay(
    friend: Friend,
    onSee: () -> Unit,
    onDismiss: () -> Unit,
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

    // Scrim
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EmberBg.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center,
    ) {
        // Card with ember offset shadow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
                .drawBehind {
                    // offset shadow in accent color
                    drawRect(
                        color   = EmberAccent,
                        topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                        size    = size,
                    )
                }
                .border(1.dp, EmberFg)
                .background(EmberBg)
                .padding(22.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                // Top row: ping badge + distance
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text(
                        "● PING",
                        style = TextStyle(
                            fontFamily    = JetBrainsMono,
                            fontSize      = 9.sp,
                            color         = EmberAccent.copy(alpha = blinkAlpha),
                            letterSpacing = 0.3.sp,
                        ),
                    )
                    Mono("${friend.dist}M AWAY", size = 9.sp, color = EmberMuted)
                }

                // Main copy
                Text(
                    "@${friend.id} is nearby",
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 22.sp, color = EmberFg, lineHeight = 30.sp),
                )
                Text(
                    "and just posted.",
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 22.sp, color = EmberMuted, lineHeight = 30.sp),
                )

                Spacer(Modifier.height(16.dp))
                Rule()
                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PxButton("DISMISS", onClick = onDismiss)
                    PxButton("SEE POST →", onClick = onSee, modifier = Modifier.weight(1f), primary = true)
                }
            }
        }
    }
}
