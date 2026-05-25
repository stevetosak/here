package net.tosak.here.screens.composer.viewmodel

import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.tosak.here.shared.events.Event
import net.tosak.here.shared.events.EventBus
import net.tosak.here.shared.location.LocationRepository
import net.tosak.here.shared.model.AppScreen
import net.tosak.here.shared.model.PostKind
import net.tosak.here.shared.storage.PostRepository
import javax.inject.Inject

@HiltViewModel
class PostPhotoViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val locationRepository: LocationRepository,
    private val eventBus: EventBus,
) : PostViewModel, ViewModel() {

    // ── Camera capture coordination ───────────────────────────────────────────

    /** Footer button emits here; InFrameCamera collects and fires takePicture(). */
    val captureRequested = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val _capturedImagePath = MutableStateFlow<String?>(null)
    /** Absolute path of the captured JPEG, or null when no photo has been taken yet. */
    val capturedImagePath = _capturedImagePath.asStateFlow()
    private val _isCapturing = MutableStateFlow(false)
    /** True while takePicture() is in flight. */
    val isCapturing = _isCapturing.asStateFlow()

    private val _caption = MutableStateFlow("")
    val caption = _caption.asStateFlow()

    private val _currentCameraSelector = MutableStateFlow(CameraSelector.DEFAULT_BACK_CAMERA)
    val currentCameraSelector = _currentCameraSelector.asStateFlow()


    fun switchCamera(){
        if(currentCameraSelector.value == CameraSelector.DEFAULT_BACK_CAMERA){
            _currentCameraSelector.value = CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            _currentCameraSelector.value = CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    fun onCaptionChanged(value: String) { _caption.value = value }

    fun requestCapture() {
        _isCapturing.value = true
        captureRequested.tryEmit(Unit)
    }

    fun onImageCaptured(path: String) {
        _capturedImagePath.value = path
        _isCapturing.value = false
    }

    fun onCaptureFailed() { _isCapturing.value = false }

    /** Clears the captured photo so the camera preview is shown again (retake). */
    fun resetPhoto() { _capturedImagePath.value = null }

    // ── Post submission ───────────────────────────────────────────────────────

    /**
     * Saves the photo post locally, then navigates back and shows a toast.
     * [onDone] is called after the save completes — callers may pass an empty
     * lambda if they rely solely on EventBus navigation.
     */
    override fun submit() {
        val loc = locationRepository.lastLocation.value
        viewModelScope.launch {
            postRepository.savePost(
                kind      = PostKind.PHOTO,
                caption   = _caption.value,
                imagePath = _capturedImagePath.value,
                lat       = loc?.latitude  ?: 0.0,
                lng       = loc?.longitude ?: 0.0,
            )
            resetPhoto()
            _caption.value = ""
            eventBus.emit(Event.Toast.Show("POSTED · EXPIRES ON EXIT"))
            eventBus.emit(Event.Nav.ReplaceTop(AppScreen.MAP))
        }
    }
}