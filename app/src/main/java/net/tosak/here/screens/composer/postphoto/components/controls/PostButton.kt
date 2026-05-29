package net.tosak.here.screens.composer.postphoto.components.controls

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import net.tosak.here.screens.composer.viewmodel.PostPhotoViewModel
import net.tosak.here.shared.components.PxButton

@Composable
fun PostButton(onSubmit : () -> Unit) {
    PxButton(
        text = "POST TO RADIUS →",
        onClick = onSubmit,
        modifier = Modifier.fillMaxWidth(),
        primary = true,
    )
}