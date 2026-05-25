package net.tosak.here.screens.handshake

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import net.tosak.here.screens.handshake.viewmodel.MementoData
import net.tosak.here.screens.handshake.viewmodel.MementoViewModel
import net.tosak.here.shared.components.Mono
import net.tosak.here.shared.components.PxButton
import net.tosak.here.shared.components.Rule
import net.tosak.here.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Final receipt screen shown after a confirmed proximity handshake.
 *
 * Design intent: minimal, no engagement prompts. This should feel like a
 * tangible record of something real — like a ticket stub kept in a wallet.
 *
 * Persistence: in production, [MementoData] is saved locally and synced
 * to the server so both users can access it from the friendship detail screen.
 * Nothing auto-shares to feeds or timelines — the memento is private.
 */
@Composable
fun MementoScreen(
    memento: MementoData,
    modifier: Modifier = Modifier,
    viewModel: MementoViewModel = hiltViewModel(),
) {
    val dateStr = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        .format(Date(memento.timestamp))
        .uppercase()
    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault())
        .format(Date(memento.timestamp))

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EmberBg)
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(64.dp))

        // ── Decorative mark ───────────────────────────────────────────────────
        CrossMark()

        Spacer(Modifier.height(40.dp))

        // ── Header label ──────────────────────────────────────────────────────
        Mono(
            text          = "Connected",
            size          = 10.sp,
            color         = EmberMuted,
            letterSpacing = 0.32.sp,
        )

        Spacer(Modifier.height(12.dp))
        Rule()
        Spacer(Modifier.height(24.dp))

        // ── Friend nickname ───────────────────────────────────────────────────
        Text(
            text  = memento.friendNickname,
            style = TextStyle(
                fontFamily = JetBrainsMono,
                fontSize   = 36.sp,
                color      = EmberFg,
                lineHeight = 42.sp,
            ),
        )

        Spacer(Modifier.height(28.dp))
        Rule(color = EmberBorder)
        Spacer(Modifier.height(20.dp))

        // ── Location ──────────────────────────────────────────────────────────
        Text(
            text  = memento.location,
            style = TextStyle(
                fontFamily = JetBrainsMono,
                fontSize   = 14.sp,
                color      = EmberFg,
                lineHeight = 20.sp,
            ),
        )

        Spacer(Modifier.height(8.dp))

        // ── Date + time ───────────────────────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Mono(
                text          = dateStr,
                size          = 10.sp,
                color         = EmberMuted,
                letterSpacing = 0.20.sp,
            )
            Mono(
                text          = "·",
                size          = 10.sp,
                color         = EmberMuted,
            )
            Mono(
                text          = timeStr,
                size          = 10.sp,
                color         = EmberMuted,
                letterSpacing = 0.20.sp,
            )
        }

        Spacer(Modifier.height(40.dp))
        Rule(color = EmberBorder)
        Spacer(Modifier.height(20.dp))

        // ── Private note ──────────────────────────────────────────────────────
        Mono(
            text          = "visible only to you two.",
            size          = 9.sp,
            color         = EmberMuted.copy(alpha = 0.6f),
            letterSpacing = 0.18.sp,
        )

        // ── Spacer pushes Continue to bottom ──────────────────────────────────
        Spacer(Modifier.weight(1f))

        PxButton(
            text     = "Continue",
            onClick  = viewModel::onContinue,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(28.dp))
    }
}

/** A minimal crosshair/plus mark used as the decorative header element. */
@Composable
private fun CrossMark() {
    Box(
        modifier         = Modifier.size(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val half = size.minDimension * 0.45f
            val stroke = 1.dp.toPx()

            drawLine(
                color       = EmberFg.copy(alpha = 0.60f),
                start       = Offset(cx, cy - half),
                end         = Offset(cx, cy + half),
                strokeWidth = stroke,
            )
            drawLine(
                color       = EmberFg.copy(alpha = 0.60f),
                start       = Offset(cx - half, cy),
                end         = Offset(cx + half, cy),
                strokeWidth = stroke,
            )
            // Small centre dot
            drawCircle(
                color  = EmberAccent,
                radius = 2.dp.toPx(),
                center = Offset(cx, cy),
            )
        }
    }
}
