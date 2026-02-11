package cn.huohuas001

import cn.huohuas001.tools.manager.BanManager.initializeDatabase
import cn.huohuas001.tools.manager.CommandManager
import io.ktor.server.application.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureRouting()

    monitor.subscribe(ApplicationStarted) {
        initializeDatabase()
        launch(Dispatchers.IO) {
            CommandManager.startConsoleCommandLoop()
        }
    }
}
