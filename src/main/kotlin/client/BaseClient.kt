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
    abstract var mClientType: ClientType
    abstract var mSession: ClientSession;

    open val logger: Logger = LoggerFactory.getLogger("BaseClient")
    open var mServerId: String = ""
    open var mHashKey: String = ""
    open var mLastHeartbeatTime: Long = 0


    init {
        mClientType = clientType
        mSession = session
    }

    /**
     * 获取数据包
     * @param type
     * @param body
     * @param packId
     * @return
     */
    private fun getDataPack(type: String,body: JSONObject,packId: String): JSONObject {
        val dataPack = JSONObject()

        val header = JSONObject()
        header["type"] = type
        header["id"] = packId

        dataPack["header"] = header
        dataPack["body"] = body
        return dataPack
    }

    /**
     * 发送消息
     * @param type 消息类型
     * @param body 消息体
     * @param packId 数据包ID
     * @return
     */
    fun baseSendMessage(type: String, body: JSONObject, packId: String): Boolean {
        val sendLock = ClientManager.getSendLock()
        synchronized(sendLock){
            try {
                if (!mSession.mSession.isActive) return false // 再次检查

                val pack: JSONObject = getDataPack(type, body, packId)
                coroutineScope.launch {
                    mSession.mSession.send(pack.toJSONString())
                }
                return true
            } catch (e: IllegalStateException) {
                // 处理 TEXT_PARTIAL_WRITING 或其他状态异常
                logger.error("[Websocket] 发送消息时状态异常: {}", e.message)
                coroutineScope.launch {
                    close(CloseReason.Codes.NORMAL, "Connection state invalid") // 主动关闭会话
                }
                return false
            } catch (e: IOException) {
                // 处理 Broken pipe 或其他 IO 错误
                logger.error("[Websocket] 发送消息失败: {}", e.message)
                return false
            }
        }
    }

    /**
     * 发送消息(无PackId)
     * @param type 事件类型
     * @param body 消息包
     */
    fun baseSendMessage(type: String, body: JSONObject) {
        val packId: String = getPackID()
        baseSendMessage(type, body, packId)
    }

    /**
     * 更新最后一次心跳的时间
     */
    fun updateLastHeartbeatTime() {
        mLastHeartbeatTime = System.currentTimeMillis()
    }

    /**
     * 判断是否超时
     *
     * @param timeoutMillis 超时时间
     * @return 是否超时 boolean
     */
    fun isTimeout(timeoutMillis: Long): Boolean {
        return (System.currentTimeMillis() - mLastHeartbeatTime) > timeoutMillis
    }

    /**
     * 关闭连接
     */
    fun close(code: CloseReason.Codes, reason: String) {
        try {
            val status: CloseReason = CloseReason(code, reason)
            if (isActive()) {
                coroutineScope.launch {
                    mSession.mSession.close(status)
                }

                if (ClientManager.isRegisteredServer(mServerId)) {
                    logger.info("[Websocket]  服务端主动关闭连接, ServerId: {}", mServerId)
                }
            } else {
                if (ClientManager.isRegisteredServer(mServerId)) {
                    logger.info("[Websocket]  服务端主动关闭连接时发现客户端已离线, ServerId: {}", mServerId)
                }
            }
        } catch (e: IOException) {
            logger.error("[Websocket]  服务端主动关闭连接时发生错误:", e)
        }
    }

    fun getRemoteAddress(): String? {
        return try {
            mSession.mIp
        } catch (e: Exception) {
            null
        }
    }

    fun isActive(): Boolean {
        return mSession.mSession.isActive
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val client = other as BaseClient
        return mSession == client.mSession
    }


}