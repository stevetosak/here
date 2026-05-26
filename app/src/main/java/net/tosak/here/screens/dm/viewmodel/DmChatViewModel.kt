package net.tosak.here.screens.dm.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.tosak.here.shared.events.Event
import net.tosak.here.shared.events.EventBus
import net.tosak.here.shared.model.Friend
import net.tosak.here.shared.state.AppStateRepository
import net.tosak.here.shared.storage.ChatRepository
import net.tosak.here.shared.storage.DmMessageEntity
import javax.inject.Inject
import kotlin.OptIn
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DmChatViewModel @Inject constructor(
    private val eventBus: EventBus,
    private val chatRepository: ChatRepository,
    appStateRepository: AppStateRepository,
) : ViewModel() {

    val activeFriend: StateFlow<Friend?> = appStateRepository.activeFriend

    val messages: StateFlow<List<DmMessageEntity>> = activeFriend
        .flatMapLatest { f ->
            if (f == null) kotlinx.coroutines.flow.flowOf(emptyList())
            else chatRepository.messages(f.id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private var typingJob: Job? = null

    private val replyPool = listOf(
        "k", "ok", "lol", "haha", "yes", "no", "maybe",
        "where r u", "on my way", "give me a sec", "in 10",
        "💀", "fr", "ye", "nice", "sounds good", "lmk",
    )

    fun sendText(text: String) {
        val friend = activeFriend.value ?: return
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { chatRepository.sendText(friend.id, trimmed) }
        scheduleMockReply(friend.id)
    }

    fun sendImage(uri: Uri) {
        val friend = activeFriend.value ?: return
        viewModelScope.launch { chatRepository.sendImage(friend.id, uri) }
        scheduleMockReply(friend.id)
    }

    private fun scheduleMockReply(friendId: String) {
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            _isTyping.value = true
            delay(3_000)
            chatRepository.receiveText(friendId, replyPool.random())
            _isTyping.value = false
        }
    }

    fun onBack() {
        typingJob?.cancel()
        _isTyping.value = false
        eventBus.emit(Event.Nav.GoBack)
    }

    override fun onCleared() {
        super.onCleared()
        typingJob?.cancel()
    }
}
