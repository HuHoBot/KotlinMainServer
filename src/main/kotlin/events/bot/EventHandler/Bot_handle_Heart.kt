package cn.huohuas001.events.bot.EventHandler

import cn.huohuas001.events.BaseEvent
import cn.huohuas001.events.EventContext
import cn.huohuas001.events.bot.EventEnum.BotClientSendEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Bot_handle_Heart : BaseEvent() {
    override val logger: Logger = LoggerFactory.getLogger("Bot_handle_Heart")

    override fun run(context: EventContext): Boolean {
        context.botClient?.let { bot ->
            bot.sendMessage(BotClientSendEvent.BotHeart, context.body, context.packId)
            bot.updateLastHeartbeatTime()
        }
        return true
    }
}
