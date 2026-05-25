package net.tosak.here.screens.composer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import net.tosak.here.shared.location.LocationRepository
import net.tosak.here.shared.model.PostKind
import net.tosak.here.shared.storage.PostRepository
import javax.inject.Inject

/**
 * ViewModel for [ComposerScreen].
 *
 * Owns only the TEXT post submission path. Photo post logic lives in
 * [PostPhotoViewModel] — the PHOTO kind navigates to a dedicated screen.
 */
@HiltViewModel
class ComposerViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val locationRepository: LocationRepository,
) : ViewModel() {

    /**
     * Persists a TEXT post locally and calls [onDone] on the main thread when saved.
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
                imagePath = null,   // TEXT posts carry no image
                lat       = loc?.latitude  ?: 0.0,
                lng       = loc?.longitude ?: 0.0,
            )
            onDone()
        }
    }
}