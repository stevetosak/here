package net.tosak.here.screens.composer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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

    /**
     * Persists the post locally and calls [onDone] on the main thread when saved.
     *
     * [imagePath] is the absolute path of the captured photo in filesDir, or null
     * for text posts (and photo posts before camera integration is wired up).
     */
    fun submit(
        kind: PostKind,
        caption: String,
        imagePath: String?,
        onDone: () -> Unit,
    ) {
        val loc = locationRepository.lastLocation.value
        viewModelScope.launch {
            postRepository.savePost(
                kind      = kind,
                caption   = caption,
                imagePath = imagePath,
                lat       = loc?.latitude  ?: 0.0,
                lng       = loc?.longitude ?: 0.0,
            )
            onDone()
        }
    }
}
