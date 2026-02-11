package cn.huohuas001.tools.pack

import cn.huohuas001.client.BaseClient
import cn.huohuas001.events.NetworkEvent
import com.alibaba.fastjson2.JSONObject

data class ActionPack(
    val type: NetworkEvent,
    val body: JSONObject,
    val packId: String,
    val client: BaseClient?
)