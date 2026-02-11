package cn.huohuas001.tools.manager

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

object BanManager {
    private const val DB_URL = "jdbc:sqlite:data/bans.db"
    private val logger: Logger = LoggerFactory.getLogger("BanManager")

    lateinit var connection: Connection

    fun initializeDatabase() {
        try {
            val dataDir = File("data")
            if (!dataDir.exists()) {
                dataDir.mkdirs()
            }
            connection = DriverManager.getConnection(DB_URL)
            connection.createStatement().use { stmt ->
                stmt.execute(
                    "CREATE TABLE IF NOT EXISTS bans (" +
                            "server_id TEXT PRIMARY KEY," +
                            "reason TEXT," +
                            "banned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                            "unban_time INTEGER DEFAULT -1" +
                            ")"
                )
            }
            logger.info("BanManager数据库初始化成功.")
        } catch (e: SQLException) {
            logger.error("数据库初始化失败", e)
        } catch (e: Exception) {
            logger.error("数据库初始化发生异常", e)
        }
    }

    fun isBanned(serverId: String?): Boolean {
        val sql = "SELECT unban_time FROM bans WHERE server_id = ?"
        try {
            connection.prepareStatement(sql).use { pstmt ->
                pstmt.setString(1, serverId)
                val rs = pstmt.executeQuery()
                if (rs.next()) {
                    val unbanTime = rs.getLong("unban_time")
                    return unbanTime == -1L || System.currentTimeMillis() < unbanTime
                }
            }
        } catch (e: SQLException) {
            logger.error("查询封禁状态失败: serverId={}", serverId, e)
        }
        return false
    }

    fun banServer(serverId: String?, reason: String?, unbanTime: Long = -1): Boolean {
        val sql = "INSERT OR REPLACE INTO bans(server_id, reason, unban_time) VALUES(?,?,?)"
        try {
            connection.prepareStatement(sql).use { pstmt ->
                pstmt.setString(1, serverId)
                pstmt.setString(2, reason)
                pstmt.setLong(3, unbanTime)
                pstmt.executeUpdate()
                return true
            }
        } catch (e: SQLException) {
            logger.error("添加封禁记录失败: serverId={}", serverId, e)
        }
        return false
    }

    fun unbanServer(serverId: String?): Boolean {
        val sql = "DELETE FROM bans WHERE server_id = ?"
        try {
            connection.prepareStatement(sql).use { pstmt ->
                pstmt.setString(1, serverId)
                return pstmt.executeUpdate() > 0
            }
        } catch (e: SQLException) {
            logger.error("解除封禁失败: serverId={}", serverId, e)
        }
        return false
    }

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
            logger.error("查询封禁原因失败: serverId={}", serverId, e)
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
            logger.error("查询解封时间失败: serverId={}", serverId, e)
        }
        return null
    }
}
