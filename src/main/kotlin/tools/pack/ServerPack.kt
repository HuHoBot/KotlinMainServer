package cn.huohuas001.tools.pack

import cn.huohuas001.client.ServerClient

class ServerPack(serverId: String, serverClient: ServerClient?) {
    val mServerId: String
    val mServerClient: ServerClient?

    init {
        mServerId = serverId
        mServerClient = serverClient
    }
}