package net.tosak.here.screens.composer.components

import android.R.attr.scaleX
import android.graphics.BitmapFactory
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
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
import net.tosak.here.screens.composer.viewmodel.PostPhotoViewModel
import net.tosak.here.shared.components.HudStrip
import net.tosak.here.shared.components.Mono
import net.tosak.here.shared.components.PxButton
import net.tosak.here.ui.theme.EmberAccent
import net.tosak.here.ui.theme.EmberBg
import net.tosak.here.ui.theme.EmberFg
import net.tosak.here.ui.theme.EmberMuted
import net.tosak.here.ui.theme.JetBrainsMono
import java.io.File

/**
 * Full-screen photo post composer.
 *
 * Layout (top → bottom):
 *   ┌──────────────────────────────┐
 *   │  HudStrip                    │  ← fixed height (natural size)
 *   ├──────────────────────────────┤
 *   │                              │
 *   │  Camera / captured photo     │  ← weight(2f) → ~2/3 of flexible space
 *   │                              │
 *   ├──────────────────────────────┤
 *   │  Action buttons              │  ← weight(1f) → ~1/3 of flexible space
 *   ├──────────────────────────────┤
 *   │  EXPIRES ON EXIT · coords    │  ← fixed height (natural size)
 *   └──────────────────────────────┘
 *
 * The two weighted sections divide the space that remains after the HudStrip and
 * metadata row have taken their natural heights. weight(2f) + weight(1f) = 3 total
 * parts, so the camera gets 2/3 and the actions get 1/3 of that flexible space.
 */
@Composable
fun PostPhotoScreen(
    viewModel: PostPhotoViewModel = hiltViewModel(),
) {
    val capturedPath by viewModel.capturedImagePath.collectAsStateWithLifecycle()
    val isCapturing by viewModel.isCapturing.collectAsStateWithLifecycle()
    val caption by viewModel.caption.collectAsStateWithLifecycle()
    val cameraSelector by viewModel.currentCameraSelector.collectAsStateWithLifecycle()

    // Load and rotate the JPEG on the IO thread any time capturedPath changes.
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(capturedPath) {
        imageBitmap = withContext(Dispatchers.IO) {
            capturedPath?.let { BitmapFactory.decodeFile(it).asImageBitmap() }
        }
    }

    // ── Root: vertical stack, fills entire screen ─────────────────────────────
    //
    // Modifier.fillMaxSize()  → the Column stretches to occupy the full screen.
    // Modifier.background()   → paints EmberBg behind everything.
    // Modifier.padding()      → 22 dp breathing room from both side edges; all
    //                           children share this inset automatically.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EmberBg)
            .padding(horizontal = 22.dp),
    ) {

        // ── Status / nav bar ──────────────────────────────────────────────────
        //
        // HudStrip has its own internal horizontal padding (14 dp) so it sits
        // slightly inset from the Column's 22 dp — that's intentional and keeps
        // it aligned with the Ember grid.
        HudStrip(presenceOn = true, minimal = true)

        // ── Camera / photo frame ──────────────────────────────────────────────
        //
        // Modifier.fillMaxWidth()  → stretches to the full Column width.
        // Modifier.weight(2f)      → "give me 2 units of whatever flexible space
        //                            is left after fixed-size siblings."
        //                            Since the action column below uses weight(1f),
        //                            the total is 3 units: this Box gets 2/3.
        //
        // Box is used (not Column/Row) because it stacks children in the Z axis,
        // which lets us overlay the caption bar and dismiss button directly on
        // top of the image without extra positioning tricks.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(2f),
        ) {
            when {
                // ── Live camera preview ───────────────────────────────────────
                //
                // Modifier.fillMaxSize() makes InFrameCamera expand to fill the
                // entire Box (which is already constrained to 2/3 of the screen).
                capturedPath == null -> InFrameCamera(
                    captureRequested = viewModel.captureRequested,
                    onImageCaptured = viewModel::onImageCaptured,
                    onCaptureFailed = viewModel::onCaptureFailed,
                    cameraSelector = cameraSelector,
                    modifier = Modifier.fillMaxSize(),
                )

                // ── Captured photo ────────────────────────────────────────────
                imageBitmap != null -> {
                    // Image fills the Box entirely; ContentScale.Crop ensures it
                    // never shows empty bars — it zooms and crops to fill.
                    Box(modifier = Modifier.fillMaxSize()) {
                        // blurred background to fill the empty space
                        Image(
                            bitmap = imageBitmap!!,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(20.dp),
                            contentScale = ContentScale.Crop,
                        )
                        // actual image on top, fully visible
                        Image(
                            bitmap = imageBitmap!!,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                    }

                    // Caption bar — overlaid at the bottom of the photo.
                    //
                    // align(Alignment.BottomStart) works because this composable
                    // is a direct child of a Box; Box knows how to position children
                    // at named anchors (TopStart, BottomCenter, etc.).
                    BasicTextField(
                        value = caption,
                        onValueChange = viewModel::onCaptionChanged,
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
                            if (caption.isEmpty()) {
                                Mono("caption (optional)…", size = 13.sp, color = EmberMuted)
                            }
                            inner()
                        },
                    )

                    // ✕ dismiss — overlaid at the top-left corner.
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .background(EmberBg.copy(alpha = 0.82f))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = viewModel::resetPhoto,
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Mono("✕", size = 12.sp, color = EmberFg, letterSpacing = 0.22.sp)
                    }
                }
            }
        }

        // ── Action area ───────────────────────────────────────────────────────
        //
        // weight(1f) → takes the remaining 1/3 of flexible space.
        // verticalArrangement = Arrangement.Center → buttons sit in the middle
        //   of this zone rather than being glued to the top edge, giving the
        //   action area visual breathing room against the camera frame above.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            if (capturedPath == null) {
                // Live camera — flip stub + take button side by side.
                //
                // Arrangement.spacedBy(8.dp) → 8 dp gap between the two buttons.
                // Modifier.weight(1f) on TAKE → it expands to fill whatever width
                //   the flip button doesn't consume (i.e. "take the rest").
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PxButton(
                        text = "⟳",
                        onClick = viewModel::switchCamera,
                    )
                    PxButton(
                        text = if (isCapturing) "  …  " else "◉ TAKE",
                        onClick = viewModel::requestCapture,
                        modifier = Modifier.weight(1f),
                        primary = true,
                    )
                }
            } else {
                // Photo captured — single full-width post button.
                PxButton(
                    text = "POST TO RADIUS →",
                    onClick = viewModel::submit,
                    modifier = Modifier.fillMaxWidth(),
                    primary = true,
                )
            }
        }

        // ── Metadata strip ────────────────────────────────────────────────────
        //
        // Natural (unwrapped) height — sits below both weighted sections.
        // SpaceBetween pushes the two labels to opposite edges of the row.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Mono("EXPIRES ON EXIT", size = 8.sp, color = EmberMuted, letterSpacing = 0.18.sp)
            Mono("41°59′N · 21°25′E", size = 8.sp, color = EmberMuted, letterSpacing = 0.14.sp)
        }
    }
}