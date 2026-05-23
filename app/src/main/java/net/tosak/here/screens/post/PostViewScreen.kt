package net.tosak.here.screens.post

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tosak.here.shared.model.Friend
import net.tosak.here.shared.model.PostKind
import net.tosak.here.shared.components.*
import net.tosak.here.ui.theme.*

@Composable
fun PostViewScreen(
    friend: Friend,
    onClose: () -> Unit,
    onChat: (String?) -> Unit,
) {
    var expMins by remember { mutableIntStateOf(83) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000)
            if (expMins > 0) expMins--
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EmberBg),
    ) {
        HudStrip(presenceOn = true, minimal = true)

        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 14.dp)
                .padding(top = 38.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.Top,
        ) {
            Column {
                Mono("POST FROM", size = 10.sp, color = EmberMuted, letterSpacing = 0.3.sp)
                Spacer(Modifier.height(2.dp))
                Text("@${friend.id}", style = TextStyle(fontFamily = JetBrainsMono, fontSize = 22.sp, color = EmberFg))
            }
            Column(horizontalAlignment = Alignment.End) {
                Mono("${friend.dist}M AWAY", size = 9.sp, color = EmberMuted)
                Spacer(Modifier.height(4.dp))
                Text(
                    "EXPIRES ${expMins / 60}H ${expMins % 60}M",
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.sp, color = EmberAccent),
                )
            }
        }

        Rule()

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (friend.post.kind) {
                PostKind.PHOTO -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 5f)
                            .border(1.dp, EmberFg.copy(alpha = 0.33f))
                            .drawBehind {
                                val step = 16.dp.toPx()
                                var x = -size.height
                                while (x < size.width + size.height) {
                                    drawLine(EmberFg.copy(alpha = 0.06f), Offset(x, 0f), Offset(x + size.height, size.height), 1f)
                                    x += step
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Mono("[PHOTO]", size = 9.sp, color = EmberMuted, letterSpacing = 0.3.sp)
                        Mono("41°59′27″N · 21°25′41″E", size = 8.sp, color = EmberMuted,
                            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp))
                    }
                }
                PostKind.TEXT -> {
                    Text(
                        "\"${friend.post.caption}\"",
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 22.sp, lineHeight = 30.sp, color = EmberFg),
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
                PostKind.VOICE -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, EmberFg.copy(alpha = 0.33f))
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Mono("VOICE · 0:18", size = 10.sp, color = EmberMuted)
                        Row(
                            modifier         = Modifier.fillMaxWidth().height(32.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            repeat(48) { i ->
                                val h = (20 + kotlin.math.abs(kotlin.math.sin(i * 0.6)) * 70).toFloat()
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .fillMaxHeight(h / 100f)
                                        .background(EmberFg.copy(alpha = 0.6f))
                                )
                            }
                        }
                        PxButton("▶  PLAY", onClick = {})
                    }
                }
            }

            if (friend.post.kind != PostKind.TEXT && friend.post.caption.isNotBlank()) {
                Text(friend.post.caption, style = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp, color = EmberFg, lineHeight = 20.sp))
            }
            friend.post.place?.let {
                Mono("↳ ${it.uppercase()}", size = 10.sp, color = EmberMuted, letterSpacing = 0.18.sp)
            }

            Rule(dashed = true)

            Mono("QUICK REPLY", size = 10.sp, color = EmberMuted, letterSpacing = 0.18.sp)
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ON MY WAY", "WHERE EXACTLY?", "GIVE ME 10", "CAN'T TONIGHT").forEach { q ->
                    Box(
                        modifier = Modifier
                            .border(1.dp, EmberFg.copy(alpha = 0.33f))
                            .clickable(
                                indication        = null,
                                interactionSource = remember { MutableInteractionSource() },
                            ) { onChat(q) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Mono(q, size = 10.sp, letterSpacing = 0.18.sp)
                    }
                }
            }
        }

        Rule()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PxButton("← MAP", onClick = onClose)
            PxButton("OPEN THREAD →", onClick = { onChat(null) }, modifier = Modifier.weight(1f), primary = true)
        }
    }
}
