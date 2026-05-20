package net.tosak.here.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tosak.here.model.Friend
import net.tosak.here.model.FriendStatus
import net.tosak.here.ui.theme.*

/**
 * Tactical square marker used as a Mapbox ViewAnnotation.
 * Renders [A] / [K] / [M] / [N] in the ember design language.
 * Friends with JUST_POSTED status show an expanding pulse ring.
 *
 * The overall composable is 64×64 dp so the pulse ring has room to breathe;
 * the square marker is 32×32 dp centered inside that space.
 * ViewAnnotationAnchor.CENTER will pin the GPS coordinate to the center of this view.
 */
@Composable
fun FriendMarkerView(
    friend: Friend,
    onClick: () -> Unit,
) {
    val showPulse = friend.status == FriendStatus.JUST_POSTED

    val infiniteTransition = rememberInfiniteTransition(label = "friendPulse")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1600, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulseProgress",
    )

    Box(
        modifier         = Modifier.size(64.dp),
        contentAlignment = Alignment.Center,
    ) {
        // ── Expanding pulse ring (JUST_POSTED only) ───────────────────────
        if (showPulse) {
            val ringSize = (32 + 28 * pulseProgress).dp
            Box(
                modifier = Modifier
                    .size(ringSize)
                    .border(
                        width = 1.dp,
                        color = EmberFg.copy(alpha = (1f - pulseProgress) * 0.75f),
                        shape = CircleShape,
                    ),
            )
        }

        // ── Square tactical marker ────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(EmberBg)
                .border(1.dp, EmberFg)
                .clickable(
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick           = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text  = friend.mark,
                style = TextStyle(
                    fontFamily = JetBrainsMono,
                    fontSize   = 11.sp,
                    color      = EmberFg,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
}