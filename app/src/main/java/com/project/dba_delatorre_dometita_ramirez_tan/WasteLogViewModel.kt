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

                android.util.Log.d("WasteLogViewModel", "━━━━━━━━━━━━━━━━━━━━━━━━")
                android.util.Log.d("WasteLogViewModel", "📋 Loaded all waste logs: ${wasteLogsList.size}")
                if (wasteLogsList.isNotEmpty()) {
                    android.util.Log.d("WasteLogViewModel", "   First 5 entries:")
                    wasteLogsList.take(5).forEach { log ->
                        android.util.Log.d("WasteLogViewModel", "     - ${log.productName} (${log.quantity}): ${log.wasteDate}")
                    }
                }
                android.util.Log.d("WasteLogViewModel", "━━━━━━━━━━━━━━━━━━━━━━━━")
            } catch (e: Exception) {
                android.util.Log.e("WasteLogViewModel", "❌ Load failed: ${e.message}", e)
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
                startDate = "$todayStr 00:00:00"
                endDate = "$todayStr 23:59:59"
            }
            "Week" -> {
                val weekAgo = today.minusDays(6)
                startDate = weekAgo.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 00:00:00"
                endDate = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 23:59:59"
            }
            "Month" -> {
                val firstDayOfMonth = today.withDayOfMonth(1)
                startDate = firstDayOfMonth.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 00:00:00"
                endDate = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 23:59:59"
            }
            else -> return
        }

        android.util.Log.d("WasteLogViewModel", "━━━━━━━━━━━━━━━━━━━━━━━━")
        android.util.Log.d("WasteLogViewModel", "🔍 Filtering by: $period")
        android.util.Log.d("WasteLogViewModel", "   Start: $startDate")
        android.util.Log.d("WasteLogViewModel", "   End: $endDate")
        android.util.Log.d("WasteLogViewModel", "   Total logs in wasteLogsList: ${wasteLogsList.size}")

        viewModelScope.launch {
            try {
                val filtered = repository.getWasteLogsByDateRange(startDate, endDate)
                android.util.Log.d("WasteLogViewModel", "   Filtered results: ${filtered.size}")

                if (filtered.isNotEmpty()) {
                    android.util.Log.d("WasteLogViewModel", "   Sample dates from filtered:")
                    filtered.take(3).forEach { log ->
                        android.util.Log.d("WasteLogViewModel", "     - ${log.productName}: ${log.wasteDate}")
                    }
                } else {
                    android.util.Log.w("WasteLogViewModel", "   ⚠️ No results after filtering!")
                    android.util.Log.d("WasteLogViewModel", "   Sample dates from ALL logs:")
                    wasteLogsList.take(3).forEach { log ->
                        android.util.Log.d("WasteLogViewModel", "     - ${log.productName}: ${log.wasteDate}")
                    }
                }

                wasteLogs = filtered
                totalWasteQuantity = filtered.sumOf { it.quantity }
                android.util.Log.d("WasteLogViewModel", "   Total quantity: $totalWasteQuantity")
                android.util.Log.d("WasteLogViewModel", "━━━━━━━━━━━━━━━━━━━━━━━━")
            } catch (e: Exception) {
                android.util.Log.e("WasteLogViewModel", "❌ Filter failed: ${e.message}", e)
                errorMessage = "Failed to filter waste logs: ${e.message}"
            }
        }
    }

    // Filter waste logs by custom date range
    fun filterByDateRange(startDate: String, endDate: String) {
        viewModelScope.launch {
            try {
                // Append time ranges to include full days
                val startDateTime = "$startDate 00:00:00"
                val endDateTime = "$endDate 23:59:59"

                val filtered = repository.getWasteLogsByDateRange(startDateTime, endDateTime)
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
