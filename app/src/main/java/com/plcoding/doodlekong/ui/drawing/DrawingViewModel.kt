package com.plcoding.doodlekong.ui.drawing

import androidx.annotation.IdRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.plcoding.doodlekong.DrawingView
import com.plcoding.doodlekong.R
import com.plcoding.doodlekong.data.remote.ws.DrawingAPI
import com.plcoding.doodlekong.data.remote.ws.Room
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
import com.plcoding.doodlekong.data.remote.ws.models.PhaseChange
import com.plcoding.doodlekong.data.remote.ws.models.Ping
import com.plcoding.doodlekong.data.remote.ws.models.RoundDrawInfo
import com.plcoding.doodlekong.utils.CoroutineTimer
import com.plcoding.doodlekong.utils.DispatcherProvider
import com.tinder.scarlet.WebSocket
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Stack
import javax.inject.Inject

@HiltViewModel
class DrawingViewModel @Inject constructor(
    private val drawingAPI: DrawingAPI,
    private val dispatchers: DispatcherProvider,
    private val gson: Gson
) : ViewModel() {

    sealed class SocketEvent {
        data class ChatMessageEvent(val data: ChatMessage): SocketEvent()
        data class AnnouncementEvent(val data: Announcement): SocketEvent()
        data class GameStateEvent(val data: GameState): SocketEvent()
        data class DrawDataEvent(val data: DrawData): SocketEvent()
        data class NewWordsEvent(val data: NewWords): SocketEvent()
        data class ChosenWordEvent(val data: ChosenWord): SocketEvent()
        data class GameErrorEvent(val data: GameError): SocketEvent()
        data class RoundDrawInfoEvent(val data: RoundDrawInfo): SocketEvent()
        data object UndoEvent: SocketEvent()
    }

    init {
        observeBaseModels()
        observeEvents()
    }

    private val _pathData = MutableStateFlow(Stack<DrawingView.PathData>())
    val pathData = _pathData.asStateFlow()

    private val _newWords = MutableStateFlow(NewWords(listOf()))
    val newWords = _newWords.asStateFlow()

    private val _phase = MutableStateFlow(PhaseChange(null, 0L, null))
    val phase = _phase.asStateFlow()

    private val _gameState = MutableStateFlow(GameState("", ""))
    val gameState = _gameState.asStateFlow()

    private val _phaseTime = MutableStateFlow(0L)
    val phaseTime = _phaseTime.asStateFlow()

    private val timer = CoroutineTimer()
    private var timerJob: Job? = null

    private val _chat = MutableStateFlow<List<BaseModel>>(listOf())
    val chat = _chat.asStateFlow()

    private val _selectedColorButtonId = MutableStateFlow(
        ColorResourceId(R.id.rbBlack)
    )
    val selectedColorButtonId = _selectedColorButtonId.asStateFlow()

    private val connectionEventChannel = Channel<WebSocket.Event>()
    val connectionEvent = connectionEventChannel.receiveAsFlow().flowOn(dispatchers.io)

    private val _connectionProgressBarVisible = MutableStateFlow(true)
    val connectionProgressBarVisible: StateFlow<Boolean> = _connectionProgressBarVisible.asStateFlow()

    private val _choseWordOverlayVisible = MutableStateFlow(false)
    val chooseWordOverlayVisible = _choseWordOverlayVisible.asStateFlow()

    private val socketEventChannel = Channel<SocketEvent>()
    val socketEvent = socketEventChannel.receiveAsFlow().flowOn(dispatchers.io)

    private fun observeEvents() {
        viewModelScope.launch(dispatchers.io) {
            drawingAPI.observeEvents().collect { event ->
                connectionEventChannel.send(event)
            }
        }
    }

    fun setPathData(stack: Stack<DrawingView.PathData>) {
        _pathData.value = stack
    }

    private fun setTimer(duration: Long) {
        timerJob?.cancel()
        timerJob = timer.timeAndEmit(duration, viewModelScope) {
            _phaseTime.value = it
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
    }

    fun setChooseWordOverlayVisibility(isVisible: Boolean) {
        _choseWordOverlayVisible.value = isVisible
    }

    fun setConnectionProgressBarVisibility(isVisible: Boolean) {
         _connectionProgressBarVisible.value = isVisible
    }

    fun sendBaseModel(baseModel: BaseModel) {
        viewModelScope.launch(dispatchers.io) {
            drawingAPI.sendBaseModel(baseModel)
        }
    }

    fun chooseWord(word: String, roomName: String) {
        val chosenWord = ChosenWord(word, roomName)
        sendBaseModel(chosenWord)
    }

    fun sendChatMessage(message: ChatMessage) {
        if(message.message.trim().isEmpty()) {
            return
        }

        viewModelScope.launch(dispatchers.io) {
            drawingAPI.sendBaseModel(message)
        }
    }

    private fun observeBaseModels() {
        viewModelScope.launch(dispatchers.io) {
            drawingAPI.observeBaseModels().collect { data ->
                when(data) {
                    is DrawData -> {
                        socketEventChannel.send(SocketEvent.DrawDataEvent(data))
                    }

                    is ChatMessage -> {
                        socketEventChannel.send(SocketEvent.ChatMessageEvent(data))
                    }

                    is ChosenWord -> {
                        socketEventChannel.send(SocketEvent.ChosenWordEvent(data))
                    }

                    is Announcement -> {
                        socketEventChannel.send(SocketEvent.AnnouncementEvent(data))
                    }

                    is NewWords -> {
                        _newWords.value = data.copy()
                        Timber.tag("VIJ").d("New words is: ${_newWords.value}")
                        socketEventChannel.send(SocketEvent.NewWordsEvent(data))
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

                    is GameState -> {
                        _gameState.value = data
                        socketEventChannel.send(SocketEvent.GameStateEvent(data))
                    }

                    is PhaseChange -> {
                        data.phase?.let {
                            _phase.value = data
                        }
                        _phaseTime.value = data.time
                        if(data.phase != Room.Phase.WAITING_FOR_PLAYERS) {
                            setTimer(data.time)
                        }
                    }

                    is GameError -> {
                        socketEventChannel.send(SocketEvent.GameErrorEvent(data))
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