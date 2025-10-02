package cn.huohuas001.command

import cn.huohuas001.tools.manager.ClientManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object listCommand: BaseCommand() {
    override val logger: Logger = LoggerFactory.getLogger("listCommand")
    override fun run(args: MutableList<String>): Boolean {
        logger.info(
            "当前在线服务器数量: {},BotClient状态 :{}",
            ClientManager.queryOnlineClientCount(),
            if (ClientManager.getBotClient() == null) "未连接" else "已连接"
        )
        return true
    }
}