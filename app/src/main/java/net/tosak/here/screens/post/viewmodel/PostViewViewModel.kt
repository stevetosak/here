package net.tosak.here.screens.post.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import net.tosak.here.shared.events.Event
import net.tosak.here.shared.events.EventBus
import net.tosak.here.shared.model.AppScreen
import javax.inject.Inject

@HiltViewModel
class PostViewViewModel @Inject constructor(
    private val eventBus: EventBus,
) : ViewModel() {

    fun onClose() {
        eventBus.emit(Event.Nav.GoBack)
    }

    fun onChat(seed: String?) {
        eventBus.emit(Event.AppState.ChatSeedChanged(seed))
        eventBus.emit(Event.Nav.NavigateTo(AppScreen.CHAT))
    }
}