package cn.huohuas001.events.server.EventHandler

import cn.huohuas001.client.ClientSession
import cn.huohuas001.client.ServerClient
import cn.huohuas001.events.BaseEvent
import cn.huohuas001.events.EventContext
import cn.huohuas001.events.bot.EventEnum.BotClientSendEvent
import cn.huohuas001.tools.manager.BanManager
import cn.huohuas001.tools.manager.ClientManager
import cn.huohuas001.tools.manager.ConfigManager
import cn.huohuas001.tools.isVersionAllowed
import cn.huohuas001.tools.pack.ActionPack
import com.alibaba.fastjson2.JSONObject
import events.Server.EventEnum.ServerSendEvent
import io.ktor.websocket.CloseReason
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.Consumer

object Server_handle_ShakeHand : BaseEvent() {
    override val logger: Logger = LoggerFactory.getLogger("Server_handle_ShakeHand")

    override fun run(context: EventContext): Boolean {
        return false
    }

    private fun sendShakeHandPack(serverClient: ServerClient, msg: String, code: Int, needShutdown: Boolean) {
        val shakeHandPack = JSONObject().apply {
            put("code", code)
            put("msg", msg)
        }
        serverClient.sendMessage(ServerSendEvent.ServerShakeHand, shakeHandPack)
        if (needShutdown) {
            serverClient.shutdown(CloseReason.Codes.VIOLATED_POLICY, msg)
        }
    }

    fun botClientAllowConnect(serverClient: ServerClient) {
        val botClient = ClientManager.getBotClient() ?: return
        val serverId = serverClient.serverId
        val botQueryPack = JSONObject().apply {
            put("serverId", serverId)
        }

        botClient.sendRequestAndAwaitResponse(BotClientSendEvent.BotQueryBindServerById, botQueryPack)
            .thenAccept(Consumer { response ->
                val responseHashKey = response?.getString("hashKey")
                if (serverClient.hashKey != responseHashKey) {
                    sendShakeHandPack(serverClient, "客户端密钥错误", 3, true)
                    return@Consumer
                }
                if (ClientManager.isRegisteredServer(serverId)) {
                    val oriClient = ClientManager.getServerPackageById(serverId).serverClient
                    sendShakeHandPack(serverClient, "serverId重复，已将本次连接顶替上一次连接...", 2, false)
                    oriClient?.shutdown(CloseReason.Codes.VIOLATED_POLICY, "顶替连接.")
                } else {
                    sendShakeHandPack(serverClient, "握手成功", 1, false)
                }

                ClientManager.registerServer(serverId, serverClient)
                logger.info("服务器握手成功, ServerId: {}", serverId)
            })
    }

    fun runShake(session: ClientSession, msgPack: ActionPack): Boolean {
        val body = msgPack.body
        val serverId = body.getString("serverId") ?: ""
        val hashKey = body.getString("hashKey") ?: ""
        val platform = body.getString("platform") ?: "Unknown"
        val name = body.getString("name") ?: "Unknown"
        val version = body.getString("version") ?: "0.0.0"

        val serverClient = ServerClient(session)

        if (serverId.isEmpty()) {
            serverClient.shutdown(CloseReason.Codes.VIOLATED_POLICY, "serverId为空.")
            return false
        }

        serverClient.serverId = serverId
        serverClient.hashKey = hashKey
        serverClient.platform = platform
        serverClient.name = name
        serverClient.version = version

        if (BanManager.isBanned(serverId)) {
            val reason = BanManager.queryBanReason(serverId)
            sendShakeHandPack(serverClient, "服务器被封禁，请联系机器人管理员查看详情.原因:$reason", 8, true)
            return false
        }

        if (ClientManager.recordConnectionAttempt(serverId)) {
            sendShakeHandPack(serverClient, "频繁连接导致的服务器被封禁，请联系机器人管理员查看详情.", 8, true)
            return false
        }

        if (serverClient.hashKey.isEmpty()) {
            ClientManager.putAbsentServer(serverId, serverClient)
            sendShakeHandPack(serverClient, "等待绑定", 6, false)
            return false
        }

        val latestVersion = ConfigManager.getLatestClientVersion(serverClient.platform)
        val clientVersion = serverClient.version

        if ("dev" == clientVersion) {
            sendShakeHandPack(serverClient, "您正在使用的是开发版，如有问题请在对应适配器的GitHub仓库中提出Issues", 2, false)
        } else if (!isVersionAllowed(clientVersion, latestVersion)) {
            sendShakeHandPack(
                serverClient,
                "插件版本过低，最新版本为:$latestVersion，您当前版本为:$clientVersion。请更新插件后重试。",
                4,
                true
            )
            return false
        }

        val botClient = ClientManager.getBotClient()

        if (botClient == null || !botClient.isActive()) {
            sendShakeHandPack(
                serverClient,
                "BotClient尚未连接，已将您的请求加入等待队列，正在等待BotClient连接. 长时间未连接请联系机器人管理员.",
                5,
                false
            )
            ClientManager.putWaitingBotClientList(serverId, serverClient)
            return false
        }

        botClientAllowConnect(serverClient)

        return true
    }
}
