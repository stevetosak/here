package net.tosak.here.screens.composer.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.camera.core.Camera
import androidx.camera.core.FocusMeteringAction
import androidx.camera.view.PreviewView
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.util.concurrent.TimeUnit

fun File.toRotatedImageBitmap(): ImageBitmap {
    val exif = ExifInterface(absolutePath)
    val rotation = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90  -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    val original = BitmapFactory.decodeFile(absolutePath)
    if (rotation == 0f) return original.asImageBitmap()
    val matrix = Matrix().apply { postRotate(rotation) }
    return Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
        .also { original.recycle() }
        .asImageBitmap()
}

fun setupTapToFocus(previewView: PreviewView, camera: Camera){
    previewView.setOnTouchListener { view, event ->
        if (event.action == MotionEvent.ACTION_UP) {
            val point = previewView.meteringPointFactory
                .createPoint(event.x, event.y)
            val action = FocusMeteringAction.Builder(point)
                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                .build()
            camera.cameraControl.startFocusAndMetering(action)
            view.performClick()
        }
        true
    }
}

fun setupZoom(context: Context,previewView: PreviewView,camera: Camera){
    val scaleGesture = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val currentZoom =
                    camera.cameraInfo.zoomState.value?.zoomRatio ?: 1f
                camera.cameraControl.setZoomRatio(currentZoom * detector.scaleFactor)
                return true
            }
        }
    )

    // zoom with auto focus
    previewView.setOnTouchListener { view, event ->
        scaleGesture.onTouchEvent(event)
        if (event.action == MotionEvent.ACTION_UP && !scaleGesture.isInProgress) {
            val point = previewView.meteringPointFactory
                .createPoint(event.x, event.y)
            val action = FocusMeteringAction.Builder(point)
                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                .build()
            camera.cameraControl.startFocusAndMetering(action)
        }
        view.performClick()
        true
    }
}