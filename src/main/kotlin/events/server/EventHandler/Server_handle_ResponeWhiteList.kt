package cn.huohuas001.events.server.EventHandler

import cn.huohuas001.events.BaseEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Server_handle_ResponeWhiteList: BaseEvent () {
    override val logger: Logger = LoggerFactory.getLogger("Server_handle_ResponeWhiteList")

    override fun run(): Boolean {
        if (!mPackId.isEmpty()) {
            val msg: String = mBody.getString("list")
            botClient!!.callBack(msg, mPackId)
        }
        return true
    }
}