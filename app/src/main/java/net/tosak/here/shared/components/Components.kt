package net.tosak.here.shared.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tosak.here.ui.theme.*

// ─── Mono label ──────────────────────────────────────────────────────────────
@Composable
fun Mono(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = 10.sp,
    color: Color = EmberFg,
    letterSpacing: TextUnit = 0.18.sp,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text          = text.uppercase(),
        modifier      = modifier,
        style         = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
            fontFamily    = JetBrainsMono,
            fontSize      = size,
            letterSpacing = letterSpacing,
            color         = color,
        ),
        maxLines      = maxLines,
        overflow      = TextOverflow.Ellipsis,
    )
}

// ─── Blinking presence status ─────────────────────────────────────────────────
@Composable
fun StatusChip(presenceOn: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "blink")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1400
                1f at 0
                1f at 700
                0f at 701
                0f at 1400
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "blinkAlpha",
    )
    val dotColor = if (presenceOn) EmberAccent else EmberFg
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text  = "[PRESENCE: ${if (presenceOn) "ON" else "OFF"}]",
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                fontFamily    = JetBrainsMono,
                fontSize      = 10.sp,
                color         = if (presenceOn) dotColor.copy(alpha = alpha) else EmberMuted,
                letterSpacing = 0.14.sp,
            ),
        )
    }
}

// ─── Ambient readout (rotates every ~4 s) ─────────────────────────────────────
private val AMBIENT_LINES = listOf(
    "21:47 · CLEAR · 18°C",
    "WIND 4KT NE · CLEAR",
    "MOON 73% · WAXING",
    "21:48 · LOW TRAFFIC",
    "LIGHTS ON · CAFÉS OPEN",
)

@Composable
fun AmbientReadout() {
    var idx by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(3800)
            idx = (idx + 1) % AMBIENT_LINES.size
        }
    }
    Mono(text = AMBIENT_LINES[idx], size = 8.sp, color = EmberMuted, letterSpacing = 0.14.sp)
}

// ─── Navigation composition local ────────────────────────────────────────────
val LocalBackAction = staticCompositionLocalOf<(() -> Unit)?> { null }

// ─── HUD strip (top of every screen) ─────────────────────────────────────────
@Composable
fun HudStrip(
    presenceOn: Boolean,
    modifier: Modifier = Modifier,
    minimal: Boolean = false,
    place: String = "Debar Maalo",
    coords: String = "41°59′N · 21°25′E",
) {
    val backAction = LocalBackAction.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.Top,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (backAction != null) {
                BackButton()
            } else {
                StatusChip(presenceOn)
                if (!minimal) {
                    Mono(
                        text          = place,
                        size          = 8.sp,
                        color         = EmberMuted,
                        letterSpacing = 0.18.sp,
                    )
                }
            }
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Mono(text = coords, size = 8.sp, color = EmberMuted, letterSpacing = 0.14.sp)
            if (!minimal) AmbientReadout()
        }
    }
}

// ─── Hairline divider ─────────────────────────────────────────────────────────
@Composable
fun Rule(
    modifier: Modifier = Modifier,
    color: Color = EmberBorder,
    dashed: Boolean = false,
) {
    if (!dashed) {
        HorizontalDivider(modifier = modifier, thickness = 1.dp, color = color)
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(1.dp)
                .drawBehind {
                    val dashLen = 6.dp.toPx()
                    val gapLen  = 4.dp.toPx()
                    var x = 0f
                    while (x < size.width) {
                        drawLine(
                            color       = color,
                            start       = Offset(x, 0f),
                            end         = Offset((x + dashLen).coerceAtMost(size.width), 0f),
                            strokeWidth = 1.dp.toPx(),
                        )
                        x += dashLen + gapLen
                    }
                },
        )
    }
}

// ─── Global back button (top-left of secondary screens) ──────────────────────
@Composable
fun BackButton(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    label: String = "← BACK",
) {
    val action = onClick ?: LocalBackAction.current ?: return
    Box(
        modifier = modifier
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick           = action,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Mono(label, size = 10.sp, color = EmberFg, letterSpacing = 0.22.sp)
    }
}

// ─── Tactical button ─────────────────────────────────────────────────────────
@Composable
fun PxButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
) {
    val bg     = if (primary) EmberFg else Color.Transparent
    val fg     = if (primary) EmberBg else EmberFg
    val shadow = if (primary) EmberAccent else Color.Transparent

    Box(
        modifier = modifier
            .border(1.dp, EmberFg)
            .drawBehind {
                if (primary) {
                    drawRect(
                        color    = shadow,
                        topLeft  = Offset(4.dp.toPx(), 4.dp.toPx()),
                        size     = size,
                    )
                }
            }
            .then(if (primary) Modifier.offset((-2).dp, (-2).dp) else Modifier)
            .drawBehind {
                drawRect(color = bg)
            }
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick           = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text  = text.uppercase(),
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                fontFamily    = JetBrainsMono,
                fontSize      = 12.sp,
                color         = fg,
                fontWeight    = FontWeight.Normal,
                letterSpacing = 0.22.sp,
            ),
        )
    }
}

// ─── Settings row (label + value, read-only) ──────────────────────────────────
@Composable
fun SettingRow(label: String, value: String, hint: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(label, style = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp, color = EmberFg))
            hint?.let { Spacer(Modifier.height(2.dp)); Mono(it, size = 9.sp, color = EmberMuted, letterSpacing = 0.10.sp) }
        }
        Mono(value, size = 11.sp, color = EmberAccent, letterSpacing = 0.18.sp)
    }
    Rule(dashed = true, color = EmberFg.copy(alpha = 0.13f))
}

// ─── Settings toggle (label + minimal switch) ─────────────────────────────────
@Composable
fun SettingToggle(
    label: String,
    hint: String?,
    value: Boolean,
    onChange: (Boolean) -> Unit,
    disabled: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(label, style = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp, color = if (disabled) EmberMuted else EmberFg))
            hint?.let { Spacer(Modifier.height(2.dp)); Mono(it, size = 9.sp, color = EmberMuted, letterSpacing = 0.10.sp) }
        }
        // Minimal toggle
        Box(
            modifier = Modifier
                .size(width = 50.dp, height = 22.dp)
                .background(if (value && !disabled) EmberAccent else Color.Transparent)
                .border(1.dp, if (disabled) EmberMuted else EmberFg)
                .then(if (!disabled) Modifier.clickable(
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { onChange(!value) } else Modifier),
        ) {
            Box(
                modifier = Modifier
                    .size(width = 19.dp, height = 18.dp)
                    .padding(1.dp)
                    .offset(x = if (value) 29.dp else 1.dp)
                    .background(if (value) EmberBg else if (disabled) EmberMuted else EmberFg),
            )
        }
    }
    Rule(dashed = true, color = EmberFg.copy(alpha = 0.13f))
}
