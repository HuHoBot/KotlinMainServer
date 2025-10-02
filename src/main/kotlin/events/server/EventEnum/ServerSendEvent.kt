package events.Server.EventEnum;

import cn.huohuas001.events.NetworkEvent
import cn.huohuas001.events.server.EventEnum.ServerRecvEvent

enum class ServerSendEvent: NetworkEvent {
    ServerSendConfig{
        override val eventName: String = "sendConfig"
    },
    ServerShakeHand{
        override val eventName: String = "shaked"
    },
    ServerChat{
        override val eventName: String = "chat"
    },
    ServerAddWhiteList{
        override val eventName: String = "add"
    },
    ServerDeleteWhiteList{
        override val eventName: String = "delete"
    },
    ServerRunCommand{
        override val eventName: String = "cmd"
    },
    ServerQueryWhiteList{
        override val eventName: String = "queryList"
    },
    ServerQueryOnline{
        override val eventName: String = "queryOnline"
    },
    ServerShutdown{
        override val eventName: String = "shutdown"
    },
    ServerRunAction{
        override val eventName: String = "run"
    },
    ServerRunActionAdmin{
        override val eventName: String = "runAdmin"
    },
    ServerHeart{
        override val eventName: String = "heart"
    },
    ServerBindRequest{
        override val eventName: String = "bindRequest"
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
fun getServerSendEvent(value: String?): ServerSendEvent {
    if (value.isNullOrEmpty()) {
        return ServerSendEvent.UNKNOWN
    }

    // 根据 eventName 属性查找匹配的枚举项
    return ServerSendEvent.entries.find {
        it.eventName.equals(value, ignoreCase = true)
    } ?: ServerSendEvent.UNKNOWN
}
