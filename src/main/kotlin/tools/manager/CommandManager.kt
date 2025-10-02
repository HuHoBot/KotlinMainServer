package cn.huohuas001.tools.manager

import cn.huohuas001.command.BaseCommand
import cn.huohuas001.command.banCommand
import cn.huohuas001.command.exitCommand
import cn.huohuas001.command.helpCommand
import cn.huohuas001.command.listCommand
import cn.huohuas001.command.queryCommand
import cn.huohuas001.command.shutdownCommand
import cn.huohuas001.command.unbanCommand
import cn.huohuas001.tools.splitCommandParams
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

object CommandManager {
    private val logger: Logger = LoggerFactory.getLogger("CommandHelper")
    val running: AtomicBoolean = AtomicBoolean(true)
    val commandMap: MutableMap<String, BaseCommand> = HashMap<String, BaseCommand>()

    init{
        // 注册Ctrl+C钩子
        Runtime.getRuntime().addShutdownHook(Thread(Runnable {
            logger.info("接收到关闭信号，正在清理资源...")
            running.set(false)
        }))

        registerCommand("ban", banCommand)
        registerCommand("help", helpCommand)
        registerCommand("exit", exitCommand)
        registerCommand("unban", unbanCommand)
        registerCommand("list", listCommand)
        registerCommand("shutdown", shutdownCommand)
        registerCommand("query", queryCommand)
    }

    private fun registerCommand(commandName: String, command: BaseCommand) {
        commandMap[commandName.lowercase(Locale.getDefault())] = command
    }

    fun startConsoleCommandLoop() {
        val scanner = Scanner(System.`in`)
        logger.info("控制台指令系统已启动，输入 'help' 查看可用指令")
        logger.info("按Ctrl+C可安全退出")

        try {
            while (running.get()) {
                print("> ")
                val command = scanner.nextLine().trim { it <= ' ' }

                if (command.isEmpty()) {
                    continue
                }

                //切割命令参数（支持带引号的参数）
                val params: MutableList<String> = splitCommandParams(command)
                if (params.isEmpty()) {
                    continue
                }
                val commandName = params.get(0)
                val cmdObj: BaseCommand? =
                    commandMap[commandName.lowercase(Locale.getDefault())]

                if (cmdObj != null) {
                    cmdObj.run(params.subList(1, params.size))
                } else {
                    logger.warn("未知指令: {}", command)
                }
            }
        } catch (ignored: NoSuchElementException) {
        } catch (e: Exception) {
            logger.error("控制台命令循环异常", e)
        } finally {
            scanner.close()
            logger.info("控制台命令循环已停止")
        }
    }
}