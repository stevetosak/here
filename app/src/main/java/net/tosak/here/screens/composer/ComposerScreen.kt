package net.tosak.here.screens.composer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tosak.here.shared.model.PostKind
import net.tosak.here.shared.components.*
import net.tosak.here.ui.theme.*
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun ComposerScreen(onClose: () -> Unit, onSubmit: (PostKind, String) -> Unit) {
    var kind      by remember { mutableStateOf<PostKind?>(null) }
    var text      by remember { mutableStateOf("") }
    var voiceSecs by remember { mutableIntStateOf(0) }
    var recording by remember { mutableStateOf(false) }

    LaunchedEffect(recording) {
        if (!recording) return@LaunchedEffect
        while (recording) {
            kotlinx.coroutines.delay(1000)
            voiceSecs++
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EmberBg)
            .padding(horizontal = 22.dp),
    ) {
        HudStrip(presenceOn = true, minimal = true)

        Row(
            modifier              = Modifier.fillMaxWidth().padding(top = 38.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.Bottom,
        ) {
            Mono("POST A MOMENT", size = 10.sp, color = EmberMuted, letterSpacing = 0.3.sp)
            Mono("EXPIRES 2H · 400M", size = 9.sp, color = EmberMuted)
        }

        Spacer(Modifier.height(20.dp))

        // Kind picker
        if (kind == null) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                KindCard("PHOTO", "A frame from where you are.", "◯") { kind = PostKind.PHOTO }
                Spacer(Modifier.height(12.dp))
                KindCard("TEXT", "140 characters. A line, a thought, a question.", "—") { kind = PostKind.TEXT }
                Spacer(Modifier.height(12.dp))
                KindCard("VOICE", "30 seconds. A sound from the room.", "◖") { kind = PostKind.VOICE }
            }
        }

        // Photo composer
        if (kind == PostKind.PHOTO) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(1.dp, EmberFg)
                    .drawBehind {
                        // Hatched placeholder
                        val step = 12.dp.toPx()
                        var x = -size.height
                        while (x < size.width + size.height) {
                            drawLine(EmberFg.copy(alpha = 0.07f), Offset(x, 0f), Offset(x + size.height, size.height), 1f)
                            x += step
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Mono("[FRAME]", size = 9.sp, color = EmberMuted, letterSpacing = 0.3.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("tap to capture", style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp, color = EmberMuted))
                }
                Mono("41°59′27″N · 21°25′41″E", size = 8.sp, color = EmberMuted, modifier = Modifier.align(Alignment.TopStart).padding(8.dp))
                Mono("● REC READY", size = 8.sp, color = EmberMuted, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
            }
            Spacer(Modifier.height(10.dp))
            BasicTextField(
                value         = text,
                onValueChange = { text = it.take(80) },
                modifier      = Modifier.fillMaxWidth().border(1.dp, EmberBorder).padding(12.dp),
                textStyle     = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp, color = EmberFg),
                cursorBrush   = SolidColor(EmberAccent),
                decorationBox = { inner ->
                    if (text.isEmpty()) Mono("caption (optional)…", size = 13.sp, color = EmberMuted)
                    inner()
                },
            )
        }

        // Text composer
        if (kind == PostKind.TEXT) {
            BasicTextField(
                value         = text,
                onValueChange = { text = it.take(140) },
                modifier      = Modifier.weight(1f).fillMaxWidth().border(1.dp, EmberBorder).padding(14.dp),
                textStyle     = TextStyle(fontFamily = JetBrainsMono, fontSize = 15.sp, lineHeight = 22.sp, color = EmberFg),
                cursorBrush   = SolidColor(EmberAccent),
                maxLines      = Int.MAX_VALUE,
                decorationBox = { inner ->
                    if (text.isEmpty()) Text(
                        "say something only the people here can read…",
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 15.sp, color = EmberMuted),
                    )
                    inner()
                },
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Mono("${text.length} / 140", size = 9.sp, color = EmberMuted)
                Mono("~ ${text.split(Regex("\\s+")).filter { it.isNotBlank() }.size.coerceAtLeast(1)} WORDS", size = 9.sp, color = EmberMuted)
            }
        }

        // Voice composer
        if (kind == PostKind.VOICE) {
            Column(
                modifier            = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text  = "%02d:%02d".format(voiceSecs / 60, voiceSecs % 60),
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 36.sp, color = EmberFg, letterSpacing = 0.05.sp),
                )
                Spacer(Modifier.height(18.dp))
                // Waveform bars
                Row(
                    modifier         = Modifier.height(36.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    repeat(32) { i ->
                        val barH = if (recording) (10 + abs(sin(i + voiceSecs.toDouble()).toFloat()) * 26).dp else 4.dp
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(barH)
                                .background(if (recording) EmberAccent else EmberMuted),
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .background(if (recording) EmberAccent else Color.Transparent, shape = androidx.compose.foundation.shape.CircleShape)
                        .border(2.dp, EmberFg, shape = androidx.compose.foundation.shape.CircleShape)
                        .clickable(
                            indication        = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) { recording = !recording },
                )
                Spacer(Modifier.height(14.dp))
                Mono(
                    text = when {
                        recording     -> "● RECORDING"
                        voiceSecs > 0 -> "TAP TO RESUME · ↓ TO SEND"
                        else          -> "TAP TO RECORD · 30s MAX"
                    },
                    size = 9.sp, color = EmberMuted, letterSpacing = 0.22.sp,
                )
            }
        }

        // Footer buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PxButton("← BACK", onClick = { if (kind != null) kind = null else onClose() })
            if (kind != null) {
                PxButton(
                    "POST TO RADIUS →",
                    onClick  = { onSubmit(kind!!, text) },
                    modifier = Modifier.weight(1f),
                    primary  = true,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun KindCard(label: String, hint: String, icon: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, EmberFg)
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick           = onClick,
            )
            .padding(horizontal = 22.dp, vertical = 20.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(icon, style = TextStyle(fontFamily = JetBrainsMono, fontSize = 28.sp, color = EmberFg))
        Column(modifier = Modifier.weight(1f)) {
            Mono(label, size = 13.sp, letterSpacing = 0.22.sp)
            Spacer(Modifier.height(4.dp))
            Text(hint, style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.sp, color = EmberMuted))
        }
        Text("→", style = TextStyle(fontFamily = JetBrainsMono, fontSize = 16.sp, color = EmberMuted))
    }
}
