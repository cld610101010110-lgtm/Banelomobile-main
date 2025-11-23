package com.project.dba_delatorre_dometita_ramirez_tan

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter

class SalesReportViewModel(
    private val repository: SalesReportRepository,
    private val productRepository: ProductRepository  // ✅ ADD THIS
) : ViewModel() {

    var salesList by mutableStateOf<List<Entity_SalesReport>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    private val _topSales = mutableStateOf<List<TopSalesItem>>(emptyList())
    val topSales: State<List<TopSalesItem>> = _topSales

    var totalSold by mutableStateOf(0)
        private set

    var totalRevenue by mutableStateOf(0.0)
        private set

    init {
        syncAndLoadSales()
    }

    // ✅ NEW: Sync from Firebase then load sales
    fun syncAndLoadSales() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            // Sync from Firebase
            val result = productRepository.syncAllSalesFromFirebase()

            if (result.isSuccess) {
                // Load sales from Room (now synced with Firebase)
                getAllSales()
                filterByPeriod("Week") // Default to Week view
            } else {
                errorMessage = "Failed to sync sales from Firebase"
                // Still try to load from Room (cached data)
                getAllSales()
                filterByPeriod("Week")
            }

            isLoading = false
        }
    }

    fun getAllSales() {
        viewModelScope.launch {
            salesList = repository.getAll()
        }
    }

    fun insertSale(sale: Entity_SalesReport) {
        viewModelScope.launch {
            repository.insert(sale)
            getAllSales()
        }
    }

    fun clearSales() {
        viewModelScope.launch {
            repository.clear()
            getAllSales()
        }
    }

    fun filterSalesByDate(date: String) {
        viewModelScope.launch {
            salesList = repository.getByDate(date)
        }
    }

    fun filterSalesByRange(startDate: String, endDate: String) {
        viewModelScope.launch {
            salesList = repository.getSalesBetweenDates(startDate, endDate)
        }
    }

    fun loadTopSales() {
        viewModelScope.launch {
            _topSales.value = repository.getTopSales()
        }
    }

    fun filterByPeriod(period: String) {
        val today = LocalDate.now()
        val startDate: String
        val endDate: String

        when (period) {
            "Today" -> {
                // Format: "2025-11-14"
                val todayStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                startDate = todayStr
                endDate = todayStr
            }
            "Week" -> {
                // Last 7 days (including today)
                val weekAgo = today.minusDays(6)
                startDate = weekAgo.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                endDate = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            }
            "Month" -> {
                // From 1st of current month to today
                val firstDayOfMonth = today.withDayOfMonth(1)
                startDate = firstDayOfMonth.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                endDate = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            }
            else -> return
        }

        viewModelScope.launch {
            android.util.Log.d("SalesViewModel", "━━━━━━━━━━━━━━━━━━━━━━━━")
            android.util.Log.d("SalesViewModel", "🔍 Filtering by: $period")
            android.util.Log.d("SalesViewModel", "📅 Start Date: $startDate")
            android.util.Log.d("SalesViewModel", "📅 End Date: $endDate")

            salesList = repository.getSalesBetweenDates(startDate, endDate)
            _topSales.value = repository.getTopSalesByDate(startDate, endDate)
            totalSold = repository.getTotalQuantitySoldBetween(startDate, endDate)
            totalRevenue = repository.getTotalRevenueBetween(startDate, endDate)

            android.util.Log.d("SalesViewModel", "✅ Found ${salesList.size} sales")
            android.util.Log.d("SalesViewModel", "💰 Total Revenue: ₱$totalRevenue")
            android.util.Log.d("SalesViewModel", "📦 Total Sold: $totalSold")
            android.util.Log.d("SalesViewModel", "━━━━━━━━━━━━━━━━━━━━━━━━")
        }
    }

    fun loadTopSalesByDate(startDate: String, endDate: String) {
        viewModelScope.launch {
            _topSales.value = repository.getTopSalesBetween(startDate, endDate)
        }
    }
}

class SalesReportViewModelFactory(
    private val repository: SalesReportRepository,
    private val productRepository: ProductRepository  // ✅ ADD THIS
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SalesReportViewModel(repository, productRepository) as T
    }
}