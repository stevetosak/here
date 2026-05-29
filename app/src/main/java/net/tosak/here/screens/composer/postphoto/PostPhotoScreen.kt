package net.tosak.here.screens.composer.postphoto

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.tosak.here.screens.composer.postphoto.components.CameraPreview
import net.tosak.here.screens.composer.postphoto.components.CapturedPhoto
import net.tosak.here.screens.composer.postphoto.components.controls.CaptureControls
import net.tosak.here.screens.composer.postphoto.components.controls.PostButton
import net.tosak.here.screens.composer.viewmodel.PostPhotoUiState
import net.tosak.here.screens.composer.viewmodel.PostPhotoViewModel
import net.tosak.here.shared.components.HudStrip
import net.tosak.here.shared.components.LoadingOverlay
import net.tosak.here.shared.components.Mono
import net.tosak.here.ui.theme.EmberBg
import net.tosak.here.ui.theme.EmberMuted

@Composable
fun PostPhotoScreen(
    viewModel: PostPhotoViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val imageBitmap by viewModel.imageBitmap.collectAsStateWithLifecycle()
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(EmberBg)
                .padding(horizontal = 22.dp),
        ) {
            HudStrip(presenceOn = true, minimal = true)

            when (state) {
                PostPhotoUiState.Preview,
                PostPhotoUiState.Capturing -> {
                    CameraPreview(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(2f)
                    )
                    CaptureControls(
                        Modifier.weight(1f),
                        viewModel::switchCamera,
                        viewModel::requestCapture
                    )
                }

                PostPhotoUiState.Captured -> {
                    CapturedPhoto(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(2f),
                        imageBitmap = imageBitmap!!
                    )
                    PostButton(Modifier.weight(1f), viewModel::submit)
                }
            }

            // ── Metadata strip ────────────────────────────────────────────────
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

        LoadingOverlay(visible = state == PostPhotoUiState.Capturing)
    }
}