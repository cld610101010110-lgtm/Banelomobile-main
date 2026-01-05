package com.project.dba_delatorre_dometita_ramirez_tan

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.runBlocking

/**
 * Background worker that runs daily at 6 AM to:
 * 1. Find all products in Inventory B with expiration_date <= today
 * 2. Deduct expired quantities from inventory_b
 * 3. Automatically record waste logs with reason "Expired"
 * 4. Sync changes to API
 */
class ExpirationCheckWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    companion object {
        private const val TAG = "ExpirationCheckWorker"
    }

    override fun doWork(): Result {
        return try {
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "🕐 ExpirationCheckWorker started (6 AM daily job)")
            Log.d(TAG, "⏰ Checking for expired products in Inventory B...")

            // Get database and repository
            val database = Database_Products.getDatabase(applicationContext)
            val productRepository = ProductRepository(
                database.dao_products(),
                database.daoSalesReport()
            )
            val wasteRepository = WasteLogRepository(database.daoWasteLog())

            // Run synchronously using runBlocking (required for Worker)
            runBlocking {
                productRepository.processExpiredProducts(wasteRepository)
            }

            Log.d(TAG, "✅ Expiration check completed successfully")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.e(TAG, "❌ Expiration check failed: ${e.message}", e)
            Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
            // Retry on failure (WorkManager will retry automatically)
            Result.retry()
        }
    }
}
