package com.plcoding.doodlekong.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plcoding.doodlekong.data.remote.ws.Room
import com.plcoding.doodlekong.repository.SetUpRepository
import com.plcoding.doodlekong.utils.Constants.MAX_ROOM_NAME_LENGTH
import com.plcoding.doodlekong.utils.Constants.MAX_USERNAME_LENGTH
import com.plcoding.doodlekong.utils.Constants.MIN_ROOM_NAME_LENGTH
import com.plcoding.doodlekong.utils.Constants.MIN_USERNAME_LENGTH
import com.plcoding.doodlekong.utils.DispatcherProvider
import com.plcoding.doodlekong.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CreateRoomViewModel @Inject constructor(
    private val setUpRepository: SetUpRepository,
    private val dispatcherProvider: DispatcherProvider
) : ViewModel() {


    sealed class SetUpEvent {
        data object InputEmptyError: SetUpEvent()
        data object InputTooShortError : SetUpEvent()
        data object InputTooLongError : SetUpEvent()

        data class CreateRoomEvent(val room: Room) : SetUpEvent()
        data class CreateRoomErrorEvent(val error: String) : SetUpEvent()
        data object GetRoomEmptyEvent : SetUpEvent()

        data class JoinRoomEvent(val roomName: String) : SetUpEvent()
        data class JoinRoomErrorEvent(val error: String) : SetUpEvent()
    }

    private val _setUpEvent = MutableSharedFlow<SetUpEvent>()
    val setUpEvent = _setUpEvent.asSharedFlow()


    fun createRoom(room: Room) {
        viewModelScope.launch(dispatcherProvider.main) {
            val trimmedRoom = room.name.trim()

            when {
                trimmedRoom.isEmpty() -> _setUpEvent.emit(SetUpEvent.InputEmptyError)
                trimmedRoom.length < MIN_ROOM_NAME_LENGTH -> _setUpEvent.emit(SetUpEvent.InputTooShortError)
                trimmedRoom.length > MAX_ROOM_NAME_LENGTH -> _setUpEvent.emit(SetUpEvent.InputTooLongError)
                else -> {
                    when (val result = setUpRepository.createRoom(room)) {
                        is Resource.Error -> {
                            _setUpEvent.emit(
                                SetUpEvent.CreateRoomErrorEvent(
                                    error = result.errorMessage ?: return@launch
                                )
                            )
                        }

                        is Resource.Success -> _setUpEvent.emit(SetUpEvent.CreateRoomEvent(room))
                    }
                }
            }
        }
    }


    fun joinRoom(username: String, roomName: String) {
        viewModelScope.launch(dispatcherProvider.main) {

            val result = withContext(dispatcherProvider.io) {
                setUpRepository.joinRoom(username, roomName)
            }

            when (result) {
                is Resource.Error -> {
                    _setUpEvent.emit(
                        SetUpEvent.JoinRoomErrorEvent(
                            result.errorMessage ?: return@launch
                        )
                    )
                }

                is Resource.Success -> _setUpEvent.emit(
                    SetUpEvent.JoinRoomEvent(
                        roomName
                    )
                )
            }
        }
    }
}