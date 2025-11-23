package com.project.dba_delatorre_dometita_ramirez_tan

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ProductViewModel(private val repository: ProductRepository) : ViewModel() {

    var productList by mutableStateOf<List<Entity_Products>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    // Load products immediately
    init {
        viewModelScope.launch {
            getAllProducts()
        }
    }

    // ============================ PRODUCT LOAD ============================

    fun getAllProducts() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                val fetchedProducts = repository.getAll()
                productList = fetchedProducts

                if (productList.isEmpty()) {
                    errorMessage = "No products found"
                }
            } catch (e: Exception) {
                errorMessage = e.message
            } finally {
                isLoading = false
            }
        }
    }

    // ============================ CRUD ============================

    fun insertProduct(product: Entity_Products) {
        viewModelScope.launch {
            repository.insert(product)
            getAllProducts()
        }
    }

    fun deleteProduct(product: Entity_Products) {
        viewModelScope.launch {
            repository.delete(product)
            getAllProducts()
        }
    }

    fun updateProduct(product: Entity_Products) {
        viewModelScope.launch {
            repository.update(product)
            getAllProducts()
        }
    }

    fun deductProductStock(productFirebaseId: String, quantity: Int) {
        viewModelScope.launch {
            repository.deductProductStock(productFirebaseId, quantity)
        }
    }

    // ============================ INVENTORY TRANSFER ============================

    fun transferInventory(productFirebaseId: String, quantity: Int, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = repository.transferInventory(productFirebaseId, quantity)
            onResult(result)
            if (result.isSuccess) getAllProducts()
        }
    }
    fun processSale(
        product: Entity_Products,
        quantity: Int,
        paymentMode: String,
        gcashReferenceId: String?,
        cashierUsername: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.processSale(
                product,
                quantity,
                paymentMode,
                gcashReferenceId,
                cashierUsername
            )

            onResult(result.isSuccess)

            if (result.isSuccess) {
                getAllProducts()
            }
        }
    }
}


// ============================ FACTORY ============================

class ProductViewModelFactory(private val repository: ProductRepository) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ProductViewModel(repository) as T
    }
}
