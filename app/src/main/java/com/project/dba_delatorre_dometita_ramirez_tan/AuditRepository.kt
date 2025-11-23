package com.project.dba_delatorre_dometita_ramirez_tan

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.gson.JsonObject
import java.text.SimpleDateFormat
import java.util.*

class AuditRepository(
    private val daoAuditLog: Dao_AuditLog
) {
    companion object {
        private const val TAG = "AuditRepository"
    }

    // ============ LOG USER ACTION ============

    suspend fun logAction(
        action: String,
        description: String,
        status: String = "Success",
        usernameParam: String? = null
    ) {
        withContext(Dispatchers.IO) {
            try {
                val currentUser = UserSession.currentUser
                val username = usernameParam ?: currentUser?.Entity_username ?: "Unknown"
                val fullName = UserSession.getUserFullName()

                val isOnline = when (action) {
                    AuditActions.LOGIN -> true
                    AuditActions.LOGOUT -> false
                    else -> currentUser != null
                }

                val dateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "📝 Logging audit action...")
                Log.d(TAG, "Username: $username")
                Log.d(TAG, "Full Name: $fullName")
                Log.d(TAG, "Action: $action")
                Log.d(TAG, "Description: $description")

                // Create audit log entity
                val auditLog = Entity_AuditLog(
                    username = username,
                    action = action,
                    description = description,
                    dateTime = dateTime,
                    status = status,
                    isOnline = isOnline
                )

                // Save to Room
                daoAuditLog.insertAuditLog(auditLog)
                Log.d(TAG, "✅ Audit log saved to Room")

                // Save to API (fire-and-forget)
                try {
                    val auditData = JsonObject().apply {
                        addProperty("username", username)
                        addProperty("action", action)
                        addProperty("description", description)
                        addProperty("dateTime", dateTime)
                        addProperty("status", status)
                        addProperty("isOnline", isOnline)
                    }

                    BaneloApiService.safeCall {
                        BaneloApiService.api.createAuditLog(auditData)
                    }

                    Log.d(TAG, "✅ Audit log saved to API")
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ API save failed, but data is in Room: ${e.message}")
                }

                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")

            } catch (e: Exception) {
                Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.e(TAG, "❌ Failed to log audit action!")
                Log.e(TAG, "Error: ${e.message}", e)
                Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
            }
        }
    }

    // ============ GET ALL AUDIT LOGS ============

    suspend fun getAllAuditLogs(): List<Entity_AuditLog> {
        return withContext(Dispatchers.IO) {
            try {
                daoAuditLog.getAllAuditLogs()
            } catch (e: Exception) {
                Log.e(TAG, "Error getting audit logs: ${e.message}", e)
                emptyList()
            }
        }
    }
}
