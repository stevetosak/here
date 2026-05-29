@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH")

package net.tosak.here.screens.composer.postphoto.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import net.tosak.here.screens.composer.postphoto.camera.awaitPicture
import net.tosak.here.screens.composer.postphoto.camera.toRotatedBitmap
import net.tosak.here.screens.composer.viewmodel.PostPhotoViewModel
import net.tosak.here.shared.components.Mono
import net.tosak.here.ui.theme.EmberAccent
import net.tosak.here.ui.theme.EmberBorder
import net.tosak.here.ui.theme.EmberMuted
import java.io.File
import java.io.FileOutputStream

@Composable
fun CameraPreviewHandler(
    modifier: Modifier = Modifier,
    scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    flashEnabled: Boolean,
) {
    val context = LocalContext.current
    var hasCamPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCamPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCamPermission) permLauncher.launch(Manifest.permission.CAMERA)
    }

    val controller = rememberCameraController(cameraSelector, flashEnabled, scaleType)
    CaptureListener(imageCapture = controller.imageCapture, cameraSelector = cameraSelector)

    Box(modifier = modifier.border(1.dp, EmberBorder)) {
        if (hasCamPermission) {
            AndroidView(modifier = Modifier.fillMaxSize(), factory = { controller.previewView })
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Mono("[CAMERA PERMISSION REQUIRED]", size = 9.sp, color = EmberMuted, letterSpacing = 0.2.sp)
                    Spacer(Modifier.height(6.dp))
                    Mono(
                        "TAP TO REQUEST",
                        size = 8.sp,
                        color = EmberAccent,
                        letterSpacing = 0.2.sp,
                        modifier = Modifier.clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) { permLauncher.launch(Manifest.permission.CAMERA) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CaptureListener(
    imageCapture: androidx.camera.core.ImageCapture,
    cameraSelector: CameraSelector,
    viewModel: PostPhotoViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val currentSelector by rememberUpdatedState(cameraSelector)

    LaunchedEffect(viewModel.captureRequested) {
        viewModel.captureRequested.collect {
            val file = File(File(context.filesDir, "images").also { it.mkdirs() }, "here_${System.currentTimeMillis()}.jpg")
            val saved = imageCapture.awaitPicture(ContextCompat.getMainExecutor(context), file)
            if (saved != null) {
                saved.toRotatedBitmap(currentSelector == CameraSelector.DEFAULT_FRONT_CAMERA)
                    ?.also { bmp ->
                        FileOutputStream(saved).use { bmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                        bmp.recycle()
                    }
                viewModel.onImageCaptured(saved.absolutePath)
            } else {
                viewModel.onCaptureFailed()
            }
        }
    }
}