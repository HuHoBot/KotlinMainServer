package cn.huohuas001.events.server.EventHandler

import cn.huohuas001.events.BaseEvent
import cn.huohuas001.events.EventContext
import com.alibaba.fastjson2.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Server_handle_ResponeOnlineList : BaseEvent() {
    override val logger: Logger = LoggerFactory.getLogger("Server_handle_ResponeOnlineList")

    override fun run(context: EventContext): Boolean {
        if (context.packId.isNotEmpty()) {
            val msg = context.body.getJSONObject("list")
            context.botClient?.jsonCallBack(msg, context.packId)
        }
        return true
    }
}
