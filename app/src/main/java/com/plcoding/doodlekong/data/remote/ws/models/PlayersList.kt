package com.plcoding.doodlekong.data.remote.ws.models

import com.plcoding.doodlekong.utils.Constants.TYPE_PLAYERS_LIST

data class PlayersList(
    val players: List<PlayerData>
): BaseModel(TYPE_PLAYERS_LIST)
