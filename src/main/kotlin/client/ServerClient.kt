package cn.huohuas001.client

import cn.huohuas001.tools.getPackID
import com.alibaba.fastjson2.JSONObject
import events.Server.EventEnum.ServerSendEvent
import org.slf4j.Logger
import io.ktor.websocket.CloseReason
import io.ktor.websocket.WebSocketSession
import org.slf4j.LoggerFactory

class ServerClient(
    override var mSession: ClientSession
) : BaseClient(mSession, ClientType.Server) {
    override var mClientType: ClientType = ClientType.Server

    override val logger: Logger = LoggerFactory.getLogger("ServerClient")
    var platform: String = "Unknown"
    var name: String = "Server"
    var version: String = "0.0.0"

    fun sendMessage(type: ServerSendEvent, body: JSONObject): Boolean {
        val packId = getPackID()
        return sendMessage(type, body, packId)
    }

    fun sendMessage(type: ServerSendEvent, body: JSONObject, packId: String): Boolean {
        return baseSendMessage(type.eventName, body, packId)
    }

    /**
     * 关闭连接
     */
    fun shutdown(code: CloseReason.Codes, reason: String) {
        val body = JSONObject()
        body["msg"] = reason
        sendMessage(ServerSendEvent.ServerShutdown, body)

        // 使用协程关闭连接
        close(code, reason)
    }
}
