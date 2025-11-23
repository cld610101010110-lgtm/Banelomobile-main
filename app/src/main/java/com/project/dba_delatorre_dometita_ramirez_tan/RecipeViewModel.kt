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

    // Sync recipes from Firebase (called on app start or manually)
    fun syncRecipes() {
        viewModelScope.launch {
            isSyncing = true
            syncMessage = "Syncing recipes from server..."

            repository.syncRecipesFromFirebase()
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

    // Calculate how many servings can be made for a product
    suspend fun getAvailableQuantity(productFirebaseId: String): Int {
        return repository.calculateMaxServings(productFirebaseId)
    }

    // Deduct ingredients when order is completed
    fun processOrder(productFirebaseId: String, quantity: Int, saveToSales: (Entity_SalesReport) -> Unit) {
        viewModelScope.launch {
            repository.deductIngredients(productFirebaseId, quantity, saveToSales)
        }
    }

    // Calculate recipe cost breakdown
    suspend fun getRecipeCost(productFirebaseId: String): RecipeRepository.RecipeCostSummary? {
        return repository.calculateRecipeCost(productFirebaseId)
    }
}

class RecipeViewModelFactory(private val repository: RecipeRepository) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RecipeViewModel(repository) as T
    }
}