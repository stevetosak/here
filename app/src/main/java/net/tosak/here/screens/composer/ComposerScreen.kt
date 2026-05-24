package net.tosak.here.screens.composer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import net.tosak.here.screens.composer.components.CameraComposer
import net.tosak.here.screens.composer.viewmodel.ComposerViewModel
import net.tosak.here.shared.model.PostKind
import net.tosak.here.shared.components.*
import net.tosak.here.ui.theme.*

@Composable
fun ComposerScreen(
    onClose: () -> Unit,
    onSubmit: (PostKind, String) -> Unit,
    viewModel: ComposerViewModel = hiltViewModel(),
) {
    var kind by remember { mutableStateOf<PostKind?>(null) }
    var text by remember { mutableStateOf("") }

    val capturedPath by viewModel.capturedImagePath.collectAsState()
    val isCapturing  by viewModel.isCapturing.collectAsState()

    // Clear captured photo whenever the user navigates back to the kind picker.
    LaunchedEffect(kind) {
        if (kind != PostKind.PHOTO) viewModel.resetPhoto()
    }

    // ── Layout ────────────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EmberBg)
            .padding(horizontal = 22.dp),
    ) {
        HudStrip(presenceOn = true, minimal = true)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 3.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Mono("POST A MOMENT", size = 10.sp, color = EmberMuted, letterSpacing = 0.3.sp)
        }

        Spacer(Modifier.height(20.dp))

        // ── Kind picker ───────────────────────────────────────────────────────
        if (kind == null) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                KindCard("PHOTO", "A frame from where you are.", "⊡") { kind = PostKind.PHOTO }
                Spacer(Modifier.height(12.dp))
                KindCard("TEXT", "140 characters. A line, a thought, a question.", "≡") {
                    kind = PostKind.TEXT
                }
            }
        }

        // ── Photo composer ────────────────────────────────────────────────────
        if (kind == PostKind.PHOTO) {
            CameraComposer(
                capturedPath     = capturedPath,
                captureRequested = viewModel.captureRequested,
                onImageCaptured  = viewModel::onImageCaptured,
                onCaptureFailed  = viewModel::onCaptureFailed,
                onRetake         = viewModel::resetPhoto,
                modifier         = Modifier.weight(1f),
            )

            Spacer(Modifier.height(10.dp))
            BasicTextField(
                value = text,
                onValueChange = { text = it.take(80) },
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, EmberBorder)
                    .padding(12.dp),
                textStyle = TextStyle(
                    fontFamily = JetBrainsMono,
                    fontSize = 13.sp,
                    color = EmberFg
                ),
                cursorBrush = SolidColor(EmberAccent),
                decorationBox = { inner ->
                    if (text.isEmpty()) Mono(
                        "caption (optional)…",
                        size = 13.sp,
                        color = EmberMuted
                    )
                    inner()
                },
            )
        }

        // ── Text composer ─────────────────────────────────────────────────────
        if (kind == PostKind.TEXT) {
            BasicTextField(
                value = text,
                onValueChange = { text = it.take(140) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(1.dp, EmberBorder)
                    .padding(14.dp),
                textStyle = TextStyle(
                    fontFamily = JetBrainsMono,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = EmberFg
                ),
                cursorBrush = SolidColor(EmberAccent),
                maxLines = Int.MAX_VALUE,
                decorationBox = { inner ->
                    if (text.isEmpty()) Text(
                        "say something only the people here can read…",
                        style = TextStyle(
                            fontFamily = JetBrainsMono,
                            fontSize = 15.sp,
                            color = EmberMuted
                        ),
                    )
                    inner()
                },
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Mono("${text.length} / 140", size = 9.sp, color = EmberMuted)
                Mono(
                    "~ ${
                        text.split(Regex("\\s+")).filter { it.isNotBlank() }.size.coerceAtLeast(1)
                    } WORDS",
                    size = 9.sp,
                    color = EmberMuted,
                )
            }
        }

        // ── Footer — single adaptive button ───────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (kind) {
                PostKind.PHOTO -> {
                    if (capturedPath == null) {
                        // No photo yet — trigger capture
                        PxButton(
                            text = if (isCapturing) "  …  " else "◉ TAKE",
                            onClick = { viewModel.requestCapture() },
                            modifier = Modifier.weight(1f),
                            primary = true,
                        )
                    } else {
                        // Photo taken — ready to post
                        PxButton(
                            text = "POST TO RADIUS →",
                            onClick = {
                                viewModel.submit(PostKind.PHOTO, text) {
                                    onSubmit(PostKind.PHOTO, text)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            primary = true,
                        )
                    }
                }

                PostKind.TEXT -> {
                    PxButton(
                        text = "POST TO RADIUS →",
                        onClick = {
                            viewModel.submit(PostKind.TEXT, text) {
                                onSubmit(PostKind.TEXT, text)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        primary = text.isNotBlank(),
                    )
                }

                null -> { /* kind picker — no footer button */ }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ── Kind picker card ──────────────────────────────────────────────────────────

@Composable
private fun KindCard(label: String, hint: String, icon: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, EmberFg)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            )
            .padding(horizontal = 22.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(icon, style = TextStyle(fontFamily = JetBrainsMono, fontSize = 22.sp, color = EmberFg))
        Column(modifier = Modifier.weight(1f)) {
            Mono(label, size = 13.sp, letterSpacing = 0.22.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                hint,
                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.sp, color = EmberMuted)
            )
        }
        Text(
            "→",
            style = TextStyle(fontFamily = JetBrainsMono, fontSize = 16.sp, color = EmberMuted)
        )
    }
}