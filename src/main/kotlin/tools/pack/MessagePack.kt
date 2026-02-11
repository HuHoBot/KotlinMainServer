package cn.huohuas001.tools.pack

import com.alibaba.fastjson2.JSONObject

data class MessagePack(
    val type: String,
    val body: JSONObject,
    val packId: String
)