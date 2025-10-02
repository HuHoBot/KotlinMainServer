package cn.huohuas001

import cn.huohuas001.client.ClientSession
import io.ktor.server.application.*
import io.ktor.server.plugins.origin
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import java.util.*

fun Application.configureRouting() {
    install(WebSockets) {
        pingPeriodMillis = 30000
        timeoutMillis = 60000
        maxFrameSize = 1024 * 1024
    }

    routing {
        webSocket("/") {
            val session = this
            val sessionId = UUID.randomUUID().toString()

            val ip = call.request.origin.remoteAddress // 获取客户端IP

            val clientSession = ClientSession(session, ip)
            
            // 处理连接建立
            WebsocketServer.handleConnectionEstablished(clientSession, sessionId)
            
            try {
                // 处理传入的消息
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()
                            WebsocketServer.handleTextMessage(clientSession, text)
                        }
                        is Frame.Close -> {
                            WebsocketServer.handleConnectionClosed(clientSession, frame.readReason())
                            frame.readReason()?.let { close(it) }
                        }
                        else -> {
                            // 忽略其他类型的帧
                        }
                    }
                }
            } catch (e: Exception) {
                WebsocketServer.handleError(session, e)
            } finally {
                // 确保连接关闭时进行清理
                WebsocketServer.handleConnectionClosed(clientSession, CloseReason(CloseReason.Codes.NORMAL, "Connection closed"))
            }
        }
    }
}