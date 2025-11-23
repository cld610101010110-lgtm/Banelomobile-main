package com.project.dba_delatorre_dometita_ramirez_tan

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.gson.JsonObject

class WasteLogRepository(private val dao: Dao_WasteLog) {

    // ============ INSERT WASTE LOG ============

    suspend fun insertWasteLog(wasteLog: Entity_WasteLog) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("WasteLogRepo", "➕ Recording waste: ${wasteLog.productName} - ${wasteLog.quantity} units")

                // Insert to local database first
                dao.insertWasteLog(wasteLog)

                // Sync to API (fire-and-forget for performance)
                syncWasteLogToApi(wasteLog)

            } catch (e: Exception) {
                Log.e("WasteLogRepo", "❌ Failed to insert waste log: ${e.message}", e)
                throw e
            }
        }
    }

    // ============ SYNC TO API (OPTIMIZED) ============

    private suspend fun syncWasteLogToApi(wasteLog: Entity_WasteLog) {
        try {
            val wasteData = JsonObject().apply {
                addProperty("productFirebaseId", wasteLog.productFirebaseId)
                addProperty("productName", wasteLog.productName)
                addProperty("category", wasteLog.category)
                addProperty("quantity", wasteLog.quantity)
                addProperty("reason", wasteLog.reason)
                addProperty("wasteDate", wasteLog.wasteDate)
                addProperty("recordedBy", wasteLog.recordedBy)
            }

            BaneloApiService.safeCall {
                BaneloApiService.api.createWasteLog(wasteData)
            }

            Log.d("WasteLogRepo", "✅ Waste log synced to API")

            // Update local record as synced
            dao.markAsSynced(wasteLog.id, wasteLog.id.toString())

        } catch (e: Exception) {
            Log.e("WasteLogRepo", "⚠️ API sync failed (data saved locally): ${e.message}")
            // Don't throw - waste is already recorded locally
        }
    }

    // ============ SYNC UNSYNCED LOGS ============

    suspend fun syncUnsyncedLogs() {
        withContext(Dispatchers.IO) {
            try {
                val unsyncedLogs = dao.getUnsyncedWasteLogs()
                Log.d("WasteLogRepo", "🔄 Found ${unsyncedLogs.size} unsynced waste logs")

                unsyncedLogs.forEach { log ->
                    syncWasteLogToApi(log)
                }

            } catch (e: Exception) {
                Log.e("WasteLogRepo", "❌ Sync failed: ${e.message}")
            }
        }
    }

    // ============ FETCH FROM API (OPTIMIZED) ============

    suspend fun syncFromFirebase() {
        withContext(Dispatchers.IO) {
            try {
                Log.d("WasteLogRepo", "📡 Fetching waste logs from API...")

                val result = BaneloApiService.safeCall {
                    BaneloApiService.api.getWasteLogs()
                }

                if (result.isSuccess) {
                    Log.d("WasteLogRepo", "✅ API sync completed")
                    // Data is already in Room, API is backup
                } else {
                    Log.w("WasteLogRepo", "⚠️ API sync failed, using local data")
                }

            } catch (e: Exception) {
                Log.e("WasteLogRepo", "❌ API sync failed: ${e.message}")
                // Continue with local data
            }
        }
    }

    // ============ QUERY METHODS ============

    suspend fun getAllWasteLogs(): List<Entity_WasteLog> = dao.getAllWasteLogs()

    suspend fun getWasteLogsByProduct(productId: String): List<Entity_WasteLog> =
        dao.getWasteLogsByProduct(productId)

    suspend fun getWasteLogsByDateRange(startDate: String, endDate: String): List<Entity_WasteLog> =
        dao.getWasteLogsByDateRange(startDate, endDate)

    suspend fun getWasteLogsByUser(username: String): List<Entity_WasteLog> =
        dao.getWasteLogsByUser(username)

    suspend fun getTotalWasteForProduct(productId: String): Int =
        dao.getTotalWasteForProduct(productId) ?: 0

    suspend fun getTotalWasteByDateRange(startDate: String, endDate: String): Int =
        dao.getTotalWasteByDateRange(startDate, endDate) ?: 0

    suspend fun clearAllWasteLogs() = dao.clearAllWasteLogs()
}
