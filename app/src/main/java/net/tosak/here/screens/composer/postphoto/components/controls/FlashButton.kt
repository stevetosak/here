package net.tosak.here.screens.composer.postphoto.components.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import net.tosak.here.R
import net.tosak.here.screens.composer.viewmodel.PostPhotoViewModel
import net.tosak.here.ui.theme.EmberBg
import net.tosak.here.ui.theme.EmberFg

@Composable
fun FlashButton(modifier: Modifier, viewModel: PostPhotoViewModel = hiltViewModel()){
    val flashEnabled by viewModel.flashEnabled.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .padding(15.dp)
            .background(EmberBg.copy(alpha = 0.22f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = viewModel::toggleFlash,
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Icon(
            painter = painterResource(
                if (flashEnabled) R.drawable.flash_on else R.drawable.flash_off
            ),
            contentDescription = null,
            tint = EmberFg,
            modifier = Modifier.size(20.dp),
        )
    }
}