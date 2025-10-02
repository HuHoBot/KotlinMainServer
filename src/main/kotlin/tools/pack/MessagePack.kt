package cn.huohuas001.tools.pack

import com.alibaba.fastjson2.JSONObject

class MessagePack(type: String, body: JSONObject, packId: String,) {
    val mPackId: String
    val mBody: JSONObject
    val mType: String

    init {
        mPackId = packId
        mBody = body
        mType = type
    }
}