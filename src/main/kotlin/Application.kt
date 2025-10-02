package cn.huohuas001

import cn.huohuas001.tools.manager.BanManager.initializeDatabase
import cn.huohuas001.tools.manager.CommandManager
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureRouting()

    // 应用启动后启动控制台
    monitor.subscribe(ApplicationStarted) {
        initializeDatabase() // 初始化数据库
        Thread({
            CommandManager.startConsoleCommandLoop()
        }, "Console-Thread").start()
    }
}

