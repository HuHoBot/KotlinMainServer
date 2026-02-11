package cn.huohuas001.events.bot.EventHandler

import cn.huohuas001.client.ServerClient
import cn.huohuas001.events.BaseEvent
import cn.huohuas001.events.EventContext
import cn.huohuas001.events.bot.EventEnum.BotClientSendEvent
import cn.huohuas001.tools.manager.ClientManager
import cn.huohuas001.tools.pack.ServerPack
import com.alibaba.fastjson2.JSONObject
import events.Server.EventEnum.getServerSendEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Bot_handle_SendPack2Server : BaseEvent() {
    override val logger: Logger = LoggerFactory.getLogger("Bot_handle_SendPack2Server")

    override fun run(context: EventContext): Boolean {
        try {
            val serverId = context.body.getString("serverId")
            val type = context.body.getString("type")
            val data = context.body.getJSONObject("data")

            val serverPack = ClientManager.getServerPackageById(serverId)
            val serverClient = serverPack.serverClient

            val statusPack = JSONObject()
            var status = false

            if (serverClient != null) {
                try {
                    status = serverClient.sendMessage(getServerSendEvent(type), data, context.packId)
                } catch (e: IllegalArgumentException) {
                    logger.error("未知的类型: {}", type, e)
                } catch (e: Exception) {
                    logger.error("无法发送数据给服务器", e)
                }
            }

            statusPack["status"] = status
            if (context.botClient != null) {
                context.botClient?.sendMessage(BotClientSendEvent.BotCallBack, statusPack, context.packId)
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
