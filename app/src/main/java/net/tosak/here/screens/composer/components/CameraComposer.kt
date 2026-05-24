package net.tosak.here.screens.composer.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import net.tosak.here.screens.composer.camera.toRotatedImageBitmap
import net.tosak.here.shared.components.Mono
import net.tosak.here.ui.theme.EmberBg
import net.tosak.here.ui.theme.EmberFg
import java.io.File

/**
 * Switches between the live camera preview ([InFrameCamera]) and the captured photo.
 *
 * State is owned by the parent (ViewModel) — this composable is purely presentational:
 * - [capturedPath] null → show camera preview
 * - [capturedPath] non-null → show the captured image with a "RETAKE" badge
 *
 * Capture is triggered externally via [captureRequested]; results bubble up through
 * [onImageCaptured] / [onCaptureFailed]. "RETAKE" calls [onRetake].
 */
@Composable
fun CameraComposer(
    capturedPath: String?,
    captureRequested: Flow<Unit>,
    onImageCaptured: (String) -> Unit,
    onCaptureFailed: () -> Unit,
    onRetake: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Load and rotate the bitmap on the IO thread whenever capturedPath changes.
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(capturedPath) {
        imageBitmap = withContext(Dispatchers.IO) {
            capturedPath?.let { File(it).toRotatedImageBitmap() }
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        when {
            // ── Live camera preview ───────────────────────────────────────────
            capturedPath == null -> InFrameCamera(
                modifier = Modifier.fillMaxSize(),
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
                // RETAKE badge — top-right corner
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(EmberBg.copy(alpha = 0.82f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = onRetake,
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Mono("RETAKE", size = 8.sp, color = EmberFg, letterSpacing = 0.22.sp)
                }
            }
        }
    }
}