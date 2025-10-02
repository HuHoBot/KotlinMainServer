package cn.huohuas001

/*import cn.huohuas001.client.BotClient
import cn.huohuas001.client.ServerClient*/
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
import kotlin.collections.LinkedHashMap

object WebsocketServer {
    //Logger日志
    private val logger = LoggerFactory.getLogger("Server")
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    //回调消息队列
    private val responseFutureList: MutableMap<String, CompletableFuture<JSONObject>> =
        object : LinkedHashMap<String, CompletableFuture<JSONObject>>(1000, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<String, CompletableFuture<JSONObject>>?): Boolean {
                return size > 100 // 超出容量自动移除最旧条目
            }
        }

    // 管理活动连接
    private val activeConnections: MutableMap<String, ClientSession> = ConcurrentHashMap(512)
    private val serverEventMapping = mutableMapOf<ServerRecvEvent, BaseEvent>()
    private val botEventMapping = mutableMapOf<BotClientRecvEvent, BaseEvent>()

    //初始化注册事件
    init {
        try {
            // 现有的初始化代码
            // Server Event
            registerServerProcess(ServerRecvEvent.ServerHeartEvent, Server_handle_Heart)
            registerServerProcess(ServerRecvEvent.ServerReplySuccessEvent, Server_handle_ResponeMsg)
            registerServerProcess(ServerRecvEvent.ServerReplyErrorEvent, Server_handle_ResponeMsg)
            registerServerProcess(ServerRecvEvent.ServerQueryWhiteListEvent, Server_handle_ResponeWhiteList)
            registerServerProcess(ServerRecvEvent.ServerQueryOnlineEvent, Server_handle_ResponeOnlineList)
            registerServerProcess(ServerRecvEvent.ServerBindConfirmEvent, Server_handle_BindConfirm)
            registerServerProcess(ServerRecvEvent.ServerChatEvent, Server_handle_Chat)

            //Bot Event
            registerBotProcess(BotClientRecvEvent.BotSendServerMsgEvent, Bot_handle_SendPack2Server)
            registerBotProcess(BotClientRecvEvent.BotQueryClientListEvent, Bot_handle_QueryClientList)
            registerBotProcess(BotClientRecvEvent.BotHeartEvent, Bot_handle_Heart)
        } catch (e: Exception) {
            logger.error("WebsocketServer initialization failed", e)
            throw e
        }
    }

    /**
     * 处理连接建立
     * @param session 客户端
     * @param sessionId 会话Id
     */
    fun handleConnectionEstablished(session: ClientSession, sessionId: String) {
        activeConnections[sessionId] = session

        // 添加10秒握手超时检测
        coroutineScope.launch {
            delay(10000)
            try {
                val serverPackage = ClientManager.getServerPackageBySession(session)
                val botClient = ClientManager.getBotClient()

                if (serverPackage == null || !ClientManager.isShakeHand(serverPackage.mServerId)) {
                    if (botClient != null && botClient.mSession != session) {
                        session.mSession.close(CloseReason(CloseReason.Codes.NORMAL, "握手超时"))
                    }
                }
            } catch (e: IOException) {
                logger.error("[Websocket] 关闭超时连接时出错", e)
            }
        }
    }

    /**
     * 处理连接关闭
     * @param session 断开的连接
     * @param reason 断开原因
     */
    fun handleConnectionClosed(session: ClientSession, reason: CloseReason?) {
        val sessionId = getSessionId(session.mSession)
        activeConnections.remove(sessionId)
        val serverPackage = ClientManager.getServerPackageBySession(session)
        if (serverPackage != null) {
            if (ClientManager.isRegisteredServer(serverPackage.mServerId)) {
                logger.info("[Websocket] 客户端断开连接, ServerId: {}", serverPackage.mServerId)
            }
            ClientManager.unRegisterServer(serverPackage.mServerId)
        }
    }

    fun handleError(session: WebSocketSession, error: Exception) {
        logger.error("[Websocket] 处理消息时发生错误", error)
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
        val packId = messagePack.mPackId
        val msgType = messagePack.mType
        val body = messagePack.mBody

        val eventType = getServerRecvEvent(msgType)
        if (eventType == ServerRecvEvent.ServerShakeHandEvent) {
            val msgPack = ActionPack(eventType, body, packId, null)
            val handler = Server_handle_ShakeHand
            handler.setData(msgPack)
            handler.runShake(session)
            return
        }

        val event = serverEventMapping[eventType]
        if (event != null) {
            val serverPackage = ClientManager.getServerPackageBySession(session)
            val client = serverPackage?.mServerClient
            val botClient = ClientManager.getBotClient()
            if (botClient == null) {
                client?.shutdown(CloseReason.Codes.CANNOT_ACCEPT, "BotClient连接出现问题，请联系机器人管理员")
                return
            }

            if (client == null && botClient != session) {
                try {
                    coroutineScope.launch {
                        session.mSession.close(CloseReason(CloseReason.Codes.NORMAL, "无效的客户端连接"))
                    }
                } catch (e: IOException) {
                    logger.error("[Websocket] 处理无效客户端连接时发生错误:", e)
                }
                return
            }
            val msgPack = ActionPack(eventType, body, packId, client)
            event.eventCall(msgPack)
        } else {
            logger.error("[Websocket] 未找到Server处理程序: {}", msgType)
        }
    }

    private fun handleBotMessage(session: ClientSession, messagePack: MessagePack) {
        val packId = messagePack.mPackId
        val msgType = messagePack.mType
        val body = messagePack.mBody

        val eventType = getBotRecvEvent(msgType)
        if (eventType == BotClientRecvEvent.BotShakeHandEvent) {
            val msgPack = ActionPack(eventType, body, packId, null)
            val shakeHandEvent = Bot_handle_ShakeHand
            shakeHandEvent.setData(msgPack)
            shakeHandEvent.runShake(session)
            return
        }
        val event = botEventMapping[eventType]
        if (event != null) {
            val msgPack = ActionPack(eventType, body, packId, ClientManager.getBotClient())
            event.eventCall(msgPack)
        } else {
            logger.error("[Websocket] 未找到Bot处理程序: {}", msgType)
        }
    }

    private fun checkPackLegal(data: JSONObject){

    }

    fun handleTextMessage(session: ClientSession, payload: String) {
        try {
            val data = JSONObject.parseObject(payload)
            val header = data.getJSONObject("header")
            var body = data.getJSONObject("body")
            if(body ==  null){
                body = JSONObject()
            }
            val msgType = header.getString("type")
            val packId = header.getString("id")

            if(msgType == null || packId == null){
                logger.error("[Websocket] 收到无效的封包: {}", payload)
                try {
                    coroutineScope.launch {
                        session.mSession.close(CloseReason(CloseReason.Codes.PROTOCOL_ERROR, "无效的封包"))
                    }
                } catch (e: IOException) {
                    logger.error("[Websocket] 处理无效封包时发生错误:", e)
                }
                return
            }

            //封包
            val messagePack = MessagePack(msgType, body, packId)

            //执行回调消息
            if (responseFutureList.containsKey(packId)) {
                logger.debug("[Websocket] 收到response消息: {}", payload)
                logger.debug("处理事件回调 {}", packId)
                val responseFuture = responseFutureList[packId]
                if (responseFuture != null && !responseFuture.isDone) {
                    responseFuture.complete(body)
                }
                responseFutureList.remove(packId)
                return
            }

            //判断是否是Bot发过来的消息
            if (msgType.startsWith("BotClient.")) {
                handleBotMessage(session, messagePack)
            } else {
                handleServerMessage(session, messagePack)
            }
        } catch (e: Exception) {
            logger.error("[Websocket] 处理消息时发生错误", e)
        }
    }

    private fun getSessionId(session: WebSocketSession): String {
        // 实现获取会话ID的逻辑
        return session.hashCode().toString()
    }
}