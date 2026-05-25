package net.tosak.here.screens.composer.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import net.tosak.here.screens.composer.camera.toRotatedImageBitmap
import net.tosak.here.shared.components.Mono
import net.tosak.here.ui.theme.EmberAccent
import net.tosak.here.ui.theme.EmberBg
import net.tosak.here.ui.theme.EmberFg
import net.tosak.here.ui.theme.EmberMuted
import net.tosak.here.ui.theme.JetBrainsMono
import java.io.File

/**
 * Switches between the live camera preview ([InFrameCamera]) and the captured photo.
 *
 * The [modifier] passed in must constrain the size to match the desired frame
 * (e.g. `fillMaxWidth().aspectRatio(4f/5f)`), so that overlaid children — the ✕ dismiss
 * button and the caption bar — are anchored to the actual image edges, not an
 * arbitrarily tall parent slot.
 *
 * [caption] / [onCaptionChange] are owned by the caller and overlaid at the bottom of
 * the captured image only (not shown during the live preview).
 */
@Composable
fun CameraPreview(
    capturedPath: String?,
    captureRequested: Flow<Unit>,
    onImageCaptured: (String) -> Unit,
    onCaptureFailed: () -> Unit,
    onRetake: () -> Unit,
    caption: String,
    onCaptionChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Load and rotate the bitmap on the IO thread whenever capturedPath changes.
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(capturedPath) {
        imageBitmap = withContext(Dispatchers.IO) {
            capturedPath?.let { File(it).toRotatedImageBitmap() }
        }
    }

    // The Box is sized by the caller's modifier (e.g. fillMaxWidth + aspectRatio),
    // so no contentAlignment centering is needed — children fill it directly.
    Box(modifier = modifier) {
        when {
            // ── Live camera preview ───────────────────────────────────────────
            capturedPath == null -> InFrameCamera(
                captureRequested = captureRequested,
                onImageCaptured = onImageCaptured,
                onCaptureFailed = onCaptureFailed,
            )

            // ── Captured photo ────────────────────────────────────────────────
            imageBitmap != null -> {
                Image(
                    bitmap = imageBitmap!!,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )

                // Caption bar — overlaid at the bottom of the image
                BasicTextField(
                    value = caption,
                    onValueChange = onCaptionChange,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(EmberBg.copy(alpha = 0.75f))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    textStyle = TextStyle(
                        fontFamily = JetBrainsMono,
                        fontSize = 13.sp,
                        color = EmberFg,
                    ),
                    cursorBrush = SolidColor(EmberAccent),
                    decorationBox = { inner ->
                        if (caption.isEmpty()) Mono(
                            "caption (optional)…",
                            size = 13.sp,
                            color = EmberMuted,
                        )
                        inner()
                    },
                )

                // ✕ dismiss — overlaid at the top-left corner of the image
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .background(EmberBg.copy(alpha = 0.82f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = onRetake,
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Mono("X", size = 12.sp, color = EmberFg, letterSpacing = 0.22.sp)
                }
            }
        }
    }
}
