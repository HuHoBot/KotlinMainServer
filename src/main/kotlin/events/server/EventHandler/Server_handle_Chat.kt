package cn.huohuas001.events.server.EventHandler

import cn.huohuas001.events.BaseEvent
import cn.huohuas001.events.EventContext
import cn.huohuas001.events.bot.EventEnum.BotClientSendEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Server_handle_Chat : BaseEvent() {
    override val logger: Logger = LoggerFactory.getLogger("Server_handle_Chat")

    override fun run(context: EventContext): Boolean {
        context.botClient?.sendMessage(BotClientSendEvent.BotChat, context.body, context.packId)
        return true
    }
}
