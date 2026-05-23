package net.tosak.here.screens.post.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.tosak.here.shared.storage.PostEntity
import net.tosak.here.shared.storage.PostRepository
import javax.inject.Inject

@HiltViewModel
class OwnPostViewModel @Inject constructor(
    private val postRepository: PostRepository,
) : ViewModel() {

    /** The user's current active post, or null if it has expired / been deleted. */
    val activePost: StateFlow<PostEntity?> = postRepository.activePosts
        .map { it.firstOrNull() }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    fun deletePost(onDeleted: () -> Unit) {
        val post = activePost.value ?: return
        viewModelScope.launch {
            postRepository.deletePost(post.id)
            onDeleted()
        }
    }
}
