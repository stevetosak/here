package net.tosak.here.screens.handshake

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import net.tosak.here.ui.theme.EmberAccent
import net.tosak.here.ui.theme.EmberBg
import net.tosak.here.ui.theme.EmberFg

/**
 * Full-screen confirmation wave that sweeps across both screens simultaneously
 * when the server confirms a mutual handshake.
 *
 * Animation:
 * 1. A radial glow expands from the screen centre (0 → full radius, 0–60 % of duration)
 * 2. A bright vertical band sweeps left → right (0–100 % of duration), followed by a fade
 * 3. After [durationMs] the [onComplete] callback fires → navigate to Memento
 *
 * Placing this overlay on top of any content produces the "same wave, same moment"
 * effect described in the spec, provided both devices receive their WebSocket push
 * within a few hundred milliseconds of each other.
 */
@Composable
fun ConfirmationWaveOverlay(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    durationMs: Int = 1_350,
) {
    val animatable = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animatable.animateTo(
            targetValue   = 1f,
            animationSpec = tween(durationMs, easing = FastOutSlowInEasing),
        )
        onComplete()
    }

    val progress by animatable.asState()

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        // ── 1. Dimming background  ────────────────────────────────────────────
        drawRect(color = EmberBg.copy(alpha = (progress * 0.75f).coerceIn(0f, 0.75f)))

        // ── 2. Radial glow from centre (first 60 % of animation) ─────────────
        val glowFraction = (progress / 0.6f).coerceIn(0f, 1f)
        val glowRadius   = glowFraction * (size.minDimension * 0.65f)
        val glowAlpha    = (1f - glowFraction) * 0.55f
        if (glowRadius > 0f) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors  = listOf(EmberAccent.copy(alpha = glowAlpha), EmberBg.copy(alpha = 0f)),
                    center  = Offset(cx, cy),
                    radius  = glowRadius,
                ),
                radius = glowRadius,
                center = Offset(cx, cy),
            )
        }

        // ── 3. Vertical light band sweeping left → right ──────────────────────
        // The band leads at `progress * (w + bandHalf)` so it starts off-screen left
        // and ends off-screen right.
        val bandHalf  = w * 0.18f
        val leadX     = -bandHalf + progress * (w + bandHalf * 2f)
        val bandAlpha = ((1f - progress) * 1.6f).coerceIn(0f, 1f)  // fades near end

        if (bandAlpha > 0f) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors  = listOf(
                        EmberFg.copy(alpha = 0f),
                        EmberFg.copy(alpha = bandAlpha * 0.75f),
                        EmberFg.copy(alpha = bandAlpha),
                        EmberFg.copy(alpha = bandAlpha * 0.75f),
                        EmberFg.copy(alpha = 0f),
                    ),
                    startX = leadX - bandHalf,
                    endX   = leadX + bandHalf,
                ),
            )
        }
    }
}
