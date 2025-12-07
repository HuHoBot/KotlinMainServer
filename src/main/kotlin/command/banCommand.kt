package cn.huohuas001.command

import cn.huohuas001.tools.manager.BanManager.banServer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object banCommand: BaseCommand() {
    override val logger: Logger = LoggerFactory.getLogger("banCommand")
    override fun run(args: MutableList<String>): Boolean {
        if (args.isEmpty()) {
            logger.info("用法: ban <serverId> <reason>")
            return false
        }

        val serverId = args[0]
        var reason: String? = "无"
        if (args.size > 1) {
            reason = args[1]
        }

        if (banServer(serverId, reason,-1)) {
            logger.info("成功封禁服务器: {}", serverId)
            return true
        } else {
            logger.error("封禁失败")
            return false
        }
    }
}