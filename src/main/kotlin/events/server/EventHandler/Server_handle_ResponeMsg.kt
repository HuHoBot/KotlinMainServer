package cn.huohuas001.events.server.EventHandler

import cn.huohuas001.events.BaseEvent
import cn.huohuas001.events.EventContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Server_handle_ResponeMsg : BaseEvent() {
    override val logger: Logger = LoggerFactory.getLogger("Server_handle_ResponeMsg")

    override fun run(context: EventContext): Boolean {
        if (context.packId.isNotEmpty()) {
            val msg = context.body.getString("msg")
            val callbackConvert = context.body.getInteger("callbackConvert")
            context.botClient?.textCallBack(msg, callbackConvert, context.packId)
        }
        return true
    }
}
