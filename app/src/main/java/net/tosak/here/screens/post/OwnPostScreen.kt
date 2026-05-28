package net.tosak.here.screens.post

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.tosak.here.shared.components.*
import net.tosak.here.shared.model.PostKind
import net.tosak.here.shared.storage.PostEntity
import net.tosak.here.screens.post.viewmodel.OwnPostViewModel
import net.tosak.here.ui.theme.*

@Composable
fun OwnPostScreen(
    viewModel: OwnPostViewModel = hiltViewModel(),
) {
    val post by viewModel.activePost.collectAsStateWithLifecycle()

    // Post expired or was deleted while the screen was open — go back automatically.
    if (post == null) {
        LaunchedEffect(Unit) { viewModel.onClose() }
        return
    }

    OwnPostContent(
        post     = post!!,
        onClose  = viewModel::onClose,
        onDelete = viewModel::deletePost,
    )
}

// ── Content ───────────────────────────────────────────────────────────────────

@Composable
private fun OwnPostContent(
    post: PostEntity,
    onClose: () -> Unit,
    onDelete: () -> Unit,
) {
    // Decode the photo off the main thread; null for text posts or until load completes.
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(post.imagePath) {
        imageBitmap = withContext(Dispatchers.IO) {
            post.imagePath?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EmberBg),
    ) {
        HudStrip(presenceOn = true, minimal = true)

        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 14.dp)
                .padding(top = 38.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.Top,
        ) {
            Column {
                Mono("YOUR POST", size = 10.sp, color = EmberMuted, letterSpacing = 0.3.sp)
                Spacer(Modifier.height(2.dp))
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Mono(kindGlyph(post.kind), size = 18.sp, color = EmberAccent, letterSpacing = 0.sp)
                    Text(
                        kindLabel(post.kind),
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 22.sp, color = EmberFg),
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Mono("EXPIRES IN", size = 9.sp, color = EmberMuted)
                Spacer(Modifier.height(4.dp))
                Text(
                    timeLeft(post.expiresAt),
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 14.sp, color = EmberAccent),
                )
            }
        }

        Rule()

        // ── Body ──────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (PostKind.valueOf(post.kind)) {
                PostKind.PHOTO -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f)
                            .border(1.dp, EmberFg.copy(alpha = 0.33f))
                            .then(
                                if (imageBitmap == null) Modifier.drawBehind {
                                    val step = 16.dp.toPx()
                                    var x = -size.height
                                    while (x < size.width + size.height) {
                                        drawLine(EmberFg.copy(alpha = 0.06f), Offset(x, 0f), Offset(x + size.height, size.height), 1f)
                                        x += step
                                    }
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (imageBitmap != null) {
                            Image(
                                bitmap             = imageBitmap!!,
                                contentDescription = null,
                                modifier           = Modifier.fillMaxSize(),
                                contentScale       = ContentScale.Crop,
                            )
                        } else {
                            Mono("[PHOTO]", size = 9.sp, color = EmberMuted, letterSpacing = 0.3.sp)
                        }
                    }
                    if (post.caption.isNotBlank()) {
                        Text(
                            post.caption,
                            style = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp, color = EmberFg, lineHeight = 20.sp),
                        )
                    }
                }
                PostKind.TEXT -> {
                    Text(
                        "\"${post.caption}\"",
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 22.sp, lineHeight = 30.sp, color = EmberFg),
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
            }

            Rule(dashed = true)

            // Location stamp
            Mono(
                "POSTED AT · ${coordLabel(post.lat, post.lng)}",
                size          = 9.sp,
                color         = EmberMuted,
                letterSpacing = 0.14.sp,
            )
        }

        // ── Footer ────────────────────────────────────────────────────────────
        Rule()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
        ) {
            PxButton(
                text     = "DELETE POST",
                onClick  = onDelete,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun kindGlyph(kind: String): String = when (kind) {
    "PHOTO" -> "⊡"
    "TEXT"  -> "≡"
    else    -> "·"
}

private fun kindLabel(kind: String): String = when (kind) {
    "PHOTO" -> "photo"
    "TEXT"  -> "text"
    else    -> kind.lowercase()
}

private fun timeLeft(expiresAt: Long): String {
    val ms = (expiresAt - System.currentTimeMillis()).coerceAtLeast(0L)
    val h  = ms / 3_600_000L
    val m  = (ms % 3_600_000L) / 60_000L
    return if (h > 0L) "${h}H ${m}M" else "${m}M"
}

private fun coordLabel(lat: Double, lng: Double): String {
    val latAbs = Math.abs(lat); val lngAbs = Math.abs(lng)
    val latD   = latAbs.toInt(); val latM = ((latAbs - latD) * 60).toInt()
    val lngD   = lngAbs.toInt(); val lngM = ((lngAbs - lngD) * 60).toInt()
    return "${latD}°${latM}′${if (lat >= 0) "N" else "S"} · ${lngD}°${lngM}′${if (lng >= 0) "E" else "W"}"
}