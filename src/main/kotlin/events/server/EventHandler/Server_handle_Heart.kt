package cn.huohuas001.events.server.EventHandler

import cn.huohuas001.events.BaseEvent
import cn.huohuas001.events.EventContext
import events.Server.EventEnum.ServerSendEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Server_handle_Heart : BaseEvent() {
    override val logger: Logger = LoggerFactory.getLogger("Server_handle_Heart")

    override fun run(context: EventContext): Boolean {
        context.client?.let { client ->
            client.sendMessage(ServerSendEvent.ServerHeart, context.body, context.packId)
            client.updateLastHeartbeatTime()
        }
        return true
    }
}
