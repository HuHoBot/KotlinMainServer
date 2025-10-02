package cn.huohuas001.command

import cn.huohuas001.tools.manager.BanManager.unbanServer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object unbanCommand: BaseCommand() {
    override val logger: Logger = LoggerFactory.getLogger("unbanCommand")
    override fun run(args: MutableList<String>): Boolean {
        if (args.isEmpty()) {
            logger.info("用法: unban <serverId>")
            return false
        }

        val serverId: String? = args.get(0)

        if (unbanServer(serverId)) {
            logger.info("成功解封服务器: {}", serverId)
            return true
        } else {
            logger.error("解封失败，服务器可能未被封禁")
            return false
        }
    }
}