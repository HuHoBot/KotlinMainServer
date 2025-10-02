package cn.huohuas001.events.server.EventEnum

import cn.huohuas001.events.NetworkEvent

enum class ServerRecvEvent: NetworkEvent {
    ServerSendMessageEvent{
        override val eventName: String = "sendMsg"
    },
    ServerHeartEvent{
        override val eventName: String = "heart"
    },
    ServerReplySuccessEvent{
        override val eventName: String = "success"
    },
    ServerReplyErrorEvent{
        override val eventName: String = "error"
    },
    ServerShakeHandEvent{
        override val eventName: String = "shakeHand"
    },
    ServerQueryWhiteListEvent{
        override val eventName: String = "queryWl"
    },
    ServerQueryOnlineEvent{
        override val eventName: String = "queryOnline"
    },
    ServerBindConfirmEvent{
        override val eventName: String = "bindConfirm"
    },
    ServerChatEvent{
        override val eventName: String = "chat"
    },
    UNKNOWN{
        override val eventName: String = "unknown"
    };
}

/**
 * 根据字符串值查找对应的枚举项（不区分大小写）
 *
 * @param value 要查找的值
 * @return 对应的枚举项，找不到则返回 unknown
 */
fun getServerRecvEvent(value: String?): ServerRecvEvent {
    if (value.isNullOrEmpty()) {
        return ServerRecvEvent.UNKNOWN
    }

    // 根据 eventName 属性查找匹配的枚举项
    return ServerRecvEvent.entries.find {
        it.eventName.equals(value, ignoreCase = true)
    } ?: ServerRecvEvent.UNKNOWN
}
