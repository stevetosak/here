package net.tosak.here.screens.composer.postphoto.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import net.tosak.here.screens.composer.viewmodel.PostPhotoViewModel
import net.tosak.here.shared.components.Mono
import net.tosak.here.ui.theme.EmberAccent
import net.tosak.here.ui.theme.EmberBg
import net.tosak.here.ui.theme.EmberFg
import net.tosak.here.ui.theme.EmberMuted
import net.tosak.here.ui.theme.JetBrainsMono

@Composable
fun CapturedPhoto(
    modifier: Modifier,
    imageBitmap: ImageBitmap,
    viewModel: PostPhotoViewModel = hiltViewModel()
) {
    // Image fills the Box entirely; ContentScale.Crop ensures it
    // never shows empty bars — it zooms and crops to fill.

    val caption by viewModel.caption.collectAsStateWithLifecycle()
    Box(
        modifier = modifier
    ){

        Box(modifier = Modifier.fillMaxSize()) {
            // blurred background to fill the empty space
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(20.dp),
                contentScale = ContentScale.Crop,
            )
            // actual image on top, fully visible
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }

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