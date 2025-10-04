package cn.huohuas001.command

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object helpCommand: BaseCommand() {
    override val logger: Logger = LoggerFactory.getLogger("helpCommand")
    override fun run(args: MutableList<String>): Boolean {
        logger.info("可用指令:")
        logger.info("  help     - 显示帮助信息")
        logger.info("  exit     - 退出应用程序")
        logger.info("  ban      - 封禁服务器")
        logger.info("  unban    - 解封服务器")
        logger.info("  list     - 查看链接数量")
        logger.info("  shutdown - 关闭服务器")
        logger.info("  query    - 查询在线服务器或封禁原因")
        return true
    }
}