package com.project.dba_delatorre_dometita_ramirez_tan

class SalesReportRepository(private val dao: Dao_SalesReport) {
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