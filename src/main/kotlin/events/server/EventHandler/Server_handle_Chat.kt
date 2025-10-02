package cn.huohuas001.events.server.EventHandler

import cn.huohuas001.events.BaseEvent
import cn.huohuas001.events.bot.EventEnum.BotClientSendEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Server_handle_Chat: BaseEvent () {
    override val logger: Logger = LoggerFactory.getLogger("Server_handle_Chat")
    override fun run(): Boolean {
        botClient!!.sendMessage(BotClientSendEvent.BotChat, mBody, mPackId) //消息转发至BotClient
        return true
    }
}