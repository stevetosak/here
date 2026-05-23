package net.tosak.here.screens.onboarding.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tosak.here.screens.onboarding.data.OnboardStep
import net.tosak.here.shared.components.Mono
import net.tosak.here.ui.theme.EmberAccent
import net.tosak.here.ui.theme.EmberFg
import net.tosak.here.ui.theme.EmberMuted
import net.tosak.here.ui.theme.JetBrainsMono

@Composable
fun InfoStepBody(info: OnboardStep, currentStep: Int){
    Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
        Mono(info.eyebrow, size = 10.sp, color = EmberMuted, letterSpacing = 0.32.sp)
        Text(
            text  = info.title,
            style = TextStyle(
                fontFamily = JetBrainsMono,
                fontSize   = 30.sp,
                lineHeight = 36.sp,
                color      = EmberFg,
            ),
        )
        Text(
            text  = info.body,
            style = TextStyle(
                fontFamily = JetBrainsMono,
                fontSize   = 14.sp,
                lineHeight = 22.sp,
                color      = EmberMuted,
            ),
        )
        OnboardGlyph(step = currentStep)
    }
}


@Composable
private fun OnboardGlyph(step: Int) {
    androidx.compose.foundation.Canvas(
        modifier = Modifier.size(72.dp),
    ) {
        val cx = size.width / 2
        val cy = size.height / 2
        when (step) {
            0 -> {
                // Pulsing dot — presence
                drawCircle(color = EmberFg.copy(alpha = 0.4f), radius = size.width * 0.42f, center = Offset(cx, cy), style = Stroke(width = 0.8f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 4f))))
                drawCircle(color = EmberAccent.copy(alpha = 0.7f), radius = size.width * 0.15f, center = Offset(cx, cy), style = Stroke(width = 1f))
                drawCircle(color = EmberAccent, radius = size.width * 0.066f, center = Offset(cx, cy))
            }
            1 -> {
                // Box + circle — proximity gate
                drawRect(color = EmberFg.copy(alpha = 0.3f), topLeft = Offset(size.width * 0.07f, size.height * 0.07f), size = Size(size.width * 0.86f, size.height * 0.86f), style = Stroke(0.8f))
                drawCircle(color = EmberFg.copy(alpha = 0.5f), radius = size.width * 0.31f, center = Offset(cx, cy), style = Stroke(width = 0.7f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f))))
                drawCircle(color = EmberAccent, radius = 3f, center = Offset(cx, cy))
            }
            2 -> {
                // Toggle off — invisible by default
                drawRect(color = EmberFg.copy(alpha = 0.6f), topLeft = Offset(size.width * 0.22f, size.height * 0.38f), size = Size(size.width * 0.56f, size.height * 0.24f), style = Stroke(0.8f))
                drawRect(color = EmberFg, topLeft = Offset(size.width * 0.24f, size.height * 0.40f), size = Size(size.width * 0.24f, size.height * 0.20f))
            }
            3 -> {
                // Lines crossed out — nothing persists
                drawRect(color = EmberFg.copy(alpha = 0.5f), topLeft = Offset(size.width * 0.1f, size.height * 0.2f), size = Size(size.width * 0.8f, size.height * 0.55f), style = Stroke(0.6f))
                for (y in listOf(0.35f, 0.48f, 0.58f)) {
                    drawLine(EmberFg.copy(alpha = 0.3f), Offset(size.width * 0.18f, size.height * y), Offset(size.width * 0.82f, size.height * y), 0.5f)
                }
                drawLine(EmberAccent, Offset(size.width * 0.04f, size.height * 0.14f), Offset(size.width * 0.96f, size.height * 0.86f), 1.4f)
            }
        }
    }
}