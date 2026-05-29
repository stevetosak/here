package net.tosak.here.screens.composer.postphoto

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.tosak.here.screens.composer.postphoto.components.CameraPreview
import net.tosak.here.screens.composer.postphoto.components.CapturedPhoto
import net.tosak.here.screens.composer.postphoto.components.controls.CaptureControls
import net.tosak.here.screens.composer.postphoto.components.controls.PostButton
import net.tosak.here.screens.composer.viewmodel.PostPhotoViewModel
import net.tosak.here.shared.components.HudStrip
import net.tosak.here.shared.components.Mono
import net.tosak.here.ui.theme.EmberBg
import net.tosak.here.ui.theme.EmberMuted

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

    // Load and rotate the JPEG on the IO thread any time capturedPath changes.
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(capturedPath) {
        imageBitmap = withContext(Dispatchers.IO) {
            capturedPath?.let { BitmapFactory.decodeFile(it).asImageBitmap() }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EmberBg)
            .padding(horizontal = 22.dp),
    ) {

        HudStrip(presenceOn = true, minimal = true)

        when {
            capturedPath == null -> {
                CameraPreview(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(2f)
                )
            }

            imageBitmap != null -> {
                CapturedPhoto(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(2f),
                    imageBitmap = imageBitmap!!
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            if (capturedPath == null) {
                CaptureControls()
            } else {
                PostButton(viewModel::submit)
            }
        }

        // ── Metadata strip ────────────────────────────────────────────────────
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