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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


object ClientManager {
    private val logger = LoggerFactory.getLogger("ClientManager")
    private val mSendLock: Object = Object()
    // 注册的服务器
    private val registeredServers: MutableMap<String, ServerClient> = ConcurrentHashMap<String, ServerClient>()
    // 未注册的服务器
    private val absentRegisteredServers: MutableMap<String, ServerClient> = ConcurrentHashMap<String, ServerClient>()
    //等待BotClient连接队列
    private val waitingBotClientList: MutableMap<String, ServerClient> = ConcurrentHashMap<String, ServerClient>()

    // 连接/断连计数器和最后事件时间
    private val connectionAttemptCount: MutableMap<String, Int> = ConcurrentHashMap()
    private val lastConnectionAttemptTime: MutableMap<String, Long> = ConcurrentHashMap()

    //心跳超时时间
    private const val HEARTBEAT_TIME_MILLIS: Long = 60 * 1000L
    // 连接失败次数
    private const val CONNECT_FAILED_MAX_ATTEMPTS = 15
    // 断连失败清理时间
    private const val CONNECT_FAILED_TIME_WINDOW_MILLIS = 60 * 1000L // 1分钟

    private var mBotClient: BotClient? = null
    //心跳检测协程
    private val heartbeat_coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())



    init {
        // 每隔5秒检查一次心跳，替代原来的 scheduler.scheduleAtFixedRate
        heartbeat_coroutineScope.launch {
            while (isActive) {
                delay(20*1000)
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
        logger.info("已将服务器 $serverId 加入未注册服务器列表")
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
    fun getServerPackageById(serverId: String): ServerPack {
        if (registeredServers.containsKey(serverId)) {
            return ServerPack(serverId, registeredServers[serverId])
        }
        if (absentRegisteredServers.containsKey(serverId)) {
            return ServerPack(serverId, absentRegisteredServers[serverId])
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

    fun queryOnlineClient(serverId: String): String {
        val serverPack: ServerPack = getServerPackageById(serverId)
        if (serverPack.mServerClient != null) {
            return serverPack.mServerClient.name
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
            if (client.isTimeout(HEARTBEAT_TIME_MILLIS)) {
                logger.info(
                    "客户端({})心跳超时，自动断开, ServerId: {}",
                    client.getRemoteAddress(),
                    client.mServerId
                )
                client.close(CloseReason.Codes.NORMAL, "Timeout.")
                return@removeIf true // 从 Map 中移除
            }
            false
        }
    }

    /**
     * 记录连接尝试并检查是否需要封禁
     * @param serverId 服务器ID
     * @return true表示被封禁，false表示允许连接
     */
    fun recordConnectionAttempt(serverId: String): Boolean {
        val currentTime = System.currentTimeMillis()

        // 检查时间窗口是否需要重置
        val lastTime = lastConnectionAttemptTime[serverId] ?: 0L
        if (currentTime - lastTime > CONNECT_FAILED_TIME_WINDOW_MILLIS) {
            // 重置计数器
            connectionAttemptCount[serverId] = 0
        }

        // 更新计数和时间
        val currentCount = connectionAttemptCount.getOrDefault(serverId, 0) + 1
        connectionAttemptCount[serverId] = currentCount
        lastConnectionAttemptTime[serverId] = currentTime

        // 检查是否超过阈值
        if (currentCount >= CONNECT_FAILED_MAX_ATTEMPTS) {
            val unbanTime:Long = System.currentTimeMillis() + 5 * 60 * 1000L
            // 创建日期格式化器
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = Date(unbanTime)
            val formattedDate = formatter.format(date)
            BanManager.banServer(serverId,"频繁连接/断连，于 $formattedDate 解封",unbanTime)

            // 清理计数器
            connectionAttemptCount.remove(serverId)
            lastConnectionAttemptTime.remove(serverId)

            return true
        }

        return false
    }
}