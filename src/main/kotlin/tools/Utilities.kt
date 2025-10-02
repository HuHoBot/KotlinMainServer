package cn.huohuas001.tools

import com.alibaba.fastjson2.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*
import kotlin.math.max

fun getPackID(): String {
    return UUID.randomUUID().toString().replace("-", "")
}

fun isVersionAllowed(clientVersion:String,latestVersion:String): Boolean{
    if ("dev" == clientVersion) {
        return true
    }

    val clientParts: Array<String?> = clientVersion.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    val latestParts: Array<String?> = latestVersion.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

    val maxLength = max(clientParts.size, latestParts.size)
    for (i in 0..<maxLength) {
        // 提取纯数字部分（处理类似"5-hotfix"的情况）
        val clientStr = if (i < clientParts.size) clientParts[i]!!.replace("[^0-9]".toRegex(), "") else "0"
        val latestStr = if (i < latestParts.size) latestParts[i]!!.replace("[^0-9]".toRegex(), "") else "0"

        // 处理空字符串的情况（当某段完全没有数字时）
        val client = if (clientStr.isEmpty()) 0 else clientStr.toInt()
        val latest = if (latestStr.isEmpty()) 0 else latestStr.toInt()

        if (client > latest) return true
        if (client < latest) return false
    }
    return true
}

/**
 * 切割命令参数（支持带引号的参数）
 *
 * @param params 输入参数字符串
 * @return 切割后的参数列表
 */
fun splitCommandParams(params: String?): MutableList<String> {
    val result: MutableList<String> = ArrayList<String>()
    if (params == null || params.trim { it <= ' ' }.isEmpty()) {
        return result
    }

    val current = StringBuilder()
    var inQuote = false

    for (i in 0..<params.length) {
        val c = params[i]

        if (c == '"') {
            if (inQuote && i > 0 && params.get(i - 1) != '\\') {
                // 结束引号
                result.add(current.toString())
                current.setLength(0)
                inQuote = false
            } else if (!inQuote) {
                // 开始引号
                inQuote = true
            } else {
                // 转义的引号
                current.append(c)
            }
        } else if (Character.isWhitespace(c) && !inQuote) {
            // 非引号内的空格分割
            if (current.isNotEmpty()) {
                result.add(current.toString())
                current.setLength(0)
            }
        } else {
            current.append(c)
        }
    }

    // 添加最后一个参数
    if (current.isNotEmpty()) {
        result.add(current.toString())
    }

    return result
}


fun generateHashKey(inputString: String?, saltLength: Int, useSalt: Boolean): String {
    var salt = ""
    if (useSalt) {
        // 仅当需要加盐时生成随机盐值
        val secureRandom = SecureRandom()
        val saltBytes = ByteArray(saltLength)
        secureRandom.nextBytes(saltBytes)
        salt = HexFormat.of().formatHex(saltBytes)
    }

    // 拼接输入字符串和盐值（当需要时）
    val combined = inputString + salt

    // 计算SHA256哈希
    val messageDigest = MessageDigest.getInstance("SHA-256")
    val hashBytes = messageDigest.digest(combined.toByteArray())

    // 将哈希结果转换为十六进制字符串
    return HexFormat.of().formatHex(hashBytes)
}


fun generateHashKey(inputString: String?, saltLength: Int): String {
    return generateHashKey(inputString, saltLength, true)
}

fun getServerConfig(serverId: String?, isMoreGroup: Boolean): JSONObject {
    val hashKey: String = generateHashKey(serverId, 16, isMoreGroup)
    val config = JSONObject()
    config["serverId"] = serverId
    config["hashKey"] = hashKey
    return config
}
