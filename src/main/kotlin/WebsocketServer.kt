package cn.huohuas001

import cn.huohuas001.client.ClientSession
import cn.huohuas001.events.BaseEvent
import cn.huohuas001.events.bot.EventEnum.BotClientRecvEvent
import cn.huohuas001.events.bot.EventEnum.getBotRecvEvent
import cn.huohuas001.events.bot.EventHandler.Bot_handle_Heart
import cn.huohuas001.events.bot.EventHandler.Bot_handle_QueryClientList
import cn.huohuas001.events.bot.EventHandler.Bot_handle_SendPack2Server
import cn.huohuas001.events.server.EventEnum.ServerRecvEvent
import cn.huohuas001.events.server.EventEnum.getServerRecvEvent
import cn.huohuas001.events.server.EventHandler.Server_handle_BindConfirm
import cn.huohuas001.events.server.EventHandler.Server_handle_Chat
import cn.huohuas001.events.server.EventHandler.Server_handle_Heart
import cn.huohuas001.events.server.EventHandler.Server_handle_ResponeMsg
import cn.huohuas001.events.server.EventHandler.Server_handle_ResponeOnlineList
import cn.huohuas001.events.server.EventHandler.Server_handle_ResponeWhiteList
import cn.huohuas001.events.server.EventHandler.Server_handle_ShakeHand
import cn.huohuas001.tools.manager.ClientManager
import cn.huohuas001.tools.pack.ActionPack
import cn.huohuas001.tools.pack.MessagePack
import com.alibaba.fastjson2.JSONObject
import events.bot.Bot_handle_ShakeHand
import io.ktor.websocket.*
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

