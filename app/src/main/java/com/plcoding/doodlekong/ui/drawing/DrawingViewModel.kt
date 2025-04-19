package com.plcoding.doodlekong.ui.drawing

import androidx.annotation.IdRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.plcoding.doodlekong.R
import com.plcoding.doodlekong.data.remote.ws.DrawingAPI
import com.plcoding.doodlekong.data.remote.ws.models.Announcement
import com.plcoding.doodlekong.data.remote.ws.models.BaseModel
import com.plcoding.doodlekong.data.remote.ws.models.ChatMessage
import com.plcoding.doodlekong.data.remote.ws.models.ChosenWord
import com.plcoding.doodlekong.data.remote.ws.models.DrawAction
import com.plcoding.doodlekong.data.remote.ws.models.DrawAction.Companion.ACTION_UNDO
import com.plcoding.doodlekong.data.remote.ws.models.DrawData
import com.plcoding.doodlekong.data.remote.ws.models.GameError
import com.plcoding.doodlekong.data.remote.ws.models.GameState
import com.plcoding.doodlekong.data.remote.ws.models.NewWords
import com.plcoding.doodlekong.data.remote.ws.models.Ping
import com.plcoding.doodlekong.data.remote.ws.models.RoundDrawInfo
import com.plcoding.doodlekong.utils.DispatcherProvider
import com.tinder.scarlet.WebSocket
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DrawingViewModel @Inject constructor(
    private val drawingAPI: DrawingAPI,
    private val dispatchers: DispatcherProvider,
    private val gson: Gson
) : ViewModel() {

    sealed class SocketEvent {
        data class CheckMessageEvent(val data: ChatMessage): SocketEvent()
        data class AnnouncementEvent(val data: Announcement): SocketEvent()
        data class GameStateEvent(val data: GameState): SocketEvent()
        data class DrawDataEvent(val data: DrawData): SocketEvent()
        data class NewWordsEvent(val data: NewWords): SocketEvent()
        data class ChosenWordEvent(val data: ChosenWord): SocketEvent()
        data class GameErrorEvent(val data: GameError): SocketEvent()
        data class RoundDrawInfoEvent(val data: RoundDrawInfo): SocketEvent()
        data object UndoEvent: SocketEvent()
    }

    private val _selectedColorButtonId = MutableStateFlow(
        ColorResourceId(R.id.rbBlack)
    )
    val selectedColorButtonId = _selectedColorButtonId.asStateFlow()

    private val connectionEventChannel = Channel<WebSocket.Event>()
    val connectionEvent = connectionEventChannel.receiveAsFlow().flowOn(dispatchers.io)

    private val socketEventChannel = Channel<SocketEvent>()
    val socketEvent = socketEventChannel.receiveAsFlow().flowOn(dispatchers.io)

    fun observeEvents() {
        viewModelScope.launch(dispatchers.io) {
            drawingAPI.observeEvents().collect { event ->
                connectionEventChannel.send(event)
            }
        }
    }

    fun sendBaseModel(baseModel: BaseModel) {
        viewModelScope.launch(dispatchers.io) {
            drawingAPI.sendBaseModel(baseModel)
        }
    }

    fun observeBaseModels() {
        viewModelScope.launch(dispatchers.io) {
            drawingAPI.observeBaseModels().collect { data ->
                when(data) {
                    is DrawData -> {
                        socketEventChannel.send(SocketEvent.DrawDataEvent(data))
                    }

                    is DrawAction -> {
                        when(data.action) {
                            ACTION_UNDO -> {
                                socketEventChannel.send(SocketEvent.UndoEvent)
                            }
                        }
                    }

                    is Ping -> {
                        sendBaseModel(Ping())
                    }
                }
            }
        }
    }


    fun checkRadioButton(colorResourceId: ColorResourceId) {
        _selectedColorButtonId.value = colorResourceId
    }

    @JvmInline
    value class ColorResourceId(@IdRes val id: Int)
}