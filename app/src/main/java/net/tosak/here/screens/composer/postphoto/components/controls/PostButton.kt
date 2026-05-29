package net.tosak.here.screens.composer.postphoto.components.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.tosak.here.screens.composer.viewmodel.PostPhotoViewModel
import net.tosak.here.shared.components.PxButton

@Composable
fun PostButton(modifier: Modifier, onSubmit: () -> Unit) {

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        PxButton(
            text = "POST TO RADIUS →",
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            primary = true,
        )
    }

}