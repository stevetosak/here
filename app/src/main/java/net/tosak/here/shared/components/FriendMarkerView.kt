package net.tosak.here.shared.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tosak.here.shared.model.Friend
import net.tosak.here.shared.model.FriendStatus
import net.tosak.here.shared.model.PostKind
import net.tosak.here.ui.theme.*

/**
 * Callout-pin marker. Two visual modes:
 *
 * Has post (JUST_POSTED):
 *   ┌──────────────────────────┐   ← EmberAccent border
 *   │  ⊡ · @ALEX · 47M        │
 *   └───────────┬──────────────┘
 *               │                 ← accent stem
 *               ◉                 ← accent dot + pulse ring
 *
 * No post (LIVE):
 *   ┌──────────────┐              ← dim EmberFg@40% border
 *   │  @KRIS · 180M│
 *   └──────┬───────┘
 *           │                     ← dim stem
 *           ◉                     ← dim dot, no pulse
 */
@Composable
fun FriendMarkerView(
    friend: Friend,
    onClick: () -> Unit,
) {
    val hasPost     = friend.post != null
    val markerColor = if (hasPost) EmberAccent else EmberFg.copy(alpha = 0.45f)

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

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .alpha(if (hasPost) 1f else 0.60f)
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick           = onClick,
            ),
    ) {

        // ── Label card ────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .background(EmberBg)
                .border(0.5.dp, markerColor)
                .padding(horizontal = 8.dp, vertical = 5.dp),
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                if (hasPost) {
                    Mono(postGlyph(friend.post!!.kind), size = 9.sp, color = markerColor, letterSpacing = 0.sp)
                    Mono("·",                           size = 9.sp, color = EmberMuted,  letterSpacing = 0.sp)
                }
                Mono("@${friend.id}", size = 9.sp, color = EmberFg,    letterSpacing = 0.12.sp)
                Mono("·",             size = 9.sp, color = EmberMuted,  letterSpacing = 0.sp)
                Mono("${friend.dist}m", size = 8.sp, color = EmberMuted, letterSpacing = 0.10.sp)
            }
        }

        // ── Stem ──────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(6.dp)
                .background(markerColor),
        )

        // ── Pin dot with optional pulse ring ─────────────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier         = Modifier.size(28.dp),
        ) {
            if (hasPost) {
                val ringDp = (7 + 16 * pulseProgress).dp
                Box(
                    modifier = Modifier
                        .size(ringDp)
                        .border(
                            width = 1.dp,
                            color = EmberAccent.copy(alpha = (1f - pulseProgress) * 0.70f),
                            shape = CircleShape,
                        ),
                )
            }
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(markerColor, CircleShape),
            )
        }
    }
}

/** Maps [PostKind] to a single monospace-safe glyph. */
private fun postGlyph(kind: PostKind): String = when (kind) {
    PostKind.PHOTO -> "⊡"
    PostKind.TEXT  -> "≡"
}