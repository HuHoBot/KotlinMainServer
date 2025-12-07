package cn.huohuas001.events.server.EventHandler

import cn.huohuas001.events.BaseEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Server_handle_ResponeMsg: BaseEvent () {
    override val logger: Logger = LoggerFactory.getLogger("Server_handle_ResponeMsg")

    override fun run(): Boolean {
        if (!mPackId.isEmpty()) {
            val msg: String = mBody.getString("msg")
            val callbackConvert:Int = mBody.getInteger("callbackConvert")
            botClient!!.textCallBack(msg, callbackConvert, mPackId)
        }
        return true
    }
}