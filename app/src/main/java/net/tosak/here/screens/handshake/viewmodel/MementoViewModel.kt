package net.tosak.here.screens.handshake.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import net.tosak.here.shared.events.Event
import net.tosak.here.shared.events.EventBus
import net.tosak.here.shared.model.AppScreen
import javax.inject.Inject

@HiltViewModel
class MementoViewModel @Inject constructor(
    private val eventBus: EventBus,
) : ViewModel() {

    fun onContinue() {
        eventBus.emit(Event.AppState.PendingMementoChanged(null))
        eventBus.emit(Event.Toast.Show("CONNECTED · SAVED TO MOMENTS"))
        eventBus.emit(Event.Nav.Reset(AppScreen.MAP))
    }
}