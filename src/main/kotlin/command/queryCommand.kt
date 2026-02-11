package cn.huohuas001.command

import cn.huohuas001.tools.manager.BanManager
import cn.huohuas001.tools.manager.ClientManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object queryCommand : BaseCommand() {
    override val logger: Logger = LoggerFactory.getLogger("queryCommand")

    override fun run(args: MutableList<String>): Boolean {
        if (args.isEmpty()) {
            logger.info("用法: query <serverId>")
            return false
        }

        val serverId = args[0]
        val serverPackage = ClientManager.getServerPackageById(serverId)

        val banReason = BanManager.queryBanReason(serverId)
        if (banReason != null) {
            logger.info("服务器 $serverId 已被封禁,封禁理由为: $banReason")
            return false
        }

        val serverClient = serverPackage.serverClient
        if (serverClient == null) {
            logger.warn("未查找到 $serverId 的服务器")
            return false
        }

        logger.info("服务器 $serverId 的详细为: ${serverClient.name}|${serverClient.version}|${serverClient.platform}|${serverClient.session.ip}")
        return true
    }
}
