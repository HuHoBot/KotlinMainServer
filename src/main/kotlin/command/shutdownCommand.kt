package cn.huohuas001.command

import cn.huohuas001.tools.manager.ClientManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object shutdownCommand: BaseCommand() {
    override val logger: Logger = LoggerFactory.getLogger("shutdownCommand")
    override fun run(args: MutableList<String>): Boolean {
        if (args.isEmpty()) {
            logger.info("用法: shutdown <serverId>")
            return false
        }

        val serverId: String? = args.get(0)

        if (ClientManager.shutDownClient(serverId)) {
            logger.info("成功关闭服务器: {}", serverId)
        } else {
            logger.error("关闭失败，无法查找到服务器.")
        }
        return true
    }
}