object WebsocketServer {
    private val logger = LoggerFactory.getLogger("Server")
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val responseFutureList: MutableMap<String, CompletableFuture<JSONObject>> =
        object : LinkedHashMap<String, CompletableFuture<JSONObject>>(1000, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<String, CompletableFuture<JSONObject>>?): Boolean {
                return size > 100
            }
        }

    private val activeConnections: MutableMap<String, ClientSession> = ConcurrentHashMap(512)
    private val serverEventMapping = mutableMapOf<ServerRecvEvent, BaseEvent>()
    private val botEventMapping = mutableMapOf<BotClientRecvEvent, BaseEvent>()

    init {
        try {
            registerServerProcess(ServerRecvEvent.ServerHeartEvent, Server_handle_Heart)
            registerServerProcess(ServerRecvEvent.ServerReplySuccessEvent, Server_handle_ResponeMsg)
            registerServerProcess(ServerRecvEvent.ServerReplyErrorEvent, Server_handle_ResponeMsg)
            registerServerProcess(ServerRecvEvent.ServerQueryWhiteListEvent, Server_handle_ResponeWhiteList)
            registerServerProcess(ServerRecvEvent.ServerQueryOnlineEvent, Server_handle_ResponeOnlineList)
            registerServerProcess(ServerRecvEvent.ServerBindConfirmEvent, Server_handle_BindConfirm)
            registerServerProcess(ServerRecvEvent.ServerChatEvent, Server_handle_Chat)

            registerBotProcess(BotClientRecvEvent.BotSendServerMsgEvent, Bot_handle_SendPack2Server)
            registerBotProcess(BotClientRecvEvent.BotQueryClientListEvent, Bot_handle_QueryClientList)
            registerBotProcess(BotClientRecvEvent.BotHeartEvent, Bot_handle_Heart)
        } catch (e: Exception) {
            logger.error("WebsocketServer initialization failed", e)
            throw e
        }
    }

    fun handleConnectionEstablished(session: ClientSession, sessionId: String) {
        activeConnections[sessionId] = session

        coroutineScope.launch {
            delay(15 * 1000)
            try {
                val serverPackage = ClientManager.getServerPackageBySession(session)
                val botClient = ClientManager.getBotClient()

                if (serverPackage.serverClient == null || !ClientManager.isShakeHand(serverPackage.serverId)) {
                    if (botClient != null && botClient.session != session) {
                        session.session.close(CloseReason(CloseReason.Codes.NORMAL, "握手超时"))
                    }
                }
            } catch (e: IOException) {
                logger.error("关闭超时连接时出错", e)
            }
        }
    }

    fun handleConnectionClosed(session: ClientSession, reason: CloseReason?) {
        val sessionId = getSessionId(session.session)
        activeConnections.remove(sessionId)
        val serverPackage = ClientManager.getServerPackageBySession(session)
        if (serverPackage.serverClient != null) {
            if (ClientManager.isRegisteredServer(serverPackage.serverId)) {
                logger.info("客户端断开连接, ServerId: {}", serverPackage.serverId)
            }
            ClientManager.unRegisterServer(serverPackage.serverId)
        }
    }

    fun handleError(session: ClientSession, error: Exception) {
        coroutineScope.launch {
            when (error) {
                is IOException -> {
                    if (error.message?.contains("Ping timeout") == true) {
                        try {
                            session.session.close(CloseReason(CloseReason.Codes.GOING_AWAY, "Ping timeout detected"))
                        } catch (e: Exception) {
                            logger.warn("关闭Ping超时连接时发生错误", e)
                        }
                    } else {
                        session.session.close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, "IO Error: ${error.message}"))
                    }
                }
                else -> {
                    session.session.close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, "Error Message."))
                }
            }
        }
        if (error.message?.contains("Ping timeout") == false) {
            val clientId = ClientManager.getServerPackageBySession(session).serverId.ifEmpty { "Unknown Server" }
            logger.error("$clientId 处理消息时发生错误", error)
        }
    }

    fun addCallback(id: String, callback: CompletableFuture<JSONObject>) {
        responseFutureList[id] = callback
    }

    private fun registerServerProcess(eventType: ServerRecvEvent, event: BaseEvent) {
        serverEventMapping[eventType] = event
    }

    private fun registerBotProcess(eventType: BotClientRecvEvent, event: BaseEvent) {
        botEventMapping[eventType] = event
    }

    private fun handleServerMessage(session: ClientSession, messagePack: MessagePack) {
        val packId = messagePack.packId
        val msgType = messagePack.type
        val body = messagePack.body

        val eventType = getServerRecvEvent(msgType)
        if (eventType == ServerRecvEvent.ServerShakeHandEvent) {
            val msgPack = ActionPack(eventType, body, packId, null)
            Server_handle_ShakeHand.runShake(session, msgPack)
            return
        }

        val event = serverEventMapping[eventType]
        if (event != null) {
            val serverPackage = ClientManager.getServerPackageBySession(session)
            val client = serverPackage.serverClient
            val botClient = ClientManager.getBotClient()
            if (botClient == null) {
                client?.shutdown(CloseReason.Codes.CANNOT_ACCEPT, "BotClient连接出现问题，请联系机器人管理员")
                return
            }

            if (client == null && botClient.session != session) {
                try {
                    coroutineScope.launch {
                        session.session.close(CloseReason(CloseReason.Codes.NORMAL, "无效的客户端连接"))
                    }
                } catch (e: IOException) {
                    logger.error("处理无效客户端连接时发生错误:", e)
                }
                return
            }
            val msgPack = ActionPack(eventType, body, packId, client)
            event.eventCall(msgPack)
        } else {
            logger.error("未找到Server处理程序: {}", msgType)
        }
    }

    private fun handleBotMessage(session: ClientSession, messagePack: MessagePack) {
        val packId = messagePack.packId
        val msgType = messagePack.type
        val body = messagePack.body

        val eventType = getBotRecvEvent(msgType)
        if (eventType == BotClientRecvEvent.BotShakeHandEvent) {
            val msgPack = ActionPack(eventType, body, packId, null)
            Bot_handle_ShakeHand.runShake(session, msgPack)
            return
        }
        val event = botEventMapping[eventType]
        if (event != null) {
            val msgPack = ActionPack(eventType, body, packId, ClientManager.getBotClient())
            event.eventCall(msgPack)
        } else {
            logger.error("未找到Bot处理程序: {}", msgType)
        }
    }

    fun handleTextMessage(session: ClientSession, payload: String) {
        try {
            val data = JSONObject.parseObject(payload)
            val header = data.getJSONObject("header")
            val body = data.getJSONObject("body") ?: JSONObject()
            val msgType = header.getString("type")
            val packId = header.getString("id")

            if (msgType == null || packId == null) {
                logger.error("收到无效的封包: {}", payload)
                try {
                    coroutineScope.launch {
                        session.session.close(CloseReason(CloseReason.Codes.PROTOCOL_ERROR, "无效的封包"))
                    }
                } catch (e: IOException) {
                    logger.error("处理无效封包时发生错误:", e)
                }
                return
            }

            val messagePack = MessagePack(msgType, body, packId)

            if (responseFutureList.containsKey(packId)) {
                logger.debug("收到response消息: {}", payload)
                logger.debug("处理事件回调 {}", packId)
                val responseFuture = responseFutureList[packId]
                if (responseFuture != null && !responseFuture.isDone) {
                    responseFuture.complete(body)
                }
                responseFutureList.remove(packId)
                return
            }

            if (msgType.startsWith("BotClient.")) {
                handleBotMessage(session, messagePack)
            } else {
                handleServerMessage(session, messagePack)
            }
        } catch (e: Exception) {
            logger.error("处理消息时发生错误", e)
        }
    }

    private fun getSessionId(session: WebSocketSession): String {
        return session.hashCode().toString()
    }
}
