package cn.huohuas001.client

import cn.huohuas001.tools.manager.ClientManager
import cn.huohuas001.tools.getPackID
import com.alibaba.fastjson2.JSONObject
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.slf4j.Logger
import java.io.IOException

abstract class BaseClient(session: ClientSession, clientType: ClientType) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    abstract var clientType: ClientType
    abstract var session: ClientSession

    open val logger: Logger = LoggerFactory.getLogger("BaseClient")
    open var serverId: String = ""
    open var hashKey: String = ""
    open var lastHeartbeatTime: Long = 0

    init {
        this.clientType = clientType
        this.session = session
        updateLastHeartbeatTime()
    }

    private fun getDataPack(type: String, body: JSONObject, packId: String): JSONObject {
        val dataPack = JSONObject()
        val header = JSONObject()
        header["type"] = type
        header["id"] = packId
        dataPack["header"] = header
        dataPack["body"] = body
        return dataPack
    }

    fun baseSendMessage(type: String, body: JSONObject, packId: String): Boolean {
        val sendLock = ClientManager.getSendLock()
        synchronized(sendLock) {
            try {
                if (!session.session.isActive) return false

                val pack = getDataPack(type, body, packId)
                coroutineScope.launch {
                    session.session.send(pack.toJSONString())
                }
                return true
            } catch (e: IllegalStateException) {
                logger.error("发送消息时状态异常: {}", e.message)
                coroutineScope.launch {
                    close(CloseReason.Codes.NORMAL, "Connection state invalid")
                }
                return false
            } catch (e: IOException) {
                logger.error("发送消息失败: {}", e.message)
                return false
            }
        }
    }

    fun baseSendMessage(type: String, body: JSONObject) {
        val packId = getPackID()
        baseSendMessage(type, body, packId)
    }

    fun updateLastHeartbeatTime() {
        lastHeartbeatTime = System.currentTimeMillis()
    }

    fun isTimeout(timeoutMillis: Long): Boolean {
        return (System.currentTimeMillis() - lastHeartbeatTime) > timeoutMillis
    }

    fun close(code: CloseReason.Codes, reason: String) {
        try {
            val status = CloseReason(code, reason)
            if (isActive()) {
                coroutineScope.launch {
                    session.session.close(status)
                }
                if (ClientManager.isRegisteredServer(serverId)) {
                    logger.info("服务端主动关闭连接, ServerId: {}", serverId)
                }
            } else {
                if (ClientManager.isRegisteredServer(serverId)) {
                    logger.info("服务端主动关闭连接时发现客户端已离线, ServerId: {}", serverId)
                }
            }
        } catch (e: IOException) {
            logger.error("服务端主动关闭连接时发生错误:", e)
        }
    }

    fun getRemoteAddress(): String? {
        return try {
            session.ip
        } catch (e: Exception) {
            null
        }
    }

    fun isActive(): Boolean {
        return session.session.isActive
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val client = other as BaseClient
        return session == client.session
    }

    override fun hashCode(): Int {
        return session.hashCode()
    }
}
