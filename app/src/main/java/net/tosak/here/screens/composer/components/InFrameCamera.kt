package net.tosak.here.screens.composer.components

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
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
import androidx.core.view.setPadding
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.Flow
import net.tosak.here.screens.composer.camera.setupTapToFocus
import net.tosak.here.screens.composer.camera.setupZoom
import net.tosak.here.shared.components.Mono
import net.tosak.here.ui.theme.EmberAccent
import net.tosak.here.ui.theme.EmberBorder
import net.tosak.here.ui.theme.EmberFg
import net.tosak.here.ui.theme.EmberMuted
import java.io.File

/**
 * Embedded CameraX preview that lives inside any parent frame.
 *
 * Renders a full-size [PreviewView] with tap-to-focus and pinch-to-zoom.
 * There are no buttons inside this composable — the parent controls capture
 * by emitting a value to [captureRequested].
 *
 * Camera permission is requested inline the first time the composable enters the tree.
 * If the user denies it, a tap-to-retry nudge is shown in place of the preview.
 */
@Composable
fun InFrameCamera(
    captureRequested: Flow<Unit>,
    onImageCaptured: (String) -> Unit,
    onCaptureFailed: () -> Unit,
    scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraState = remember { mutableStateOf<Camera?>(null) }

    // ── Permission ─────────────────────────────────────────────────────────────
    var hasCamPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCamPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCamPermission) permLauncher.launch(Manifest.permission.CAMERA)
    }

    // ── CameraX ────────────────────────────────────────────────────────────────
    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraProvider = remember { mutableStateOf<ProcessCameraProvider?>(null) }

    // Release the camera when this composable leaves the tree.
    DisposableEffect(lifecycleOwner) {
        onDispose { cameraProvider.value?.unbindAll() }
    }

    // ── Capture trigger ────────────────────────────────────────────────────────
    // Collects the shared flow emitted by the footer button in ComposerScreen.
    LaunchedEffect(captureRequested) {
        captureRequested.collect {
            val imgDir = File(context.filesDir, "images").also { it.mkdirs() }
            val file = File(imgDir, "photo_${System.currentTimeMillis()}.jpg")
            imageCapture.takePicture(
                ImageCapture.OutputFileOptions.Builder(file).build(),
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(out: ImageCapture.OutputFileResults) =
                        onImageCaptured(file.absolutePath)

                    override fun onError(e: ImageCaptureException) {
                        file.delete()
                        onCaptureFailed()
                    }
                },
            )
        }
    }

    // ── Layout ─────────────────────────────────────────────────────────────────
    Box(modifier = modifier.border(1.dp, EmberBorder)) {
        if (hasCamPermission) {
            AndroidView(
                modifier = Modifier
                    .padding(0.dp)
                    .fillMaxSize(),
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        this.scaleType = scaleType
                        setPadding(10)
                        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }
                },
                update = { previewView ->
                    cameraProviderFuture.addListener({
                        val provider = cameraProviderFuture.get()
                        cameraProvider.value = provider
                        val preview = Preview.Builder().build()
                            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                        try {
                            provider.unbindAll()
                            val camera = provider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture,
                            )
                            cameraState.value = camera
                            setupTapToFocus(previewView = previewView, camera = camera)
                            setupZoom(context = context, previewView = previewView, camera = camera)
                        } catch (_: Exception) { /* camera unavailable on this device */ }
                    }, ContextCompat.getMainExecutor(context))
                }
            )
        } else {
            // Permission denied — show inline fallback.
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Mono(
                        "[CAMERA PERMISSION REQUIRED]",
                        size = 9.sp,
                        color = EmberMuted,
                        letterSpacing = 0.2.sp,
                    )
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