package cn.huohuas001.events.server.EventHandler

import cn.huohuas001.events.BaseEvent
import events.Server.EventEnum.ServerSendEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Server_handle_Heart: BaseEvent () {
    override val logger: Logger = LoggerFactory.getLogger("Server_handle_Heart")

    override fun run(): Boolean {
        mClient!!.sendMessage(ServerSendEvent.ServerHeart, mBody, mPackId)
        mClient!!.updateLastHeartbeatTime()
        return true
    }
}