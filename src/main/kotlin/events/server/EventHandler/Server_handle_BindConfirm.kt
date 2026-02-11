package cn.huohuas001.events.server.EventHandler

import cn.huohuas001.events.BaseEvent
import cn.huohuas001.events.EventContext
import cn.huohuas001.events.bot.EventEnum.BotClientSendEvent
import cn.huohuas001.tools.getServerConfig
import com.alibaba.fastjson2.JSONObject
import events.Server.EventEnum.ServerSendEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.Consumer

object Server_handle_BindConfirm : BaseEvent() {
    override val logger: Logger = LoggerFactory.getLogger("Server_handle_BindConfirm")

    override fun run(context: EventContext): Boolean {
        context.botClient?.sendRequestAndAwaitResponse(
            BotClientSendEvent.BotGetConfirmData,
            JSONObject(),
            context.packId
        )?.thenAccept(Consumer { response ->
            val serverTempData = response?.getJSONObject("serverTempData") ?: return@Consumer
            val serverId = serverTempData.getString("serverId") ?: return@Consumer
            val groupOpenid = serverTempData.getString("groupId")
            val author = serverTempData.getString("author")
            val isMoreGroup = !serverTempData.getBoolean("isMoreGroup")

            try {
                val config = getServerConfig(serverId, isMoreGroup)
                val client = context.client ?: return@Consumer

                if (client.sendMessage(ServerSendEvent.ServerSendConfig, config, context.packId)) {
                    val bindServerPack = JSONObject().apply {
                        put("group", groupOpenid)
                        put("serverConfig", config)
                    }
                    context.botClient?.sendMessage(BotClientSendEvent.BotBindServer, bindServerPack, context.packId)

                    val addAdminPack = JSONObject().apply {
                        put("group", groupOpenid)
                        put("author", author)
                    }
                    context.botClient?.sendMessage(BotClientSendEvent.BotAddAdmin, addAdminPack, context.packId)
                    context.botClient?.textCallBack(
                        "已向服务端下发配置文件，并添加您为机器人管理员，如有需要请使用`/管理帮助`来查看管理员命令帮助",
                        0,
                        context.packId
                    )
                } else {
                    context.botClient?.textCallBack(
                        "无法向Id为$serverId 的服务器下发配置文件，请管理员检查连接状态",
                        0,
                        context.packId
                    )
                }
            } catch (e: Exception) {
                logger.error("获取服务器配置时出现异常{}", e.message)
            }
        })
        return false
    }
}
