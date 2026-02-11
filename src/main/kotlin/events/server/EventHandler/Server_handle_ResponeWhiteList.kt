package cn.huohuas001.events.server.EventHandler

import cn.huohuas001.events.BaseEvent
import cn.huohuas001.events.EventContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Server_handle_ResponeWhiteList : BaseEvent() {
    override val logger: Logger = LoggerFactory.getLogger("Server_handle_ResponeWhiteList")

    override fun run(context: EventContext): Boolean {
        if (context.packId.isNotEmpty()) {
            val msg = context.body.getString("list")
            context.botClient?.textCallBack(msg, 0, context.packId)
        }
        return true
    }
}
