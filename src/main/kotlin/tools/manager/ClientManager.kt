package cn.huohuas001.tools.manager

import cn.huohuas001.client.BotClient
import cn.huohuas001.client.ClientSession
import cn.huohuas001.client.ServerClient
import io.ktor.websocket.CloseReason
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import cn.huohuas001.events.server.EventHandler.Server_handle_ShakeHand
import cn.huohuas001.tools.pack.ServerPack


object ClientManager {
    private val logger = LoggerFactory.getLogger("ClientManager")
    private val mSendLock: Object = Object()
    // 注册的服务器
    private val registeredServers: MutableMap<String, ServerClient> = ConcurrentHashMap<String, ServerClient>()
    // 未注册的服务器
    private val absentRegisteredServers: MutableMap<String, ServerClient> = ConcurrentHashMap<String, ServerClient>()
    //等待BotClient连接队列
    private val waitingBotClientList: MutableMap<String, ServerClient> = ConcurrentHashMap<String, ServerClient>()
    //心跳超时时间
    private val HeartbeatTimeOut: Long = 60*1000

    private var mBotClient: BotClient? = null
    //心跳检测协程
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        // 每隔5秒检查一次心跳，替代原来的 scheduler.scheduleAtFixedRate
        coroutineScope.launch {
            while (isActive) {
                delay(15*1000) // 5秒延迟
                checkHeartbeats()
            }
        }
    }

    /**
     * 注册服务器
     * @param serverId 服务器id
     * @param serverClient 服务器对象
     * @return 是否注册成功
     */
    fun registerServer(serverId: String?, serverClient: ServerClient?): Boolean {
        registeredServers[serverId!!] = serverClient!!
        return true
    }

    /**
     * 注销服务器
     * @param serverId 服务器id
     * @return 是否注销成功
     */
    fun unRegisterServer(serverId: String?): Boolean {
        if (registeredServers.containsKey(serverId)) {
            registeredServers.remove(serverId)
            return true
        }
        return false
    }

    /**
     * 判断服务器是否已注册
     * @param serverId 服务器id
     * @return 是否注册
     */
    fun isRegisteredServer(serverId: String?): Boolean {
        return registeredServers.containsKey(serverId)
    }

    /**
     * 判断服务器是否已握手
     *
     * @param serverId 服务器id
     * @return 是否已握手
     */
    fun isShakeHand(serverId: String): Boolean {
        return registeredServers.containsKey(serverId) || absentRegisteredServers.containsKey(serverId)
    }

    /**
     * 添加未注册服务器
     */
    fun putAbsentServer(serverId: String, serverClient: ServerClient) {
        logger.info("已将服务器 ${serverId} 加入未注册服务器列表")
        absentRegisteredServers[serverId] = serverClient
    }

    /**
     * 添加等待BotClient连接队列
     */
    fun putWaitingBotClientList(serverId: String, serverClient: ServerClient) {
        waitingBotClientList[serverId] = serverClient
    }

    /**
     * 移除等待BotClient连接队列
     */
    fun removeWaitingBotClientList(serverId: String?) {
        waitingBotClientList.remove(serverId)
    }

    /**
     * 重新握手等待的BotClient
     */
    fun reShakeWaitingServer() {
        HashMap<String?, ServerClient?>(waitingBotClientList).forEach { (serverId: String?, serverClient: ServerClient?) ->
            serverClient?.let { Server_handle_ShakeHand.botClientAllowConnect(it) }
            removeWaitingBotClientList(serverId)
        }
    }

    /**
     * 获取服务器对象
     * @param serverId 服务器id
     * @return 服务器对象包
     */
    fun getServerPackageById(serverId: String): ServerPack? {
        if (registeredServers.containsKey(serverId)) {
            return ServerPack(serverId, registeredServers.get(serverId))
        }
        if (absentRegisteredServers.containsKey(serverId)) {
            return ServerPack(serverId, absentRegisteredServers.get(serverId))
        }
        return ServerPack("", null)
    }

    fun getServerPackageBySession(session: ClientSession?): ServerPack? {
        val serverPack: AtomicReference<ServerPack?> = AtomicReference<ServerPack?>()
        registeredServers.forEach { (serverId: String, serverInfo: ServerClient) ->
            if (serverInfo.mSession == session) {
                serverPack.set(ServerPack(serverId, serverInfo))
            }
        }
        if (serverPack.get() == null) {
            absentRegisteredServers.forEach { (serverId: String, serverInfo: ServerClient) ->
                if (serverInfo.mSession == session) {
                    serverPack.set(ServerPack(serverId, serverInfo))
                }
            }
        }
        if (serverPack.get() == null) {
            return ServerPack("", null)
        }
        return serverPack.get()
    }

    fun queryOnlineClient(serverId: String): String? {
        val serverPack: ServerPack? = getServerPackageById(serverId)
        if (serverPack != null) {
            if (serverPack.mServerClient != null) {
                return serverPack.mServerClient.name
            }
            return ""
        }
        return ""
    }

    /**
     * 获取在线客户端数量
     */
    fun queryOnlineClientCount(): Int {
        return registeredServers.size
    }

    fun shutDownClient(serverId: String?): Boolean {
        if (registeredServers.containsKey(serverId)) {
            val serverClient: ServerClient = registeredServers.get(serverId)!!
            serverClient.shutdown(CloseReason.Codes.NORMAL, "Server shutdown.")
            return true
        } else {
            return false
        }
    }

    fun setBotClient(botClient: BotClient) {
        mBotClient = botClient
    }

    fun getBotClient(): BotClient? {
        return mBotClient
    }

    /**
     * 获取发送锁
     */
    fun getSendLock(): Object {
        return mSendLock
    }

    /**
     * 检查心跳
     */
    private fun checkHeartbeats() {
        checkHeartbeatsForMap(registeredServers)
        //checkHeartbeatsForMap(absentRegisteredServers)
    }

    /**
     * 检查心跳
     */
    private fun checkHeartbeatsForMap(clientMap: MutableMap<String, ServerClient>) {
        clientMap.values.removeIf { client: ServerClient ->
            if (client.isTimeout(HeartbeatTimeOut)) {
                logger.info(
                    "[ClientManager]  客户端({})心跳超时，自动断开, ServerId: {}",
                    client.getRemoteAddress(),
                    client.mServerId
                )
                client.close(CloseReason.Codes.NORMAL, "Timeout.")
                return@removeIf true // 从 Map 中移除
            }
            false
        }
    }


}