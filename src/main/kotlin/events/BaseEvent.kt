package cn.huohuas001.events

import cn.huohuas001.client.BotClient
import cn.huohuas001.client.ServerClient
import cn.huohuas001.tools.pack.ActionPack
import cn.huohuas001.tools.manager.ClientManager
import com.alibaba.fastjson2.JSONObject
import org.slf4j.Logger

data class EventContext(
    val packId: String,
    val body: JSONObject,
    val client: ServerClient?,
    val botClient: BotClient? = ClientManager.getBotClient()
)

abstract class BaseEvent {
    abstract val logger: Logger

    fun eventCall(actionPack: ActionPack): Boolean? {
        val context = EventContext(
            packId = actionPack.packId,
            body = actionPack.body,
            client = actionPack.client as? ServerClient
        )
        return run(context)
    }

    abstract fun run(context: EventContext): Boolean?
}