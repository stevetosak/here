package net.tosak.here.screens.composer.postphoto.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.camera.core.Camera
import androidx.camera.core.FocusMeteringAction
import androidx.camera.view.PreviewView
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.util.concurrent.TimeUnit

fun File.toRotatedBitmap(forceFlipHorizontal: Boolean = false): Bitmap? {
    val exif = ExifInterface(absolutePath)
    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

    val (rotation, exifFlip) = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90       -> 90f  to false
        ExifInterface.ORIENTATION_ROTATE_180      -> 180f to false
        ExifInterface.ORIENTATION_ROTATE_270      -> 270f to false
        ExifInterface.ORIENTATION_TRANSPOSE       -> 90f  to true
        ExifInterface.ORIENTATION_TRANSVERSE      -> 270f to true
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> 0f   to true
        ExifInterface.ORIENTATION_FLIP_VERTICAL   -> 180f to true
        else                                      -> 0f   to false
    }

    val flip = exifFlip || forceFlipHorizontal  // ← combine both sources
    val original = BitmapFactory.decodeFile(absolutePath)

    if (rotation == 0f && !flip) return original

    val matrix = Matrix().apply {
        if (rotation != 0f) postRotate(rotation)
        if (flip) postScale(-1f, 1f)
    }

    return Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
        .also { original.recycle() }
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

fun setupFlash(camera:Camera,flashEnabled: Boolean){
    if(camera.cameraInfo.hasFlashUnit()){
        camera.cameraControl.enableTorch(flashEnabled)
    }
}