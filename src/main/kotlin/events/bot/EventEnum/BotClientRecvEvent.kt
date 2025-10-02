package cn.huohuas001.events.bot.EventEnum

import cn.huohuas001.events.NetworkEvent

enum class BotClientRecvEvent: NetworkEvent {
    BotSendServerMsgEvent{
        override val eventName: String = "BotClient.sendMsgByServerId"
    },
    BotQueryClientListEvent{
        override val eventName: String = "BotClient.queryClientList"
    },
    BotQueryStatusEvent{
        override val eventName: String = "BotClient.queryStatus"
    },
    BotShakeHandEvent{
        override val eventName: String = "BotClient.shakeHand"
    },
    BotHeartEvent{
        override val eventName: String = "BotClient.heart"
    },
    UNKNOWN{
        override val eventName: String = "unknown"
    }
}

/**
 * 根据字符串值查找对应的枚举项（不区分大小写）
 *
 * @param value 要查找的值
 * @return 对应的枚举项，找不到则返回 unknown
 */
fun getBotRecvEvent(value: String?): BotClientRecvEvent {
    if (value.isNullOrEmpty()) {
        return BotClientRecvEvent.UNKNOWN
    }

    // 根据 eventName 属性查找匹配的枚举项
    return BotClientRecvEvent.entries.find {
        it.eventName.equals(value, ignoreCase = true)
    } ?: BotClientRecvEvent.UNKNOWN
}