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

            val result = BaneloApiService.safeCall {
                BaneloApiService.api.createWasteLog(wasteData)
            }

            // ✅ Only log success and mark as synced if API call succeeded
            if (result.isSuccess) {
                Log.d("WasteLogRepo", "✅ Waste log synced to API")

                // Update local record as synced
                dao.markAsSynced(wasteLog.id, wasteLog.id.toString())
            } else {
                Log.w("WasteLogRepo", "⚠️ API sync failed: ${result.exceptionOrNull()?.message}")
                Log.w("WasteLogRepo", "⚠️ Data is saved locally, will retry sync later")
            }

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

    suspend fun syncFromApi() {
        withContext(Dispatchers.IO) {
            try {
                Log.d("WasteLogRepo", "📡 Fetching waste logs from API...")

                val result = BaneloApiService.safeCall {
                    BaneloApiService.api.getWasteLogs()
                }

                if (result.isSuccess) {
                    val wasteLogs = result.getOrNull() ?: return@withContext
                    Log.d("WasteLogRepo", "✅ API returned ${wasteLogs.size} waste logs")

                    // Convert API responses to entities
                    val entities = wasteLogs.map { convertToEntity(it) }

                    // Cache in Room for offline access
                    dao.clearAllWasteLogs()
                    dao.insertWasteLogs(entities)

                    Log.d("WasteLogRepo", "✅ Synced to Room database")
                } else {
                    val error = result.exceptionOrNull()
                    Log.e("WasteLogRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
                    Log.e("WasteLogRepo", "❌ API sync failed!")
                    Log.e("WasteLogRepo", "Error message: ${error?.message}")
                    Log.e("WasteLogRepo", "Error type: ${error?.javaClass?.simpleName}")
                    Log.e("WasteLogRepo", "━━━━━━━━━━━━━━━━━━━━━━━━", error)
                    Log.w("WasteLogRepo", "⚠️ Using local data instead")
                }

            } catch (e: Exception) {
                Log.e("WasteLogRepo", "❌ API sync failed: ${e.message}")
                // Continue with local data
            }
        }
    }

    // ============ HELPER: Convert API response to Entity ============

    private fun convertToEntity(response: WasteLogResponse): Entity_WasteLog {
        // Convert ISO 8601 timestamp to "yyyy-MM-dd HH:mm:ss" format
        val formattedWasteDate = convertIso8601ToLocalFormat(response.wasteDate ?: "")

        return Entity_WasteLog(
            id = 0, // Room will auto-generate
            firebaseId = response.firebaseId ?: "",
            productFirebaseId = response.productFirebaseId ?: "",
            productName = response.productName ?: "",
            category = response.category ?: "",
            quantity = response.quantity ?: 0,
            reason = response.reason ?: "Unknown",
            wasteDate = formattedWasteDate,
            recordedBy = response.recordedBy ?: "Unknown",
            isSyncedToFirebase = true // Data from API is already synced
        )
    }

    // Convert ISO 8601 timestamp to "yyyy-MM-dd HH:mm:ss" format
    private fun convertIso8601ToLocalFormat(iso8601: String): String {
        if (iso8601.isEmpty()) return ""

        return try {
            // Parse ISO 8601 format: "2026-01-07T03:41:00.913Z"
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
            inputFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")

            // Format to expected format: "yyyy-MM-dd HH:mm:ss"
            val outputFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())

            val date = inputFormat.parse(iso8601)
            if (date != null) {
                outputFormat.format(date)
            } else {
                Log.w("WasteLogRepo", "⚠️ Failed to parse date: $iso8601")
                iso8601
            }
        } catch (e: Exception) {
            Log.e("WasteLogRepo", "❌ Date conversion error: ${e.message}")
            // Return original if conversion fails
            iso8601
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
