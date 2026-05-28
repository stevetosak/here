package net.tosak.here.screens.dm

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.tosak.here.screens.dm.viewmodel.DmChatViewModel
import net.tosak.here.shared.components.HudStrip
import net.tosak.here.shared.components.Mono
import net.tosak.here.shared.components.PxButton
import net.tosak.here.shared.components.Rule
import net.tosak.here.shared.storage.DmMessageEntity
import net.tosak.here.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DmChatScreen(
    viewModel: DmChatViewModel = hiltViewModel(),
) {
    val friend   by viewModel.activeFriend.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isTyping by viewModel.isTyping.collectAsStateWithLifecycle()

    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) viewModel.sendImage(uri)
    }

    LaunchedEffect(messages.size, isTyping) {
        val target = messages.size + if (isTyping) 1 else 0
        if (target > 0) listState.animateScrollToItem(target - 1)
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
            Column(
                modifier = Modifier.clickable(
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick           = viewModel::onOpenProfile,
                ),
            ) {
                Mono("DM WITH · TAP FOR PROFILE", size = 10.sp, color = EmberMuted, letterSpacing = 0.3.sp)
                Spacer(Modifier.height(2.dp))
                Text(
                    "@${friend?.id ?: "—"}",
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 22.sp, color = EmberFg),
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                friend?.let {
                    Mono("${it.dist}M", size = 9.sp, color = EmberMuted)
                    Spacer(Modifier.height(4.dp))
                }
                Mono("PERSISTENT · ${messages.size} MSGS", size = 9.sp, color = EmberMuted)
            }
        }

        Rule()

        LazyColumn(
            state    = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 22.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(messages, key = { it.id }) { m ->
                DmBubble(m)
            }
            if (isTyping) {
                item("typing") { TypingBubble() }
            }
        }

        // Input row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = EmberBorder, shape = androidx.compose.ui.graphics.RectangleShape)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .border(1.dp, EmberFg.copy(alpha = 0.33f))
                    .clickable(
                        indication        = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) {
                        pickImage.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Mono("+ IMG", size = 10.sp, color = EmberFg, letterSpacing = 0.2.sp)
            }

            BasicTextField(
                value         = input,
                onValueChange = { input = it },
                modifier      = Modifier.weight(1f),
                textStyle     = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp, color = EmberFg),
                cursorBrush   = SolidColor(EmberAccent),
                singleLine    = true,
                decorationBox = { inner ->
                    if (input.isEmpty()) Text(
                        "message…",
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp, color = EmberMuted),
                    )
                    inner()
                },
            )

            val canSend = input.isNotBlank()
            Box(
                modifier = Modifier
                    .background(if (canSend) EmberFg else EmberFg.copy(alpha = 0.25f))
                    .clickable(
                        indication        = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) {
                        if (canSend) {
                            viewModel.sendText(input)
                            input = ""
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    "SEND ↑",
                    style = TextStyle(
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = if (canSend) EmberBg else EmberBg.copy(alpha = 0.6f),
                        letterSpacing = 0.2.sp,
                    ),
                )
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DmBubble(m: DmMessageEntity) {
    val mine = m.fromMe
    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
    ) {
        if (m.imagePath != null) {
            ImageMessage(path = m.imagePath, mine = mine)
        } else {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .background(if (mine) EmberFg else Color.Transparent)
                    .then(if (!mine) Modifier.border(1.dp, EmberFg.copy(alpha = 0.33f)) else Modifier)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    m.text ?: "",
                    style = TextStyle(
                        fontFamily = JetBrainsMono,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = if (mine) EmberBg else EmberFg,
                    ),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Mono(formatTime(m.sentAt), size = 8.sp, color = EmberMuted)
    }
}

@Composable
private fun ImageMessage(path: String, mine: Boolean) {
    var bitmap by remember(path) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(path) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
        }
    }
    Box(
        modifier = Modifier
            .widthIn(max = 240.dp)
            .border(1.dp, if (mine) EmberFg else EmberFg.copy(alpha = 0.33f)),
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap        = bmp,
                contentDescription = null,
                contentScale  = ContentScale.Crop,
                modifier      = Modifier
                    .size(width = 240.dp, height = 240.dp),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(width = 240.dp, height = 240.dp)
                    .background(EmberSurface),
                contentAlignment = Alignment.Center,
            ) {
                Mono("LOADING…", size = 9.sp, color = EmberMuted)
            }
        }
    }
}

@Composable
private fun TypingBubble() {
    val transition = rememberInfiniteTransition(label = "typing")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue  = 0.85f,
        animationSpec = infiniteRepeatable(
            animation  = tween(700),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dots",
    )
    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
    ) {
        Box(
            modifier = Modifier
                .border(1.dp, EmberFg.copy(alpha = 0.33f))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                "· · ·",
                style = TextStyle(
                    fontFamily = JetBrainsMono,
                    fontSize = 16.sp,
                    color = EmberFg.copy(alpha = alpha),
                    letterSpacing = 4.sp,
                ),
            )
        }
    }
}

private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
private fun formatTime(ts: Long): String = timeFmt.format(Date(ts))
