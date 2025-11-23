package com.project.dba_delatorre_dometita_ramirez_tan

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class WasteLogViewModel(
    private val repository: WasteLogRepository
) : ViewModel() {

    var wasteLogsList by mutableStateOf<List<Entity_WasteLog>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        syncAndLoadWasteLogs()
    }

    // Sync from Firebase and load waste logs
    fun syncAndLoadWasteLogs() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                // Sync from Firebase
                repository.syncFromFirebase()

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

    // Sync unsynced logs to Firebase
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
}

class WasteLogViewModelFactory(
    private val repository: WasteLogRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return WasteLogViewModel(repository) as T
    }
}
