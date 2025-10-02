package cn.huohuas001.client

import cn.huohuas001.WebsocketServer
import cn.huohuas001.events.bot.EventEnum.BotClientSendEvent
import cn.huohuas001.tools.getPackID
import com.alibaba.fastjson2.JSONObject
import io.ktor.websocket.*
import java.util.concurrent.CompletableFuture

class BotClient(
    override var mSession: ClientSession
) : BaseClient(mSession, ClientType.Bot) {
    override var mClientType: ClientType = ClientType.Bot

    /**
     * 发送消息
     * @param type 消息类型
     * @param body 消息内容
     * @return 是否发送成功
     */
    fun sendMessage(type: BotClientSendEvent, body: JSONObject): Boolean {
        val packId: String = getPackID()
        return sendMessage(type, body, packId)
    }

    /**
     * 发送消息
     * @param type 消息类型
     * @param body 消息内容
     * @param packId 包id
     * @return 是否发送成功
     */
    fun sendMessage(type: BotClientSendEvent, body: JSONObject, packId: String): Boolean {
        return baseSendMessage(type.eventName, body, packId)
    }

    /**
     * 发送请求并等待响应
     * @param type 消息类型
     * @param body 消息内容
     * @param packId 包id
     * @return 响应结果
     */
    fun sendRequestAndAwaitResponse(type: BotClientSendEvent, body: JSONObject): CompletableFuture<JSONObject> {
        val packId: String = getPackID()
        return sendRequestAndAwaitResponse(type, body, packId)
    }

    /**
     * 发送请求并等待响应
     * @param type 消息类型
     * @param body 消息内容
     * @param packId 包id
     * @return 响应结果
     */
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

    /**
     * 回调
     * @param msg 回调内容
     * @param packId 包id
     */
    fun callBack(msg: String, packId: String) {
        val body = JSONObject()
        body["param"] = msg
        sendMessage(BotClientSendEvent.BotCallbackFunc, body, packId)
    }

    /**
     * 回调
     * @param msg 回调内容
     * @param packId 包id
     */
    fun callBack(msg: JSONObject, packId: String) {
        val body = JSONObject()
        body["param"] = msg
        sendMessage(BotClientSendEvent.BotCallbackFunc, body, packId)
    }

    /**
     * 关闭连接
     */
    fun shutdown(code: CloseReason.Codes, reason: String) {
        val body = JSONObject()
        body["msg"] = reason
        sendMessage(BotClientSendEvent.BotShutdown, body)
        close(code, reason)
    }
}