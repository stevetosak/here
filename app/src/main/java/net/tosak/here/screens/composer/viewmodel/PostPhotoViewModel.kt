package net.tosak.here.screens.composer.viewmodel

import android.R.attr.path
import android.graphics.BitmapFactory
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.tosak.here.screens.composer.postphoto.components.controls.CaptureControls
import net.tosak.here.screens.composer.postphoto.components.controls.PostButton
import net.tosak.here.shared.events.Event
import net.tosak.here.shared.events.EventBus
import net.tosak.here.shared.location.LocationRepository
import net.tosak.here.shared.model.AppScreen
import net.tosak.here.shared.model.PostKind
import net.tosak.here.shared.storage.PostRepository
import javax.inject.Inject


sealed interface PostPhotoUiState {
    data object Preview : PostPhotoUiState
    data object Capturing : PostPhotoUiState
    data object Captured : PostPhotoUiState
}


@HiltViewModel
class PostPhotoViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val locationRepository: LocationRepository,
    private val eventBus: EventBus,
) : PostViewModel, ViewModel() {

    val captureRequested = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val _capturedImagePath = MutableStateFlow<String?>(null)
    val capturedImagePath = _capturedImagePath.asStateFlow()

    private val _caption = MutableStateFlow("")
    val caption = _caption.asStateFlow()

    private val _currentCameraSelector = MutableStateFlow(CameraSelector.DEFAULT_BACK_CAMERA)
    val currentCameraSelector = _currentCameraSelector.asStateFlow()

    private val _flashEnabled = MutableStateFlow(false)
    val flashEnabled = _flashEnabled.asStateFlow();

    @OptIn(ExperimentalCoroutinesApi::class)
    val imageBitmap: StateFlow<ImageBitmap?> = _capturedImagePath
        .mapLatest { path ->
            withContext(Dispatchers.IO) {
                path?.let { BitmapFactory.decodeFile(it).asImageBitmap() }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        );


    private val _isCapturing = MutableStateFlow(false)

    val uiState: StateFlow<PostPhotoUiState> = combine(
        _isCapturing,
        imageBitmap
    ) { isCapturing, bitmap ->
        when {
            isCapturing -> PostPhotoUiState.Capturing
            bitmap != null -> PostPhotoUiState.Captured
            else -> PostPhotoUiState.Preview
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PostPhotoUiState.Preview
    )


    fun switchCamera() {
        if (currentCameraSelector.value == CameraSelector.DEFAULT_BACK_CAMERA) {
            _currentCameraSelector.value = CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            _currentCameraSelector.value = CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    fun toggleFlash() {
        _flashEnabled.value = !flashEnabled.value
    }

    fun onCaptionChanged(value: String) {
        _caption.value = value
    }

    fun requestCapture() {
        _isCapturing.value = true
        captureRequested.tryEmit(Unit)
        eventBus.emit(Event.Loading.Show)
    }

    fun onImageCaptured(path: String) {
        _capturedImagePath.value = path
        _isCapturing.value = false
        eventBus.emit(Event.Loading.Hide)
    }

    fun onCaptureFailed() {
        _isCapturing.value = false
        eventBus.emit(Event.Loading.Hide)
    }

    fun resetPhoto() {
        _capturedImagePath.value = null
        _isCapturing.value = false
    }

    override fun submit() {
        val loc = locationRepository.lastLocation.value
        viewModelScope.launch {
            postRepository.savePost(
                kind = PostKind.PHOTO,
                caption = _caption.value,
                imagePath = _capturedImagePath.value,
                lat = loc?.latitude ?: 0.0,
                lng = loc?.longitude ?: 0.0,
            )
            resetPhoto()
            _caption.value = ""
            eventBus.emit(Event.Toast.Show("POSTED · EXPIRES ON EXIT"))
            eventBus.emit(Event.Nav.ReplaceTopAndAppend(listOf(AppScreen.MAP, AppScreen.OWN_POST)));
        }
    }
}