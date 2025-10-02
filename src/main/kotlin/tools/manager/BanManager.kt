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
                            "banned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
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
        val sql = "SELECT server_id FROM bans WHERE server_id = ?"

        try {
            connection.prepareStatement(sql).use { pstmt ->
                pstmt.setString(1, serverId)
                val rs = pstmt.executeQuery()
                return rs.next()
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            return false
        }
    }

    // 添加封禁记录
    fun banServer(serverId: String?, reason: String?): Boolean {
        val sql = "INSERT OR REPLACE INTO bans(server_id, reason) VALUES(?,?)"

        try {
            connection.prepareStatement(sql).use { pstmt ->
                pstmt.setString(1, serverId)
                pstmt.setString(2, reason)
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
}