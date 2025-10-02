package cn.huohuas001.tools.pack

import cn.huohuas001.client.BaseClient
import cn.huohuas001.events.NetworkEvent
import com.alibaba.fastjson2.JSONObject

class ActionPack(type: NetworkEvent, body: JSONObject, packId: String, client: BaseClient?) {
    val mBody: JSONObject
    val mPackId: String
    val mType: NetworkEvent
    val mClient: BaseClient?

    init {
        this.mBody = body
        this.mPackId = packId
        this.mType = type
        this.mClient = client
    }
}