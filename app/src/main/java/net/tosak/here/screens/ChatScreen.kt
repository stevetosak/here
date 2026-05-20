package net.tosak.here.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tosak.here.model.Friend
import net.tosak.here.ui.components.*
import net.tosak.here.ui.theme.*

private data class ChatMessage(val who: String, val text: String, val time: String, val meta: String? = null)

@Composable
fun ChatScreen(
    friend: Friend,
    seedReply: String?,
    onClose: () -> Unit,
) {
    val initMessages = remember(friend, seedReply) {
        mutableStateListOf<ChatMessage>().apply {
            add(ChatMessage("them", friend.post.caption, "21:43", meta = "post"))
            if (seedReply != null) add(ChatMessage("you", seedReply, "21:46"))
        }
    }
    var input        by remember { mutableStateOf("") }
    val listState    = rememberLazyListState()

    LaunchedEffect(initMessages.size) {
        if (initMessages.isNotEmpty()) listState.animateScrollToItem(initMessages.lastIndex)
    }

    fun send(txt: String) {
        val t = txt.trim()
        if (t.isEmpty()) return
        initMessages.add(ChatMessage("you", t, "21:47"))
        input = ""
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
                .padding(horizontal = 22.dp)
                .padding(top = 52.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.Top,
        ) {
            Column {
                Mono("THREAD WITH", size = 10.sp, color = EmberMuted, letterSpacing = 0.3.sp)
                Spacer(Modifier.height(2.dp))
                Text("@${friend.id}", style = TextStyle(fontFamily = JetBrainsMono, fontSize = 22.sp, color = EmberFg))
            }
            Column(horizontalAlignment = Alignment.End) {
                Mono("${friend.dist}M", size = 9.sp, color = EmberMuted)
                Spacer(Modifier.height(4.dp))
                Mono("EXPIRES WITH POST", size = 9.sp, color = EmberMuted)
            }
        }

        Rule()

        LazyColumn(
            state           = listState,
            modifier        = Modifier
                .weight(1f)
                .padding(horizontal = 22.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Mono(
                    "─── THIS THREAD IS ONLY VISIBLE WHILE YOU ARE BOTH IN RADIUS ───",
                    size = 9.sp, color = EmberMuted, letterSpacing = 0.20.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            items(initMessages) { m ->
                _root_ide_package_.net.tosak.here.screens.ChatBubble(m)
            }
            item {
                Mono(
                    "NO READ RECEIPTS · NO TYPING · NO HISTORY",
                    size = 9.sp, color = EmberMuted, letterSpacing = 0.20.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Quick-reply chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("ON MY WAY", "WHERE?", "GIVE ME 10").forEach { q ->
                Box(
                    modifier = Modifier
                        .border(1.dp, EmberFg.copy(alpha = 0.33f))
                        .clickable(
                            indication        = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) { send(q.lowercase()) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    Mono(q, size = 9.sp, letterSpacing = 0.16.sp)
                }
            }
        }

        // Input row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = EmberBorder, shape = androidx.compose.ui.graphics.RectangleShape)
                .padding(horizontal = 22.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BasicTextField(
                value         = input,
                onValueChange = { input = it },
                modifier      = Modifier.weight(1f),
                textStyle     = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp, color = EmberFg),
                cursorBrush   = SolidColor(EmberAccent),
                singleLine    = true,
                decorationBox = { inner ->
                    if (input.isEmpty()) Text(
                        "say something short…",
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp, color = EmberMuted),
                    )
                    inner()
                },
            )
            Box(
                modifier = Modifier
                    .background(EmberFg)
                    .clickable(
                        indication        = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { send(input) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text("SEND ↑", style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp, color = EmberBg, letterSpacing = 0.2.sp))
            }
        }

        // Bottom back
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 14.dp),
        ) {
            PxButton("← MAP", onClick = onClose)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ChatBubble(m: ChatMessage) {
    val mine = m.who == "you"
    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
    ) {
        if (m.meta == "post") {
            Mono("↳ THEIR POST", size = 8.sp, color = EmberMuted, letterSpacing = 0.2.sp)
            Spacer(Modifier.height(4.dp))
        }
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(if (mine) EmberFg else androidx.compose.ui.graphics.Color.Transparent)
                .then(if (!mine) Modifier.border(1.dp, EmberFg.copy(alpha = 0.33f)) else Modifier)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                m.text,
                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp, lineHeight = 20.sp, color = if (mine) EmberBg else EmberFg),
            )
        }
        Spacer(Modifier.height(4.dp))
        Mono(m.time, size = 8.sp, color = EmberMuted)
    }
}