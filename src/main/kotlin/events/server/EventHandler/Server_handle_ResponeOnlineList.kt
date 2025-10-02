package cn.huohuas001.events.server.EventHandler

import cn.huohuas001.events.BaseEvent
import com.alibaba.fastjson2.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Server_handle_ResponeOnlineList: BaseEvent () {
    override val logger: Logger = LoggerFactory.getLogger("Server_handle_ResponeOnlineList")

    override fun run(): Boolean {
        if (!mPackId.isEmpty()) {
            val msg: JSONObject = mBody.getJSONObject("list")
            botClient!!.callBack(msg, mPackId)
        }
        return true
    }
}