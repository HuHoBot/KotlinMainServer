package cn.huohuas001.events.bot.EventHandler
import cn.huohuas001.client.ServerClient
import cn.huohuas001.events.BaseEvent
import cn.huohuas001.events.bot.EventEnum.BotClientSendEvent
import cn.huohuas001.tools.manager.ClientManager
import cn.huohuas001.tools.pack.ServerPack
import com.alibaba.fastjson2.JSONObject
import events.Server.EventEnum.getServerSendEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Bot_handle_SendPack2Server: BaseEvent() {
    override val logger: Logger = LoggerFactory.getLogger("Bot_handle_SendPack2Server")
    override fun run(): Boolean {
        try {
            // 1. 参数提取与校验
            val serverId: String = mBody.getString("serverId")
            val type: String = mBody.getString("type")
            val data: JSONObject = mBody.getJSONObject("data")

            // 2. 获取客户端实例（避免重复调用）
            val serverPack: ServerPack = ClientManager.getServerPackageById(serverId)
            val serverClient: ServerClient? = serverPack.mServerClient

            // 3. 构造响应状态包
            val statusPack = JSONObject()
            var status = false

            if (serverClient != null) {
                // 4. 发送消息并捕获可能的状态异常
                try {
                    status = serverClient.sendMessage(getServerSendEvent( type), data, mPackId)
                } catch (e: IllegalArgumentException) {
                    logger.error("未知的类型: {}", type, e)
                } catch (e: Exception) {
                    logger.error("无法发送数据给服务器", e)
                }
            }

            // 5. 统一发送回调（避免重复代码）
            statusPack["status"] = status
            if (botClient != null) {
                botClient!!.sendMessage(BotClientSendEvent.BotCallBack, statusPack, mPackId)
            } else {
                logger.warn("BotClient为空,不能回调")
            }

            return true
        } catch (e: Exception) {
            logger.error("BotClient处理回调时发生了错误", e)
            return false
        }
    }
}