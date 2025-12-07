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

    override fun run(): Boolean {
        return false
    }

    fun sendShakeHandPack(serverClient: ServerClient,msg:String,code:Int,needShutdown: Boolean){
        val shakeHandPack = JSONObject()
        shakeHandPack["code"] = code
        shakeHandPack["msg"] = msg
        serverClient.sendMessage(ServerSendEvent.ServerShakeHand, shakeHandPack)
        if(needShutdown){
            serverClient.shutdown(CloseReason.Codes.VIOLATED_POLICY, msg)
        }
    }

    fun botClientAllowConnect(serverClient: ServerClient) {
        val botClient = ClientManager.getBotClient()
        val botQueryPack = JSONObject()
        val serverId: String = serverClient.mServerId
        botQueryPack["serverId"] = serverId

        botClient!!.sendRequestAndAwaitResponse(BotClientSendEvent.BotQueryBindServerById, botQueryPack).thenAccept(
            Consumer { response: JSONObject? ->
                val responseHashKey = response!!.getString("hashKey")
                if (serverClient.mHashKey != responseHashKey) {
                    val msg = "客户端密钥错误"
                    sendShakeHandPack(serverClient,msg,3,true)
                    return@Consumer
                }
                if (ClientManager.isRegisteredServer(serverId)) { //顶替连接
                    val oriClient: ServerClient? = ClientManager.getServerPackageById(serverId).mServerClient
                    val msg = "serverId重复，已将本次连接顶替上一次连接..."
                    sendShakeHandPack(serverClient,msg,2,false)
                    oriClient?.shutdown(CloseReason.Codes.VIOLATED_POLICY, "顶替连接.")
                } else {
                    sendShakeHandPack(serverClient,"握手成功",1,false)
                }

                ClientManager.registerServer(serverId, serverClient)
                logger.info("服务器握手成功, ServerId: {}", serverId)
            })
    }



    fun runShake(session: ClientSession): Boolean {
        val serverId: String = mBody.getString("serverId")?: ""
        val hashKey: String = mBody.getString("hashKey")?: ""

        val platform: String = mBody.getString("platform")?: "Unknown"
        val name: String = mBody.getString("name")?: "Unknown"
        val version: String = mBody.getString("version")?: "0.0.0"

        val serverClient = ServerClient(session)

        if (serverId.isEmpty()) {
            //拒绝连接
            serverClient.shutdown(CloseReason.Codes.VIOLATED_POLICY, "serverId为空.")
            return false
        }

        serverClient.mServerId = serverId
        serverClient.mHashKey = hashKey
        serverClient.platform = platform
        serverClient.name = name
        serverClient.version = version

        //检测是否被ban
        if (BanManager.isBanned(serverId)) {
            val reason = BanManager.queryBanReason(serverId)
            val msg = "服务器被封禁，请联系机器人管理员查看详情.原因:$reason"
            sendShakeHandPack(serverClient,msg,8,true)
            return false
        }

        //记录连接次数并检测是否是频繁连接
        if(ClientManager.recordConnectionAttempt(serverId)){
            //频繁连接
            val msg = "频繁连接导致的服务器被封禁，请联系机器人管理员查看详情."
            sendShakeHandPack(serverClient,msg,8,true)
            return false
        }

        if (serverClient.mHashKey.isEmpty()) { //等待注册服务器
            ClientManager.putAbsentServer(serverId, serverClient)
            val msg = "等待绑定"
            sendShakeHandPack(serverClient,msg,6,false)
            return false
        }

        val latestVersion: String = ConfigManager.getLatestClientVersion(serverClient.platform)
        val clientVersion = serverClient.version

        // 处理开发版提示
        if ("dev" == clientVersion) {
            val msg = "您正在使用的是开发版，如有问题请在对应适配器的GitHub仓库中提出Issues"
            sendShakeHandPack(serverClient,msg,2,false)
        } else if (!isVersionAllowed(clientVersion, latestVersion)) {
            val msg =
                "插件版本过低，最新版本为:$latestVersion，您当前版本为:$clientVersion。请更新插件后重试。"
            sendShakeHandPack(serverClient,msg,4,true)
            return false
        }


        val botClient: BotClient? = ClientManager.getBotClient()

        if (botClient == null || !botClient.isActive()) {
            val msg =
                "BotClient尚未连接，已将您的请求加入等待队列，正在等待BotClient连接. 长时间未连接请联系机器人管理员."
            sendShakeHandPack(serverClient,msg,5,false)
            ClientManager.putWaitingBotClientList(serverId, serverClient)
            return false
        }

        botClientAllowConnect(serverClient)

        return true
    }
}