package cn.huohuas001.client

import io.ktor.websocket.WebSocketSession

data class ClientSession(
    val session: WebSocketSession,
    val ip: String
)