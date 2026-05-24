package net.tosak.here.screens.composer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.tosak.here.shared.location.LocationRepository
import net.tosak.here.shared.model.PostKind
import net.tosak.here.shared.storage.PostRepository
import javax.inject.Inject

@HiltViewModel
class ComposerViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val locationRepository: LocationRepository,
) : ViewModel() {

    // ── Camera capture coordination ───────────────────────────────────────────

    /** ComposerScreen footer button emits here; InFrameCamera collects and fires takePicture(). */
    val captureRequested = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val _capturedImagePath = MutableStateFlow<String?>(null)
    /** Absolute path of the captured JPEG, or null when no photo has been taken yet. */
    val capturedImagePath = _capturedImagePath.asStateFlow()

    private val _isCapturing = MutableStateFlow(false)
    /** True while takePicture() is in flight. */
    val isCapturing = _isCapturing.asStateFlow()

    fun requestCapture() {
        _isCapturing.value = true
        captureRequested.tryEmit(Unit)
    }

    fun onImageCaptured(path: String) {
        _capturedImagePath.value = path
        _isCapturing.value = false
    }

    fun onCaptureFailed() {
        _isCapturing.value = false
    }

    /** Clears the captured photo so the camera preview is shown again (retake). */
    fun resetPhoto() {
        _capturedImagePath.value = null
    }

    // ── Post submission ───────────────────────────────────────────────────────

    /**
     * Persists the post locally and calls [onDone] on the main thread when saved.
     */
    fun submit(
        kind: PostKind,
        caption: String,
        onDone: () -> Unit,
    ) {
        val loc = locationRepository.lastLocation.value
        viewModelScope.launch {
            postRepository.savePost(
                kind      = kind,
                caption   = caption,
                imagePath = _capturedImagePath.value,
                lat       = loc?.latitude  ?: 0.0,
                lng       = loc?.longitude ?: 0.0,
            )
            onDone()
        }
    }
}