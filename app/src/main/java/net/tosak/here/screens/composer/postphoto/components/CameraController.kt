package net.tosak.here.screens.composer.postphoto.components

import android.content.Context
import android.view.OrientationEventListener
import android.view.Surface
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import net.tosak.here.screens.composer.postphoto.camera.setupFlash
import net.tosak.here.screens.composer.postphoto.camera.setupTapToFocus
import net.tosak.here.screens.composer.postphoto.camera.setupZoom

class CameraController(
    val previewView: PreviewView,
    val imageCapture: ImageCapture,
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
) {
    private var camera: Camera? = null
    private var provider: ProcessCameraProvider? = null
    private val future = ProcessCameraProvider.getInstance(context)

    private val orientationListener = object : OrientationEventListener(context) {
        override fun onOrientationChanged(orientation: Int) {
            imageCapture.targetRotation = when (orientation) {
                in 45..134 -> Surface.ROTATION_270
                in 135..224 -> Surface.ROTATION_180
                in 225..314 -> Surface.ROTATION_90
                else -> Surface.ROTATION_0
            }
        }
    }

    fun bind(selector: CameraSelector, flashEnabled: Boolean) {
        camera = null
        future.addListener({
            val p = future.get()
            provider = p
            val preview = Preview.Builder().build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }
            try {
                p.unbindAll()
                val cam = p.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
                setupFlash(cam, flashEnabled)
                setupTapToFocus(previewView, cam)
                setupZoom(context, previewView, cam)
                camera = cam
            } catch (_: Exception) {}
        }, ContextCompat.getMainExecutor(context))
    }

    fun applyFlash(flashEnabled: Boolean) = camera?.let { setupFlash(it, flashEnabled) }

    fun enableOrientation() = orientationListener.enable()
    fun disableOrientation() = orientationListener.disable()
    fun release() = provider?.unbindAll()
}

@Composable
fun rememberCameraController(
    cameraSelector: CameraSelector,
    flashEnabled: Boolean,
    scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER,
): CameraController {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            this.scaleType = scaleType
            setPadding(10)
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val imageCapture = remember { ImageCapture.Builder().setTargetRotation(Surface.ROTATION_0).build() }
    val controller = remember { CameraController(previewView, imageCapture, context, lifecycleOwner) }

    LaunchedEffect(cameraSelector) { controller.bind(cameraSelector, flashEnabled) }
    LaunchedEffect(flashEnabled) { controller.applyFlash(flashEnabled) }
    DisposableEffect(Unit) {
        controller.enableOrientation()
        onDispose {
            controller.disableOrientation()
            controller.release()
        }
    }

    return controller
}
