package cn.huohuas001.client

import io.ktor.websocket.WebSocketSession

class ClientSession(session: WebSocketSession,ip: String) {
    val mSession: WebSocketSession;
    val mIp: String;

    init {
        mSession = session
        mIp = ip
    }
}