package cn.huohuas001.events.server.EventHandler

import cn.huohuas001.client.BotClient
import cn.huohuas001.client.ClientSession
import cn.huohuas001.client.ServerClient
import cn.huohuas001.events.BaseEvent
import cn.huohuas001.events.bot.EventEnum.BotClientSendEvent
import cn.huohuas001.tools.manager.BanManager
import cn.huohuas001.tools.manager.ClientManager
import cn.huohuas001.tools.manager.ConfigManager
import cn.huohuas001.tools.isVersionAllowed
import com.alibaba.fastjson2.JSONObject
import events.Server.EventEnum.ServerSendEvent
import io.ktor.websocket.CloseReason
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.Consumer

object Server_handle_ShakeHand: BaseEvent() {
    override val logger: Logger = LoggerFactory.getLogger("Server_handle_ShakeHand")

    override fun run(): Boolean? {
        return false;
    }

    fun botClientAllowConnect(serverClient: ServerClient) {
        val botClient = ClientManager.getBotClient()

        val shakeHandPack = JSONObject()
        val botQueryPack = JSONObject()
        val serverId: String = serverClient.mServerId
        botQueryPack["serverId"] = serverId

        botClient!!.sendRequestAndAwaitResponse(BotClientSendEvent.BotQueryBindServerById, botQueryPack).thenAccept(
            Consumer { response: JSONObject? ->
                val responseHashKey = response!!.getString("hashKey")
                if (serverClient.mHashKey != responseHashKey) {
                    val msg = "客户端密钥错误"
                    shakeHandPack["code"] = 3
                    shakeHandPack["msg"] = msg
                    serverClient.sendMessage(ServerSendEvent.ServerShakeHand, shakeHandPack)
                    serverClient.shutdown(CloseReason.Codes.VIOLATED_POLICY, msg)
                    return@Consumer
                }
                if (ClientManager.isRegisteredServer(serverId)) { //顶替连接
                    val oriClient: ServerClient? = ClientManager.getServerPackageById(serverId)?.mServerClient
                    val msg = "serverId重复，已将本次连接顶替上一次连接..."
                    shakeHandPack["code"] = 2
                    shakeHandPack["msg"] = msg
                    serverClient.sendMessage(ServerSendEvent.ServerShakeHand, shakeHandPack)
                    oriClient?.shutdown(CloseReason.Codes.VIOLATED_POLICY, "顶替连接.")
                } else {
                    shakeHandPack["code"] = 1
                    shakeHandPack["msg"] = ""
                    serverClient.sendMessage(ServerSendEvent.ServerShakeHand, shakeHandPack)
                }

                ClientManager.registerServer(serverId, serverClient)
                logger.info("[Websocket]  服务器握手成功, ServerId: {}", serverId)
            })
    }

    fun runShake(session: ClientSession): Boolean {
        val serverId: String? = mBody.getString("serverId")
        val hashKey: String? = mBody.getString("hashKey")

        val platform: String = mBody.getString("platform")?: "Unknown"
        val name: String = mBody.getString("name")?: "Unknown"
        val version: String = mBody.getString("version")?: "0.0.0"

        val serverClient = ServerClient(session)

        if (serverId == null || serverId.isEmpty()) {
            //拒绝连接
            serverClient.shutdown(CloseReason.Codes.VIOLATED_POLICY, "serverId为空.")
            return false
        }

        if (hashKey == null || hashKey.isEmpty()) {
            //拒绝连接
            serverClient.shutdown(CloseReason.Codes.VIOLATED_POLICY, "hashKey为空.")
            return false
        }

        serverClient.mServerId = serverId
        serverClient.mHashKey = hashKey
        serverClient.platform = platform
        serverClient.name = name
        serverClient.version = version

        val shakeHandPack = JSONObject()



        //检测是否被ban
        if (BanManager.isBanned(serverId)) {
            val msg = "服务器被封禁，请联系机器人管理员查看详情."
            shakeHandPack["code"] = 8
            shakeHandPack["msg"] = msg
            serverClient.sendMessage(ServerSendEvent.ServerShakeHand, shakeHandPack)
            serverClient.shutdown(CloseReason.Codes.VIOLATED_POLICY, msg)
            return false
        }

        if (serverClient.mHashKey.isEmpty()) { //等待注册服务器
            ClientManager.putAbsentServer(serverId, serverClient)
            val msg = "等待绑定"
            shakeHandPack["code"] = 6
            shakeHandPack["msg"] = msg
            serverClient.sendMessage(ServerSendEvent.ServerShakeHand, shakeHandPack)
            return false
        }

        val latestVersion: String = ConfigManager.getLatestClientVersion(serverClient.platform)
        val clientVersion = serverClient.version

        // 处理开发版提示
        if ("dev" == clientVersion) {
            val msg = "您正在使用的是开发版，如有问题请在对应适配器的GitHub仓库中提出Issues"
            shakeHandPack["code"] = 2
            shakeHandPack["msg"] = msg
            serverClient.sendMessage(ServerSendEvent.ServerShakeHand, shakeHandPack)
        } else if (!isVersionAllowed(clientVersion, latestVersion)) {
            val msg =
                "插件版本过低，最新版本为:$latestVersion，您当前版本为:$clientVersion。请更新插件后重试。"
            shakeHandPack["code"] = 4
            shakeHandPack["msg"] = msg
            serverClient.sendMessage(ServerSendEvent.ServerShakeHand, shakeHandPack)
            serverClient.shutdown(CloseReason.Codes.VIOLATED_POLICY, msg)
            return false
        }


        val botClient: BotClient? = ClientManager.getBotClient()

        if (botClient == null || !botClient.isActive()) {
            val msg =
                "BotClient尚未连接，已将您的请求加入等待队列，正在等待BotClient连接. 长时间未连接请联系机器人管理员."
            shakeHandPack["code"] = 5
            shakeHandPack["msg"] = msg
            serverClient.sendMessage(ServerSendEvent.ServerShakeHand, shakeHandPack)
            ClientManager.putWaitingBotClientList(serverId, serverClient)
            return false
        }

        botClientAllowConnect(serverClient)

        return true
    }
}