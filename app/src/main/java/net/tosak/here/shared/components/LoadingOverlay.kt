package net.tosak.here.shared.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tosak.here.ui.theme.*

@Composable
fun LoadingOverlay(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn(animationSpec = tween(180)),
        exit    = fadeOut(animationSpec = tween(180)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(EmberBg.copy(alpha = 0.90f))
                .pointerInput(Unit) {
                    // Consume all touch events — nothing below is reachable.
                    awaitPointerEventScope { while (true) awaitPointerEvent() }
                },
            contentAlignment = Alignment.Center,
        ) {
            val transition = rememberInfiniteTransition(label = "loading")

            val rotation by transition.animateFloat(
                initialValue = 0f,
                targetValue  = 360f,
                animationSpec = infiniteRepeatable(
                    animation  = tween(1_100, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "rotation",
            )

            val labelAlpha by transition.animateFloat(
                initialValue = 1f,
                targetValue  = 0f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 1_200
                        1f at 0
                        1f at 600
                        0f at 601
                        0f at 1_200
                    },
                    repeatMode = RepeatMode.Restart,
                ),
                label = "labelAlpha",
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Canvas(modifier = Modifier.size(56.dp)) {
                    // Background ring
                    drawArc(
                        color      = EmberFg.copy(alpha = 0.12f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter  = false,
                        style      = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Square),
                    )
                    // Rotating accent arc
                    drawArc(
                        color      = EmberAccent,
                        startAngle = rotation,
                        sweepAngle = 80f,
                        useCenter  = false,
                        style      = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Square),
                    )
                }

                Mono(
                    text          = "LOADING",
                    size          = 10.sp,
                    color         = EmberFg.copy(alpha = labelAlpha),
                    letterSpacing = 0.30.sp,
                )
            }
        }
    }
}