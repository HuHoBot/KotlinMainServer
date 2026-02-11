package events.bot

import cn.huohuas001.client.BotClient
import cn.huohuas001.client.ClientSession
import cn.huohuas001.events.BaseEvent
import cn.huohuas001.events.EventContext
import cn.huohuas001.events.bot.EventEnum.BotClientSendEvent
import cn.huohuas001.tools.manager.ClientManager
import cn.huohuas001.tools.manager.ConfigManager
import cn.huohuas001.tools.pack.ActionPack
import com.alibaba.fastjson2.JSONObject
import io.ktor.websocket.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Bot_handle_ShakeHand : BaseEvent() {
    override val logger: Logger = LoggerFactory.getLogger("Bot_handle_ShakeHand")

    override fun run(context: EventContext): Boolean {
        return false
    }

    private fun botClientConnect(session: ClientSession, serverId: String, hashKey: String) {
        val botClient = BotClient(session).apply {
            this.serverId = serverId
            this.hashKey = hashKey
        }
        ClientManager.setBotClient(botClient)

        val shakeHandPack = JSONObject().apply {
            put("code", 1)
            put("msg", "")
        }
        botClient.sendMessage(BotClientSendEvent.BotShook, shakeHandPack)
        logger.info("HuHoBot BotClient 已连接.")

        ClientManager.reShakeWaitingServer()
    }

    fun runShake(session: ClientSession, msgPack: ActionPack): Boolean {
        val body = msgPack.body
        val serverId = body.getString("serverId")
        val hashKey = body.getString("hashKey")

        val _botClient = BotClient(session)

        if (serverId != "BotClient") {
            _botClient.shutdown(CloseReason.Codes.VIOLATED_POLICY, "")
            return false
        }

        if (hashKey != ConfigManager.getKey()) {
            val shakeHandPack = JSONObject().apply {
                put("code", 3)
                put("msg", "密钥错误")
            }
            _botClient.sendMessage(BotClientSendEvent.BotShook, shakeHandPack)
            _botClient.shutdown(CloseReason.Codes.VIOLATED_POLICY, "密钥错误")
            return false
        }

        val remoteIp = session.ip

        if (remoteIp != ConfigManager.getAllowedIp()) {
            logger.warn("有一个非法的 BotClient 尝试连接，来自 $remoteIp")

            val shakeHandPack = JSONObject().apply {
                put("code", 7)
                put("msg", "IP 地址不在允许范围内")
            }
            _botClient.sendMessage(BotClientSendEvent.BotShook, shakeHandPack)
            _botClient.shutdown(CloseReason.Codes.VIOLATED_POLICY, "IP 地址不在允许范围内")
            return false
        }

        botClientConnect(session, serverId, hashKey)
        return true
    }
}
