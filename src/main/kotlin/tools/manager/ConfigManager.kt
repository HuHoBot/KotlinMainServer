package cn.huohuas001.tools.manager

import com.alibaba.fastjson2.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.file.Files

object ConfigManager {
    val logger: Logger = LoggerFactory.getLogger("Config")
    const val JSON_FILE_PATH: String = "config.json"
    var config: JSONObject = JSONObject()

    init {
        loadConfig()
    }

    fun loadConfig() {
        try {
            val bytes = Files.readAllBytes(File(JSON_FILE_PATH).toPath())
            this.config = JSONObject.parseObject(String(bytes))
        } catch (e: IOException) {
            logger.error("加载配置文件失败")
        }
    }

    fun getKey(): String{
        loadConfig()
        return config.getString("key")
    }

    fun getAllowedIp(): String? {
        loadConfig()
        return config.getString("allowed-ip")
    }

    fun getLatestClientVersion(platform: String): String {
        loadConfig()
        val clientVersionObj = config.getJSONObject("clientVersion")
        if (clientVersionObj != null && clientVersionObj.containsKey(platform)) {
            return clientVersionObj.getString(platform) ?: "0.0.0"
        }
        return "0.0.0"
    }
}