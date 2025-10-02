package cn.huohuas001.command

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

object exitCommand: BaseCommand() {
    override val logger: Logger = LoggerFactory.getLogger("exitCommand")
    override fun run(args: MutableList<String>): Boolean {
        logger.info("正在关闭服务器...")
        exitProcess(0)
    }
}