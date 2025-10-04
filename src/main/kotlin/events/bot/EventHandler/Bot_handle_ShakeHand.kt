package events.bot

import cn.huohuas001.client.BotClient
import cn.huohuas001.client.ClientSession
import cn.huohuas001.events.BaseEvent
import cn.huohuas001.events.bot.EventEnum.BotClientSendEvent
import cn.huohuas001.tools.manager.ClientManager
import cn.huohuas001.tools.manager.ConfigManager
import com.alibaba.fastjson2.JSONObject
import io.ktor.websocket.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory


object Bot_handle_ShakeHand: BaseEvent() {
    override val logger: Logger = LoggerFactory.getLogger("Bot_handle_ShakeHand")
    override fun run(): Boolean{
        return false
    }

    private fun botClientConnect(session: ClientSession, serverId: String, hashKey: String) {
        val shakeHandPack = JSONObject()
        val botClient = BotClient(session)
        botClient.mServerId = serverId
        botClient.mHashKey = hashKey
        ClientManager.setBotClient(botClient)
        shakeHandPack["code"] = 1
        shakeHandPack["msg"] = ""
        botClient.sendMessage(BotClientSendEvent.BotShook, shakeHandPack)
        logger.info("HuHoBot BotClient 已连接.")

        //重新清空WaitingList
        ClientManager.reShakeWaitingServer()
    }
    
    fun runShake(session: ClientSession): Boolean{
        val serverId: String = mBody.getString("serverId")
        val hashKey: String = mBody.getString("hashKey")

        val shakeHandPack = JSONObject()
        val _botClient = BotClient(session)

        if (serverId != "BotClient") {
            _botClient.shutdown(CloseReason.Codes.VIOLATED_POLICY, "")
            return false
        }

        if (hashKey != ConfigManager.getKey()) {
            val msg = "密钥错误"
            shakeHandPack["code"] = 3
            shakeHandPack["msg"] = msg
            _botClient.sendMessage(BotClientSendEvent.BotShook, shakeHandPack)
            _botClient.shutdown(CloseReason.Codes.VIOLATED_POLICY, msg)
            return false
        }

        val remoteIp: String = session.mIp

        if (remoteIp != ConfigManager.getAllowedIp()) {
            val msg = "IP 地址不在允许范围内"

            logger.warn("有一个非法的 BotClient 尝试连接，来自 $remoteIp")

            shakeHandPack["code"] = 7
            shakeHandPack["msg"] = msg
            _botClient.sendMessage(BotClientSendEvent.BotShook, shakeHandPack)
            _botClient.shutdown(CloseReason.Codes.VIOLATED_POLICY, msg)
            return false
        }

        botClientConnect(session, serverId, hashKey)
        return true
    }
}
