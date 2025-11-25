package com.project.dba_delatorre_dometita_ramirez_tan

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SalesReportRepository(private val dao: Dao_SalesReport) {
    private val tag = "SalesReportRepository"

    // ============ SYNC SALES FROM API ============
    suspend fun syncSalesFromApi(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(tag, "━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(tag, "📡 Fetching sales from API...")

                val result = BaneloApiService.safeCall {
                    BaneloApiService.api.getAllSales()
                }

                if (result.isSuccess) {
                    val salesResponses = result.getOrNull() ?: return@withContext Result.failure(Exception("No sales data"))
                    Log.d(tag, "✅ API returned ${salesResponses.size} sales records")

                    // Convert API response to Room entities
                    val entities = salesResponses.mapNotNull { convertToEntity(it) }

                    // Clear old data and insert new
                    dao.clearSalesReport()
                    entities.forEach { dao.insertSale(it) }

                    Log.d(tag, "✅ Synced ${entities.size} sales to local database")
                    Log.d(tag, "━━━━━━━━━━━━━━━━━━━━━━━━")
                    Result.success(Unit)
                } else {
                    Log.w(tag, "⚠️ API failed: ${result.exceptionOrNull()?.message}")
                    Log.d(tag, "━━━━━━━━━━━━━━━━━━━━━━━━")
                    Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
                }
            } catch (e: Exception) {
                Log.e(tag, "❌ Error syncing sales: ${e.message}", e)
                Log.d(tag, "━━━━━━━━━━━━━━━━━━━━━━━━")
                Result.failure(e)
            }
        }
    }

    // ============ HELPER: Convert API response to Entity ============
    private fun convertToEntity(response: SalesResponse): Entity_SalesReport? {
        return try {
            Entity_SalesReport(
                orderId = 0, // Room will auto-generate
                productName = response.productName ?: "",
                category = response.category ?: "",
                quantity = response.quantity ?: 0,
                price = response.price ?: 0.0,
                orderDate = response.orderDate ?: "",
                productFirebaseId = response.productFirebaseId ?: "",
                paymentMode = response.paymentMode ?: "Cash",
                gcashReferenceId = response.gcashReferenceId ?: ""
            )
        } catch (e: Exception) {
            Log.e(tag, "❌ Failed to convert sales response: ${e.message}", e)
            null
        }
    }

    // ============ EXISTING METHODS ============
    suspend fun insert(sale: Entity_SalesReport) = dao.insertSale(sale)
    suspend fun getAll(): List<Entity_SalesReport> = dao.getAllSales()
    suspend fun clear() = dao.clearSalesReport()
    suspend fun getByDate(date: String): List<Entity_SalesReport> = dao.getSalesByDate(date)
    suspend fun getSalesBetweenDates(startDate: String, endDate: String): List<Entity_SalesReport> {
        return dao.getSalesBetweenDates(startDate, endDate)
    }
    suspend fun getTopSalesByDate(startDate: String, endDate: String): List<TopSalesItem> {
        return dao.getTopSalesByDate(startDate, endDate)
    }
    suspend fun getTotalQuantitySoldBetween(start: String, end: String): Int {
        return dao.getTotalQuantitySoldBetween(start, end) ?: 0
    }

    suspend fun getTotalRevenueBetween(start: String, end: String): Double {
        return dao.getTotalRevenueBetween(start, end) ?: 0.0
    }

    suspend fun getTopSales(): List<TopSalesItem> {
        return dao.getTopSales()
    }

    suspend fun getTopSalesBetween(startDate: String, endDate: String): List<TopSalesItem> {
        return dao.getTopSalesBetween(startDate, endDate)
    }
}