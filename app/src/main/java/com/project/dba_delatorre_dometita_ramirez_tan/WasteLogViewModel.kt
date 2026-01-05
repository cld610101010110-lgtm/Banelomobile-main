package com.project.dba_delatorre_dometita_ramirez_tan

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter

class WasteLogViewModel(
    private val repository: WasteLogRepository
) : ViewModel() {

    var wasteLogsList by mutableStateOf<List<Entity_WasteLog>>(emptyList())
        private set

    var wasteLogs by mutableStateOf<List<Entity_WasteLog>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var totalWasteQuantity by mutableStateOf(0)
        private set

    init {
        syncAndLoadWasteLogs()
    }

    // Sync from API and load waste logs
    fun syncAndLoadWasteLogs() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                // Sync from API
                repository.syncFromApi()

                // Load from local database
                wasteLogsList = repository.getAllWasteLogs()

                isLoading = false
            } catch (e: Exception) {
                errorMessage = "Failed to load waste logs: ${e.message}"
                isLoading = false

                // Still try to load from local
                wasteLogsList = repository.getAllWasteLogs()
            }
        }
    }

    // Insert waste log
    fun insertWasteLog(wasteLog: Entity_WasteLog) {
        viewModelScope.launch {
            try {
                repository.insertWasteLog(wasteLog)

                // Refresh the list
                wasteLogsList = repository.getAllWasteLogs()

            } catch (e: Exception) {
                errorMessage = "Failed to record waste: ${e.message}"
            }
        }
    }

    // Get waste logs by product
    fun getWasteLogsByProduct(productId: String) {
        viewModelScope.launch {
            wasteLogsList = repository.getWasteLogsByProduct(productId)
        }
    }

    // Get waste logs by date range
    fun getWasteLogsByDateRange(startDate: String, endDate: String) {
        viewModelScope.launch {
            wasteLogsList = repository.getWasteLogsByDateRange(startDate, endDate)
        }
    }

    // Get waste logs by user
    fun getWasteLogsByUser(username: String) {
        viewModelScope.launch {
            wasteLogsList = repository.getWasteLogsByUser(username)
        }
    }

    // Get total waste for a product
    suspend fun getTotalWasteForProduct(productId: String): Int {
        return repository.getTotalWasteForProduct(productId)
    }

    // Get total waste by date range
    suspend fun getTotalWasteByDateRange(startDate: String, endDate: String): Int {
        return repository.getTotalWasteByDateRange(startDate, endDate)
    }

    // Sync unsynced logs to API
    fun syncUnsyncedLogs() {
        viewModelScope.launch {
            try {
                repository.syncUnsyncedLogs()
            } catch (e: Exception) {
                errorMessage = "Failed to sync waste logs: ${e.message}"
            }
        }
    }

    // Clear all waste logs (admin function)
    fun clearAllWasteLogs() {
        viewModelScope.launch {
            repository.clearAllWasteLogs()
            wasteLogsList = emptyList()
        }
    }

    // Reload all waste logs
    fun reloadWasteLogs() {
        viewModelScope.launch {
            wasteLogsList = repository.getAllWasteLogs()
        }
    }

    // Load all waste logs for waste report screen
    fun loadAllWasteLogs() {
        viewModelScope.launch {
            isLoading = true
            try {
                repository.syncFromApi()
                wasteLogsList = repository.getAllWasteLogs()
                wasteLogs = wasteLogsList
            } catch (e: Exception) {
                errorMessage = "Failed to load waste logs: ${e.message}"
                wasteLogsList = repository.getAllWasteLogs()
                wasteLogs = wasteLogsList
            } finally {
                isLoading = false
            }
        }
    }

    // Filter waste logs by period (Today, Week, Month)
    fun filterByPeriod(period: String) {
        val today = LocalDate.now()
        val startDate: String
        val endDate: String

        when (period) {
            "Today" -> {
                val todayStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                startDate = todayStr
                endDate = todayStr
            }
            "Week" -> {
                val weekAgo = today.minusDays(6)
                startDate = weekAgo.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                endDate = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            }
            "Month" -> {
                val firstDayOfMonth = today.withDayOfMonth(1)
                startDate = firstDayOfMonth.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                endDate = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            }
            else -> return
        }

        viewModelScope.launch {
            try {
                val filtered = repository.getWasteLogsByDateRange(startDate, endDate)
                wasteLogs = filtered
                totalWasteQuantity = filtered.sumOf { it.quantity }
            } catch (e: Exception) {
                errorMessage = "Failed to filter waste logs: ${e.message}"
            }
        }
    }

    // Filter waste logs by custom date range
    fun filterByDateRange(startDate: String, endDate: String) {
        viewModelScope.launch {
            try {
                val filtered = repository.getWasteLogsByDateRange(startDate, endDate)
                wasteLogs = filtered
                totalWasteQuantity = filtered.sumOf { it.quantity }
            } catch (e: Exception) {
                errorMessage = "Failed to filter waste logs: ${e.message}"
            }
        }
    }
}

class WasteLogViewModelFactory(
    private val repository: WasteLogRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return WasteLogViewModel(repository) as T
    }
}
