package com.plcoding.doodlekong.data.remote.ws.models

import com.plcoding.doodlekong.utils.Constants.TYPE_CHAT_MESSAGE

data class ChatMessage(
    val from: String,
    val roomName: String,
    val message: String,
    val timestamp: String
): BaseModel(type = TYPE_CHAT_MESSAGE)
