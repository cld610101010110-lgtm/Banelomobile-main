package com.project.dba_delatorre_dometita_ramirez_tan

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface Dao_AuditLog {

    @Insert
    suspend fun insertAuditLog(log: Entity_AuditLog)

    @Query("SELECT * FROM audit_logs ORDER BY dateTime DESC")
    suspend fun getAllAuditLogs(): List<Entity_AuditLog>

    @Query("SELECT * FROM audit_logs WHERE username = :username ORDER BY dateTime DESC")
    suspend fun getAuditLogsByUser(username: String): List<Entity_AuditLog>

    @Query("DELETE FROM audit_logs")
    suspend fun clearAllLogs()
}
