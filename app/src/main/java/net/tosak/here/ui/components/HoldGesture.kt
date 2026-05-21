package net.tosak.here.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.tosak.here.ui.theme.*
import kotlin.math.PI

/**
 * 1-second press-and-hold circle that animates a progress arc and triggers [onComplete].
 * Releasing early resets. Matches the design's HoldGesture component exactly.
 */
@Composable
fun HoldGesture(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isHeld     by remember { mutableStateOf(false) }
    var progress   by remember { mutableFloatStateOf(0f) }
    val scope      = rememberCoroutineScope()
    val haptic     = LocalHapticFeedback.current
    val textMeasurer = rememberTextMeasurer()

    // Smooth progress animation (eases when released → 0)
    val animatedProgress by animateFloatAsState(
        targetValue  = progress,
        animationSpec = if (isHeld) snap() else tween(200, easing = FastOutSlowInEasing),
        label        = "holdProgress",
    )

    Column(
        modifier            = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Canvas(
            modifier = Modifier
                .size(200.dp)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            // Wait for finger/mouse down
                            val down = awaitFirstDown(requireUnconsumed = false)
                            isHeld = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                            val startMs = System.currentTimeMillis()
                            val durationMs = 1000L

                            val job = scope.launch {
                                while (isHeld) {
                                    val elapsed = System.currentTimeMillis() - startMs
                                    progress = (elapsed / durationMs.toFloat()).coerceIn(0f, 1f)
                                    if (progress >= 1f) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onComplete()
                                        isHeld = false
                                        break
                                    }
                                    delay(16)
                                }
                            }

                            // Wait for release
                            do {
                                val event = awaitPointerEvent()
                            } while (event.changes.any { it.pressed })

                            isHeld = false
                            job.cancel()
                            if (progress < 1f) progress = 0f
                        }
                    }
                },
        ) {
            val cx = size.width / 2
            val cy = size.height / 2
            val R  = size.width * 0.4f       // outer ring radius
            val innerR = size.width * 0.29f  // inner filled circle

            // ── Dashed background ring ────────────────────────────────────
            drawCircle(
                color  = EmberFg.copy(alpha = 0.18f),
                radius = R,
                center = Offset(cx, cy),
                style  = Stroke(
                    width      = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 3f)),
                ),
            )

            // ── Progress arc (ember accent) ───────────────────────────────
            if (animatedProgress > 0f) {
                drawArc(
                    color      = EmberAccent,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter  = false,
                    topLeft    = Offset(cx - R, cy - R),
                    size       = Size(R * 2, R * 2),
                    style      = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Butt),
                )
            }

            // ── Inner circle fill ─────────────────────────────────────────
            drawCircle(
                color  = if (isHeld) EmberFg else Color.Transparent,
                radius = innerR,
                center = Offset(cx, cy),
            )
            drawCircle(
                color  = EmberFg,
                radius = innerR,
                center = Offset(cx, cy),
                style  = Stroke(1.dp.toPx()),
            )

            // ── "HOLD" label ──────────────────────────────────────────────
            val textColor = if (isHeld) EmberBg else EmberFg
            val holdLabel = textMeasurer.measure(
                AnnotatedString("HOLD"),
                style = TextStyle(
                    fontFamily    = JetBrainsMono,
                    fontSize      = 11.sp,
                    color         = textColor,
                    letterSpacing = 0.18.sp,
                ),
            )
            drawText(
                holdLabel,
                topLeft = Offset(cx - holdLabel.size.width / 2f, cy - holdLabel.size.height - 2f),
            )



            // ── Countdown "Xs" ────────────────────────────────────────────
            val secondsLeft = (1 - (animatedProgress)).coerceAtLeast(0.0f)
            val countLabel = textMeasurer.measure(
                AnnotatedString("%.2fs".format(secondsLeft)),
                style = TextStyle(
                    fontFamily    = JetBrainsMono,
                    fontSize      = 9.sp,
                    color         = textColor.copy(alpha = 0.7f),
                    letterSpacing = 0.18.sp,
                ),
            )
            drawText(
                countLabel,
                topLeft = Offset(cx - countLabel.size.width / 2f, cy + 4f),
            )
        }

        Mono(
            text          = "PRESS · HOLD",
            size          = 9.sp,
            color         = EmberMuted,
            letterSpacing = 0.20.sp,
        )
    }
}