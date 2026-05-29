package net.tosak.here.screens.composer.postphoto.components.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import net.tosak.here.screens.composer.viewmodel.PostPhotoViewModel
import net.tosak.here.shared.components.PxButton

@Composable
fun CaptureControls(viewModel: PostPhotoViewModel = hiltViewModel()){

    val isCapturing by viewModel.isCapturing.collectAsStateWithLifecycle()
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

}