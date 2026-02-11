package cn.huohuas001.events.bot.EventHandler

import cn.huohuas001.events.BaseEvent
import cn.huohuas001.events.EventContext
import cn.huohuas001.events.bot.EventEnum.BotClientSendEvent
import cn.huohuas001.tools.manager.ClientManager
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Bot_handle_QueryClientList : BaseEvent() {
    override val logger: Logger = LoggerFactory.getLogger("Bot_handle_QueryClientList")

    override fun run(context: EventContext): Boolean {
        try {
            val serverIdList = context.body.getJSONArray("serverIdList")
            if (serverIdList == null || serverIdList.isEmpty()) {
                logger.warn("ServerId列表为空")
                return false
            }

            val clientList = JSONArray()

            if (serverIdList.size == 1 && serverIdList.getString(0) == "MainServer") {
                val clientName = "MainServer 已连接.\n共${ClientManager.queryOnlineClientCount()}个服务器在线."
                clientList.add(clientName)
            } else {
                serverIdList.filterIsInstance<String>().forEach { serverIdStr ->
                    clientList.add(ClientManager.queryOnlineClient(serverIdStr))
                }
            }

            val response = JSONObject().apply {
                put("clientList", clientList)
            }

            return context.botClient?.sendMessage(BotClientSendEvent.BotCallBack, response, context.packId) ?: false
        } catch (e: Exception) {
            logger.error("查询在线服务器时发现异常:", e)
            return false
        }
    }
}
