package cn.huohuas001.tools.manager

import com.alibaba.fastjson2.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.file.Files

object ConfigManager {
    private val logger: Logger = LoggerFactory.getLogger("Config")
    private const val JSON_FILE_PATH = "config.json"

    private var config: JSONObject = loadConfig()

    private fun loadConfig(): JSONObject {
        return try {
            val bytes = Files.readAllBytes(File(JSON_FILE_PATH).toPath())
            JSONObject.parseObject(String(bytes))
        } catch (e: IOException) {
            logger.error("加载配置文件失败", e)
            JSONObject()
        }
    }

    fun reloadConfig() {
        config = loadConfig()
    }

    fun getKey(): String {
        return config.getString("key") ?: ""
    }

    fun getAllowedIp(): String? {
        return config.getString("allowed-ip")
    }

    fun getLatestClientVersion(platform: String): String {
        val clientVersionObj = config.getJSONObject("clientVersion")
        return clientVersionObj?.getString(platform) ?: "0.0.0"
    }
}
