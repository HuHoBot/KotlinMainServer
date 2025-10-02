package cn.huohuas001.events.bot.EventEnum

import cn.huohuas001.events.NetworkEvent

enum class BotClientSendEvent: NetworkEvent {
    BotCallBack{
        override val eventName: String = "BotClient.callback"
    },
    BotQueryBindServerById{
        override val eventName: String = "BotClient.queryBindServerById"
    },
    BotBindServer{
        override val eventName: String = "BotClient.bindServer"
    },
    BotAddAdmin{
        override val eventName: String = "BotClient.addAdmin"
    },
    BotCallbackFunc{
        override val eventName: String = "BotClient.callbackFunc"
    },
    BotShook{
        override val eventName: String = "shaked"
    },
    BotHeart{
        override val eventName: String = "heart"
    },
    BotChat{
        override val eventName: String = "BotClient.chat"
    },
    BotGetConfirmData{
        override val eventName: String = "BotClient.getConfirmData"
    },
    BotShutdown{
        override val eventName: String = "BotClient.shutdown"
    }
}