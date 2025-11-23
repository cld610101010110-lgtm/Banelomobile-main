package com.project.dba_delatorre_dometita_ramirez_tan

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface Dao_SalesReport {
    @Query("SELECT * FROM sales_report ORDER BY orderDate DESC")
    suspend fun getAllSales(): List<Entity_SalesReport>

    @Query("DELETE FROM sales_report")
    suspend fun clearSalesReport()

    @Insert
    suspend fun insertSale(sale: Entity_SalesReport)

    @Query("SELECT * FROM sales_report WHERE orderDate LIKE '%' || :date || '%' ORDER BY orderDate DESC")
    suspend fun getSalesByDate(date: String): List<Entity_SalesReport>

    // ✅ FIXED: Added DATE() function
    @Query("""
        SELECT * FROM sales_report 
        WHERE DATE(orderDate) BETWEEN :startDate AND :endDate
        ORDER BY orderDate DESC
    """)
    suspend fun getSalesBetweenDates(startDate: String, endDate: String): List<Entity_SalesReport>

    @Query("SELECT SUM(quantity) FROM sales_report")
    suspend fun getTotalSold(): Int?

    @Query("SELECT SUM(quantity * price) FROM sales_report")
    suspend fun getTotalSalesAmount(): Double?

    @Query("SELECT productName FROM sales_report GROUP BY productName ORDER BY SUM(quantity) DESC LIMIT 1")
    suspend fun getBestSeller(): String?

    // ✅ FIXED: This is the one causing the error - removed DATE() since no WHERE clause
    @Query("""
        SELECT productName, 
               SUM(quantity) AS totalSold, 
               SUM(quantity * price) AS totalRevenue
        FROM sales_report
        GROUP BY productName
        ORDER BY totalRevenue DESC
        LIMIT 5
    """)
    suspend fun getTopSales(): List<TopSalesItem>

    // ✅ FIXED: Added DATE() function for date filtering
    @Query("""
        SELECT productName, 
               SUM(quantity) AS totalSold, 
               SUM(quantity * price) AS totalRevenue
        FROM sales_report
        WHERE DATE(orderDate) BETWEEN :startDate AND :endDate
        GROUP BY productName
        ORDER BY totalRevenue DESC
        LIMIT 5
    """)
    suspend fun getTopSalesBetween(startDate: String, endDate: String): List<TopSalesItem>

    // ✅ FIXED: Added DATE() function for date filtering
    @Query("""
        SELECT productName, 
               SUM(quantity) AS totalSold, 
               SUM(quantity * price) AS totalRevenue
        FROM sales_report
        WHERE DATE(orderDate) BETWEEN :startDate AND :endDate
        GROUP BY productName
        ORDER BY totalRevenue DESC
        LIMIT 5
    """)
    suspend fun getTopSalesByDate(startDate: String, endDate: String): List<TopSalesItem>

    @Query("SELECT SUM(quantity) FROM sales_report")
    suspend fun getTotalQuantitySold(): Int?

    @Query("SELECT SUM(quantity * price) FROM sales_report")
    suspend fun getTotalRevenue(): Double?

    @Query("SELECT * FROM sales_report WHERE date(orderDate) = date('now')")
    suspend fun getSalesForToday(): List<Entity_SalesReport>

    @Query("SELECT * FROM sales_report WHERE date(orderDate) >= date('now', '-6 days')")
    suspend fun getSalesForThisWeek(): List<Entity_SalesReport>

    @Query("SELECT * FROM sales_report WHERE strftime('%Y-%m', orderDate) = strftime('%Y-%m', 'now')")
    suspend fun getSalesForThisMonth(): List<Entity_SalesReport>

    // ✅ FIXED: Added DATE() function for date filtering
    @Query("""
        SELECT SUM(quantity) FROM sales_report 
        WHERE DATE(orderDate) BETWEEN :start AND :end
    """)
    suspend fun getTotalQuantitySoldBetween(start: String, end: String): Int?

    // ✅ FIXED: Added DATE() function for date filtering
    @Query("""
        SELECT SUM(price * quantity) FROM sales_report 
        WHERE DATE(orderDate) BETWEEN :start AND :end
    """)
    suspend fun getTotalRevenueBetween(start: String, end: String): Double?
}