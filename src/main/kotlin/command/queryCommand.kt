package cn.huohuas001.command

import cn.huohuas001.tools.manager.BanManager
import cn.huohuas001.tools.manager.ClientManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object queryCommand: BaseCommand() {
    override val logger: Logger = LoggerFactory.getLogger("queryCommand")
    override fun run(args: MutableList<String>): Boolean {
        if (args.isEmpty()) {
            logger.info("用法: query <serverId>")
            return false
        }

        val serverId = args[0]
        val serverPackage = ClientManager.getServerPackageById(serverId)

        if(BanManager.queryBanReason(serverId) != null){
            logger.info("服务器 $serverId 已被封禁,封禁理由为: ${BanManager.queryBanReason(serverId)}")
            return false
        }

        if(serverPackage.mServerClient == null){
            logger.warn("未查找到 $serverId 的服务器")
            return false
        }
        logger.info("服务器 $serverId 的详细为: ${serverPackage.mServerClient.name}|${serverPackage.mServerClient.version}|${serverPackage.mServerClient.platform}|${serverPackage.mServerClient.mSession.mIp}")
        return true
    }
}