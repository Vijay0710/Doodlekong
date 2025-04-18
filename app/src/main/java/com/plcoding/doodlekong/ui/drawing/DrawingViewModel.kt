package com.plcoding.doodlekong.ui.drawing

import androidx.annotation.IdRes
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.plcoding.doodlekong.R
import com.plcoding.doodlekong.data.remote.ws.DrawingAPI
import com.plcoding.doodlekong.utils.DispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class DrawingViewModel @Inject constructor(
    private val drawingAPI: DrawingAPI,
    private val dispatchers: DispatcherProvider,
    private val gson: Gson
) : ViewModel() {

    private val _selectedColorButtonId = MutableStateFlow(
        ColorResourceId(R.id.rbBlack)
    )
    val selectedColorButtonId = _selectedColorButtonId.asStateFlow()


    fun checkRadioButton(colorResourceId: ColorResourceId) {
        _selectedColorButtonId.value = colorResourceId
    }

    @JvmInline
    value class ColorResourceId(@IdRes val id: Int)
}