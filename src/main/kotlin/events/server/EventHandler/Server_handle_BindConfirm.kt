package cn.huohuas001.events.server.EventHandler

import cn.huohuas001.events.BaseEvent
import cn.huohuas001.events.bot.EventEnum.BotClientSendEvent
import cn.huohuas001.tools.getServerConfig
import com.alibaba.fastjson2.JSONObject
import events.Server.EventEnum.ServerSendEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.Consumer

object Server_handle_BindConfirm: BaseEvent() {
    override val logger: Logger = LoggerFactory.getLogger("Server_handle_BindConfirm")
    override fun run(): Boolean {
        botClient?.sendRequestAndAwaitResponse(BotClientSendEvent.BotGetConfirmData, JSONObject(), mPackId)?.thenAccept(
            Consumer { response: JSONObject? ->
                val serverTempData = response!!.getJSONObject("serverTempData")
                val serverId = serverTempData.getString("serverId")
                val groupOpenid = serverTempData.getString("groupId")
                val author = serverTempData.getString("author")
                val isMoreGroup = !serverTempData.getBoolean("isMoreGroup")
                if (serverId == null) {
                    return@Consumer
                }
                try {
                    val config: JSONObject = getServerConfig(serverId, isMoreGroup)
                    if(mClient ==  null) {
                        return@Consumer
                    }
                    if (mClient!!.sendMessage(ServerSendEvent.ServerSendConfig, config, mPackId)) {
                        val bindServerPack = JSONObject()
                        bindServerPack["group"] = groupOpenid
                        bindServerPack["serverConfig"] = config
                        botClient!!.sendMessage(BotClientSendEvent.BotBindServer, bindServerPack, mPackId)
                        val addAdminPack = JSONObject()
                        addAdminPack["group"] = groupOpenid
                        addAdminPack["author"] = author
                        botClient!!.sendMessage(BotClientSendEvent.BotAddAdmin, addAdminPack, mPackId)
                        botClient!!.textCallBack(
                            "已向服务端下发配置文件，并添加您为机器人管理员，如有需要请使用`/管理帮助`来查看管理员命令帮助",
                            0,
                            mPackId
                        )
                    } else {
                        botClient!!.textCallBack(
                            "无法向Id为" + serverId + "的服务器下发配置文件，请管理员检查连接状态",
                            0,
                            mPackId
                        )
                    }
                } catch (e: Exception) {
                    logger.error("获取服务器配置时出现异常{}", e.message)
                }
            })
        return false
    }
}