package net.tosak.here.screens.chat.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import net.tosak.here.shared.events.Event
import net.tosak.here.shared.events.EventBus
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val eventBus: EventBus,
) : ViewModel() {

    fun onClose() {
        eventBus.emit(Event.Nav.GoBack)
    }
}