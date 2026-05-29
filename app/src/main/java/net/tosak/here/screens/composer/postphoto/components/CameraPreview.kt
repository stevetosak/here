package net.tosak.here.screens.composer.postphoto.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.tosak.here.R
import net.tosak.here.screens.composer.postphoto.components.controls.FlashButton
import net.tosak.here.screens.composer.viewmodel.PostPhotoViewModel
import net.tosak.here.ui.theme.EmberBg
import net.tosak.here.ui.theme.EmberFg

@Composable
fun CameraPreview(modifier: Modifier,viewModel: PostPhotoViewModel = hiltViewModel()){

    val cameraSelector by viewModel.currentCameraSelector.collectAsStateWithLifecycle()
    val flashEnabled by viewModel.flashEnabled.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
    ) {
        InFrameCamera(
            captureRequested = viewModel.captureRequested,
            onImageCaptured = viewModel::onImageCaptured,
            onCaptureFailed = viewModel::onCaptureFailed,
            cameraSelector = cameraSelector,
            modifier = Modifier.fillMaxSize(),
            flashEnabled = flashEnabled
        )
        FlashButton(modifier = Modifier.align(Alignment.TopEnd))
    }

}