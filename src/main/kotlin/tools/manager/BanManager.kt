package cn.huohuas001.tools.manager

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

object BanManager {
    const val DB_URL: String = "jdbc:sqlite:data/bans.db"
    lateinit var connection: Connection
    private val logger: Logger = LoggerFactory.getLogger("BanManager")

    // 在 initializeDatabase 函数中更新表结构
    fun initializeDatabase() {
        try {
            // 确保 data 目录存在
            val dataDir = File("data")
            if (!dataDir.exists()) {
                dataDir.mkdirs()
            }
            logger.info("BanManager数据库初始化成功.")
            connection = DriverManager.getConnection(DB_URL)
            connection.createStatement().use { stmt ->
                stmt.execute(
                    "CREATE TABLE IF NOT EXISTS bans (" +
                            "server_id TEXT PRIMARY KEY," +
                            "reason TEXT," +
                            "banned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                            "unban_time INTEGER DEFAULT -1" +  // 添加解封时间字段
                            ")"
                )
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 检查是否被封禁
    fun isBanned(serverId: String?): Boolean {
        val sql = "SELECT unban_time FROM bans WHERE server_id = ?"

        try {
            connection.prepareStatement(sql).use { pstmt ->
                pstmt.setString(1, serverId)
                val rs = pstmt.executeQuery()
                if (rs.next()) {
                    val unbanTime = rs.getLong("unban_time")
                    // 如果是-1表示永封，或者当前时间小于解封时间则仍被封禁
                    return unbanTime == -1L || System.currentTimeMillis() < unbanTime
                }
                return false
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            return false
        }
    }

    // 添加封禁记录
    fun banServer(serverId: String?, reason: String?, unbanTime: Long = -1): Boolean {
        val sql = "INSERT OR REPLACE INTO bans(server_id, reason, unban_time) VALUES(?,?,?)"

        try {
            connection.prepareStatement(sql).use { pstmt ->
                pstmt.setString(1, serverId)
                pstmt.setString(2, reason)
                pstmt.setLong(3, unbanTime)  // 设置解封时间
                pstmt.executeUpdate()
                return true
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            return false
        }
    }

    // 解除封禁
    fun unbanServer(serverId: String?): Boolean {
        val sql = "DELETE FROM bans WHERE server_id = ?"

        try {
            connection.prepareStatement(sql).use { pstmt ->
                pstmt.setString(1, serverId)
                return pstmt.executeUpdate() > 0
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            return false
        }
    }

    //查询封禁原因
    fun queryBanReason(serverId: String?): String? {
        val sql = "SELECT reason FROM bans WHERE server_id = ?"
        try {
            connection.prepareStatement(sql).use { pstmt ->
                pstmt.setString(1, serverId)
                val rs = pstmt.executeQuery()
                if (rs.next()) {
                    return rs.getString("reason")
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        }
        return null
    }

    fun queryUnbanTime(serverId: String?): Long? {
        val sql = "SELECT unban_time FROM bans WHERE server_id = ?"
        try {
            connection.prepareStatement(sql).use { pstmt ->
                pstmt.setString(1, serverId)
                val rs = pstmt.executeQuery()
                if (rs.next()) {
                    return rs.getLong("unban_time")
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        }
        return null
    }
}