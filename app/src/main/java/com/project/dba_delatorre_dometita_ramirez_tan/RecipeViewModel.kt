package com.project.dba_delatorre_dometita_ramirez_tan

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class RecipeViewModel(private val repository: RecipeRepository) : ViewModel() {

    var recipesList by mutableStateOf<List<RecipeWithIngredients>>(emptyList())
        private set

    var isSyncing by mutableStateOf(false)
        private set

    var syncMessage by mutableStateOf("")
        private set

    init {
        syncRecipes()
    }

    // Sync recipes from API (called on app start or manually)
    fun syncRecipes() {
        viewModelScope.launch {
            isSyncing = true
            syncMessage = "Syncing recipes from server..."

            repository.syncRecipesFromApi()
                .onSuccess {
                    recipesList = repository.getAllRecipesWithIngredients()
                    syncMessage = "Recipes synced successfully"
                }
                .onFailure { error ->
                    syncMessage = "Sync failed: ${error.message}"
                }

            isSyncing = false
        }
    }

    // Calculate how many servings can be made for a product (using TOTAL stock: inventoryA + inventoryB)
    suspend fun getAvailableQuantity(productId: String): Int {
        return repository.calculateMaxServings(productId)
    }

    // ✅ NEW METHOD: Calculate servings based ONLY on Inventory B
    suspend fun getAvailableQuantityFromInventoryB(productId: String): Int {
        return repository.calculateMaxServingsFromInventoryB(productId)
    }

    // Deduct ingredients when order is completed
    fun processOrder(productId: String, quantity: Int, saveToSales: (Entity_SalesReport) -> Unit) {
        viewModelScope.launch {
            repository.deductIngredients(productId, quantity, saveToSales)
        }
    }

    // Calculate recipe cost breakdown
    suspend fun getRecipeCost(productId: String): RecipeRepository.RecipeCostSummary? {
        return repository.calculateRecipeCost(productId)
    }
}

class RecipeViewModelFactory(private val repository: RecipeRepository) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RecipeViewModel(repository) as T
    }
}