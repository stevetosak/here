package net.tosak.here.screens.composer.postphoto.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.tosak.here.screens.composer.postphoto.components.controls.FlashButton
import net.tosak.here.screens.composer.viewmodel.PostPhotoViewModel

@Composable
fun CameraPreview(modifier: Modifier,viewModel: PostPhotoViewModel = hiltViewModel()){

    val cameraSelector by viewModel.currentCameraSelector.collectAsStateWithLifecycle()
    val flashEnabled by viewModel.flashEnabled.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
    ) {
        CameraPreviewHandler(
            cameraSelector = cameraSelector,
            modifier = Modifier.fillMaxSize(),
            flashEnabled = flashEnabled
        )
        FlashButton(modifier = Modifier.align(Alignment.TopEnd))
    }

}