package cn.huohuas001.events

import cn.huohuas001.client.BotClient
import cn.huohuas001.client.ServerClient
import cn.huohuas001.tools.pack.ActionPack
import cn.huohuas001.tools.manager.ClientManager
import com.alibaba.fastjson2.JSONObject
import org.slf4j.Logger

abstract class BaseEvent {
    abstract val logger: Logger
    lateinit var mPackId: String
    lateinit var mBody: JSONObject
    var mClient: ServerClient? =  null
    var botClient: BotClient? = ClientManager.getBotClient()

    fun eventCall(actionPack: ActionPack): Boolean?{
        refreshBotClient()
        setData(actionPack)
        return run()
    }

    fun setData(actionPack: ActionPack){
        mPackId = actionPack.mPackId
        mBody = actionPack.mBody
        mClient = actionPack.mClient as? ServerClient
    }

    fun refreshBotClient(){
        botClient = ClientManager.getBotClient();
    }

    abstract fun run(): Boolean?
}