package net.tosak.here.screens.handshake

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.tosak.here.screens.handshake.viewmodel.HandshakeState
import net.tosak.here.ui.theme.EmberAccent
import net.tosak.here.ui.theme.EmberFg

/**
 * Concentric radar rings that pulse outward from a central button.
 *
 * ## Scanning state
 * - 3 rings, each expanding at the same speed but offset in phase
 * - Neutral [EmberFg] colour, soft opacity — calm sonar feel
 * - Cycle duration: 2 s
 *
 * ## LockOn state
 * - Rings speed up (1.1 s cycle)
 * - Color shifts to [EmberAccent] — "target acquired"
 *
 * The animation idles immediately when neither Scanning nor LockOn.
 *
 * @param state         Current handshake state (drives colour + speed).
 * @param buttonRadius  Radius of the hold button in dp — rings start here.
 * @param size          Total canvas size.
 */
@Composable
fun RadarRingsAnimation(
    state: HandshakeState,
    buttonRadius: Dp = 80.dp,
    size: Dp = 280.dp,
    modifier: Modifier = Modifier,
) {
    val isActive = state is HandshakeState.Scanning || state is HandshakeState.LockOn
    val isLockOn = state is HandshakeState.LockOn

    // Ring speed — slower when scanning, faster on lock-on
    val cycleDurationMs = if (isLockOn) 1_100 else 2_000

    // Ring colour — neutral during scan, ember accent on lock-on
    val ringColor = if (isLockOn) EmberAccent else EmberFg

    // 3 rings with evenly-spaced phase offsets (0, 1/3, 2/3 of cycle)
    val transition = rememberInfiniteTransition(label = "radar")

    val ring1 = transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation          = tween(cycleDurationMs, easing = LinearEasing),
            repeatMode         = RepeatMode.Restart,
            initialStartOffset = StartOffset(0),
        ),
        label = "ring1",
    )
    val ring2 = transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation          = tween(cycleDurationMs, easing = LinearEasing),
            repeatMode         = RepeatMode.Restart,
            initialStartOffset = StartOffset((cycleDurationMs / 3f).toInt()),
        ),
        label = "ring2",
    )
    val ring3 = transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation          = tween(cycleDurationMs, easing = LinearEasing),
            repeatMode         = RepeatMode.Restart,
            initialStartOffset = StartOffset((cycleDurationMs * 2 / 3f).toInt()),
        ),
        label = "ring3",
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            if (!isActive) return@Canvas

            val center     = Offset(this.size.width / 2f, this.size.height / 2f)
            val btnRadiusPx = buttonRadius.toPx()
            val maxExpand  = (this.size.minDimension / 2f) - btnRadiusPx

            listOf(ring1.value, ring2.value, ring3.value).forEach { fraction ->
                val radius = btnRadiusPx + fraction * maxExpand
                val alpha  = (1f - fraction).coerceIn(0f, 1f) *
                    if (isLockOn) 0.85f else 0.55f

                drawCircle(
                    color  = ringColor.copy(alpha = alpha),
                    radius = radius,
                    center = center,
                    style  = Stroke(width = if (isLockOn) 1.8.dp.toPx() else 1.dp.toPx()),
                )
            }
        }
    }
}
