package cn.huohuas001.tools.pack

import cn.huohuas001.client.ServerClient

data class ServerPack(
    val serverId: String,
    val serverClient: ServerClient?
)