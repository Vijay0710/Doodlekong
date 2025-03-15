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
class UsernameViewModel @Inject constructor(
    private val dispatcherProvider: DispatcherProvider
) : ViewModel() {


    sealed class SetUpEvent {
        data object InputEmptyError : SetUpEvent()
        data object InputTooShortError : SetUpEvent()
        data object InputTooLongError : SetUpEvent()

        data class NavigateToSelectRoomEvent(val username: String) : SetUpEvent()
    }

    private val _setUpEvent = MutableSharedFlow<SetUpEvent>()
    val setUpEvent = _setUpEvent.asSharedFlow()


    fun validateUsernameAndNavigateToSelectRoom(username: String) {
        viewModelScope.launch(dispatcherProvider.main) {
            val trimmedUsername = username.trim()
            when {
                trimmedUsername.isEmpty() -> {
                    _setUpEvent.emit(SetUpEvent.InputEmptyError)
                }

                trimmedUsername.length < MIN_USERNAME_LENGTH -> {
                    _setUpEvent.emit(SetUpEvent.InputTooShortError)
                }

                trimmedUsername.length > MAX_USERNAME_LENGTH -> {
                    _setUpEvent.emit(SetUpEvent.InputTooLongError)
                }

                else -> {
                    _setUpEvent.emit(SetUpEvent.NavigateToSelectRoomEvent(trimmedUsername))
                }
            }
        }
    }
}