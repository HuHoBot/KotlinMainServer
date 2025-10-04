package cn.huohuas001.events.bot.EventHandler
import cn.huohuas001.events.BaseEvent
import cn.huohuas001.events.bot.EventEnum.BotClientSendEvent
import cn.huohuas001.tools.manager.ClientManager
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.BiConsumer

object Bot_handle_QueryClientList: BaseEvent() {
    override val logger: Logger = LoggerFactory.getLogger("Bot_handle_QueryClientList")
    override fun run(): Boolean {
        try {
            // 1. 参数校验
            val serverIdList: JSONArray? = mBody.getJSONArray("serverIdList")
            if (serverIdList == null || serverIdList.isEmpty()) {
                logger.warn("ServerId列表为空")
                return false
            }

            //如果是query MainServer
            var clientList = JSONArray()
            if(serverIdList.size == 1 && serverIdList.getString(0) == "MainServer"){
                val clientName = "MainServer 已连接.\n共${ClientManager.queryOnlineClientCount()}个服务器在线."
                clientList.add(clientName)
            }else{
                // 使用Java Stream API优化集合操作
                clientList = serverIdList.stream()
                    .filter { serverId: Any? -> serverId is String }
                    .map<String?> { serverId: Any -> serverId as String }
                    .map<String?> { serverIdStr: String ->
                        val clientName: String = ClientManager.queryOnlineClient(serverIdStr)
                        clientName
                    }
                    .collect(
                        { JSONArray() },
                        BiConsumer { obj: JSONArray?, e: String? -> obj!!.add(e) },
                        { obj: JSONArray?, c: JSONArray? -> obj!!.addAll(c!!) })
            }

            // 3. 构造响应
            val response = JSONObject()
            response["clientList"] = clientList

            // 4. 发送响应并处理异常
            if (botClient != null) {
                return botClient!!.sendMessage(BotClientSendEvent.BotCallBack, response, mPackId)
            } else {
                logger.error("BotClient未连接.")
                return false
            }
        } catch (e: Exception) {
            logger.error("查询在线服务器时发现异常:", e)
            return false
        }
    }
}