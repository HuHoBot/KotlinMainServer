package cn.huohuas001.client

import cn.huohuas001.WebsocketServer
import cn.huohuas001.events.bot.EventEnum.BotClientSendEvent
import cn.huohuas001.tools.getPackID
import com.alibaba.fastjson2.JSONObject
import io.ktor.websocket.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture

class BotClient(
    override var session: ClientSession
) : BaseClient(session, ClientType.Bot) {
    override var clientType: ClientType = ClientType.Bot
    override val logger: Logger = LoggerFactory.getLogger("BotClient")

    fun sendMessage(type: BotClientSendEvent, body: JSONObject): Boolean {
        val packId = getPackID()
        return sendMessage(type, body, packId)
    }

    fun sendMessage(type: BotClientSendEvent, body: JSONObject, packId: String): Boolean {
        return baseSendMessage(type.eventName, body, packId)
    }

    fun sendRequestAndAwaitResponse(type: BotClientSendEvent, body: JSONObject): CompletableFuture<JSONObject> {
        val packId = getPackID()
        return sendRequestAndAwaitResponse(type, body, packId)
    }

    fun sendRequestAndAwaitResponse(
        type: BotClientSendEvent,
        body: JSONObject,
        packId: String
    ): CompletableFuture<JSONObject> {
        val responseFuture = CompletableFuture<JSONObject>()
        WebsocketServer.addCallback(packId, responseFuture)
        baseSendMessage(type.eventName, body, packId)
        return responseFuture
    }

    fun textCallBack(msg: String, callbackConvert: Int = 0, packId: String) {
        val body = JSONObject()
        val packedMsg = JSONObject()
        packedMsg["text"] = msg
        packedMsg["callbackConvert"] = callbackConvert
        body["param"] = packedMsg
        sendMessage(BotClientSendEvent.BotCallbackFunc, body, packId)
    }

    fun jsonCallBack(msg: JSONObject, packId: String) {
        val body = JSONObject()
        body["param"] = msg
        sendMessage(BotClientSendEvent.BotCallbackFunc, body, packId)
    }

    fun shutdown(code: CloseReason.Codes, reason: String) {
        val body = JSONObject()
        body["msg"] = reason
        sendMessage(BotClientSendEvent.BotShutdown, body)
        close(code, reason)
    }
}
