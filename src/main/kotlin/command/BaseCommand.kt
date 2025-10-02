package cn.huohuas001.command

import org.slf4j.Logger

abstract class BaseCommand {
    abstract val logger: Logger
    abstract fun run(args: MutableList<String>): Boolean
}