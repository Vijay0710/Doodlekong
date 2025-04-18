package com.plcoding.doodlekong.data.remote.ws.models

import com.plcoding.doodlekong.utils.Constants.TYPE_CURRENT_ROUND_DRAW_INFO

data class RoundDrawInfo(
    val data: List<String>
): BaseModel(TYPE_CURRENT_ROUND_DRAW_INFO)
