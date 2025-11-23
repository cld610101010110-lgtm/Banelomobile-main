package com.project.dba_delatorre_dometita_ramirez_tan

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Example Usage of PostgreSQLAdapter
 *
 * Copy these examples into your actual app files
 */

// ============================================================================
// EXAMPLE 1: Test Connection in MainActivity
// ============================================================================

class MainActivity_Example : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Test PostgreSQL connection on app start
        lifecycleScope.launch {
            val result = PostgreSQLAdapter.testConnection()

            result.onSuccess { message ->
                android.util.Log.d("MainActivity", message)
                Toast.makeText(this@MainActivity_Example, message, Toast.LENGTH_LONG).show()
            }.onFailure { error ->
                android.util.Log.e("MainActivity", "PostgreSQL error: ${error.message}", error)
                Toast.makeText(
                    this@MainActivity_Example,
                    "Database Error: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

// ============================================================================
// EXAMPLE 2: Load Products in ProductRepository
// ============================================================================

class ProductRepository_Example(
    private val daoProducts: Dao_Products
) {

    /**
     * Get all products from PostgreSQL
     * Falls back to Room if PostgreSQL fails
     */
    suspend fun getAll(): List<Entity_Products> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("ProductRepo", "Fetching products from PostgreSQL...")

                // Get from PostgreSQL
                val products = PostgreSQLAdapter.getAllProducts()

                android.util.Log.d("ProductRepo", "✅ Loaded ${products.size} products from PostgreSQL")

                // Sync to Room for offline access
                if (products.isNotEmpty()) {
                    daoProducts.insertProducts(products)
                    android.util.Log.d("ProductRepo", "✅ Synced to Room database")
                }

                products

            } catch (e: Exception) {
                android.util.Log.e("ProductRepo", "❌ PostgreSQL failed: ${e.message}", e)
                android.util.Log.d("ProductRepo", "⚠️ Falling back to Room database...")

                // Fallback to Room
                val roomProducts = daoProducts.getAllProducts()
                android.util.Log.d("ProductRepo", "✅ Loaded ${roomProducts.size} products from Room")

                roomProducts
            }
        }
    }

    /**
     * Get products by category
     */
    suspend fun getProductsByCategory(category: String): List<Entity_Products> {
        return withContext(Dispatchers.IO) {
            try {
                PostgreSQLAdapter.getProductsByCategory(category)
            } catch (e: Exception) {
                android.util.Log.e("ProductRepo", "Error: ${e.message}")
                // Fallback to Room
                daoProducts.getProductsByCategory(category)
            }
        }
    }

    /**
     * Insert new product
     */
    suspend fun insert(product: Entity_Products) {
        withContext(Dispatchers.IO) {
            try {
                // Insert to PostgreSQL
                val firebaseId = PostgreSQLAdapter.insertProduct(product)
                android.util.Log.d("ProductRepo", "✅ Product inserted to PostgreSQL with ID: $firebaseId")

                // Sync to Room
                val productWithId = product.copy(firebaseId = firebaseId)
                daoProducts.insertProduct(productWithId)
                android.util.Log.d("ProductRepo", "✅ Synced to Room")

            } catch (e: Exception) {
                android.util.Log.e("ProductRepo", "❌ Insert failed: ${e.message}", e)
                throw e
            }
        }
    }

    /**
     * Update product
     */
    suspend fun update(product: Entity_Products) {
        withContext(Dispatchers.IO) {
            try {
                // Update PostgreSQL
                val success = PostgreSQLAdapter.updateProduct(product)

                if (success) {
                    // Update Room
                    daoProducts.updateProduct(product)
                    android.util.Log.d("ProductRepo", "✅ Product updated")
                } else {
                    android.util.Log.w("ProductRepo", "⚠️ Product not found")
                }

            } catch (e: Exception) {
                android.util.Log.e("ProductRepo", "❌ Update failed: ${e.message}", e)
                throw e
            }
        }
    }

    /**
     * Delete product
     */
    suspend fun delete(product: Entity_Products) {
        withContext(Dispatchers.IO) {
            try {
                // Delete from PostgreSQL (soft delete)
                PostgreSQLAdapter.deleteProduct(product.firebaseId)

                // Delete from Room
                daoProducts.deleteProduct(product)

                android.util.Log.d("ProductRepo", "✅ Product deleted")

            } catch (e: Exception) {
                android.util.Log.e("ProductRepo", "❌ Delete failed: ${e.message}", e)
                throw e
            }
        }
    }

    /**
     * Transfer inventory from A to B
     */
    suspend fun transferInventory(productFirebaseId: String, quantity: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            PostgreSQLAdapter.transferInventory(productFirebaseId, quantity)
        }
    }
}

// ============================================================================
// EXAMPLE 3: Process Sale with Ingredient Deduction (OrderProcessScreen)
// ============================================================================

@Composable
fun OrderProcessScreen_Example(
    viewModel: ProductViewModel
) {
    // ... UI code ...

    val processOrder = {
        val currentUser = UserSession.getCurrentUser()

        if (currentUser != null) {
            viewModel.viewModelScope.launch {
                try {
                    // Create sale record
                    val sale = Entity_SalesReport(
                        orderId = 0,  // Auto-generated
                        productName = selectedProduct.name,
                        category = selectedProduct.category,
                        quantity = quantity,
                        price = selectedProduct.price,
                        orderDate = SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss",
                            Locale.getDefault()
                        ).format(Date()),
                        productFirebaseId = selectedProduct.firebaseId,
                        paymentMode = selectedPaymentMode,  // "Cash", "GCash", "Card"
                        gcashReferenceId = gcashReferenceId
                    )

                    android.util.Log.d("OrderProcess", "━━━━━━━━━━━━━━━━━━━━━━━━")
                    android.util.Log.d("OrderProcess", "Processing sale:")
                    android.util.Log.d("OrderProcess", "Product: ${sale.productName}")
                    android.util.Log.d("OrderProcess", "Quantity: ${sale.quantity}")
                    android.util.Log.d("OrderProcess", "Payment: ${sale.paymentMode}")

                    // 🔥 NEW: Process sale with ingredient-based deduction
                    val result = PostgreSQLAdapter.processSaleWithIngredientDeduction(
                        productFirebaseId = selectedProduct.firebaseId,
                        quantitySold = quantity,
                        sale = sale
                    )

                    result.onSuccess { message ->
                        android.util.Log.d("OrderProcess", "✅ $message")
                        android.util.Log.d("OrderProcess", "━━━━━━━━━━━━━━━━━━━━━━━━")

                        // Show success message
                        Toast.makeText(
                            context,
                            "Order processed! Ingredients deducted.",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Refresh products to show updated inventory
                        viewModel.loadProducts()

                        // Clear cart
                        // ... your cart clearing logic ...

                        // Navigate to success screen
                        // navController.navigate("order_success")

                    }.onFailure { error ->
                        android.util.Log.e("OrderProcess", "❌ Error: ${error.message}", error)
                        android.util.Log.d("OrderProcess", "━━━━━━━━━━━━━━━━━━━━━━━━")

                        // Show error message
                        Toast.makeText(
                            context,
                            "Error processing order: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                } catch (e: Exception) {
                    android.util.Log.e("OrderProcess", "❌ Unexpected error: ${e.message}", e)
                    Toast.makeText(
                        context,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } else {
            Toast.makeText(context, "Please log in first", Toast.LENGTH_SHORT).show()
        }
    }

    // ... Button that calls processOrder ...
}

// ============================================================================
// EXAMPLE 4: Load Products in ViewModel
// ============================================================================

class ProductViewModel_Example(
    private val repository: ProductRepository
) : ViewModel() {

    private val _products = MutableStateFlow<List<Entity_Products>>(emptyList())
    val products: StateFlow<List<Entity_Products>> = _products

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /**
     * Load all products from PostgreSQL
     */
    fun loadProducts() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Get from PostgreSQL via repository
                val products = repository.getAll()

                _products.value = products
                android.util.Log.d("ProductViewModel", "✅ Loaded ${products.size} products")

            } catch (e: Exception) {
                android.util.Log.e("ProductViewModel", "❌ Error loading products: ${e.message}", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Load products by category
     */
    fun loadProductsByCategory(category: String) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val products = repository.getProductsByCategory(category)
                _products.value = products

            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Transfer inventory from warehouse to display
     */
    fun transferInventory(productFirebaseId: String, quantity: Int) {
        viewModelScope.launch {
            try {
                val result = repository.transferInventory(productFirebaseId, quantity)

                result.onSuccess {
                    android.util.Log.d("ProductViewModel", "✅ Inventory transferred")
                    // Reload products to show updated inventory
                    loadProducts()
                }.onFailure { error ->
                    android.util.Log.e("ProductViewModel", "❌ Transfer failed: ${error.message}")
                    _error.value = error.message
                }

            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}

// ============================================================================
// EXAMPLE 5: Display Products in Composable Screen
// ============================================================================

@Composable
fun ProductListScreen_Example(
    viewModel: ProductViewModel
) {
    val products by viewModel.products.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // Load products when screen appears
    LaunchedEffect(Unit) {
        viewModel.loadProducts()
    }

    Column {
        if (isLoading) {
            CircularProgressIndicator()
        }

        error?.let { errorMessage ->
            Text(
                text = "Error: $errorMessage",
                color = Color.Red
            )
        }

        LazyColumn {
            items(products) { product ->
                ProductCard(product = product)
            }
        }
    }
}

@Composable
fun ProductCard(product: Entity_Products) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.h6
            )
            Text(text = "Category: ${product.category}")
            Text(text = "Price: ₱${product.price}")
            Text(text = "Inventory A: ${product.inventoryA}")
            Text(text = "Inventory B: ${product.inventoryB}")
            Text(
                text = "Total: ${product.quantity}",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ============================================================================
// SUMMARY: Migration Checklist
// ============================================================================

/**
 * STEP-BY-STEP MIGRATION:
 *
 * 1. ✅ Add PostgreSQL dependency to build.gradle
 *    implementation 'org.postgresql:postgresql:42.7.1'
 *
 * 2. ✅ Copy PostgreSQLAdapter.kt to your project
 *
 * 3. ✅ Add internet permission to AndroidManifest.xml
 *
 * 4. ✅ Configure network security config (Android 9+)
 *
 * 5. ✅ Update ProductRepository to use PostgreSQLAdapter
 *    - Replace Firestore calls with PostgreSQLAdapter calls
 *    - Keep Room as fallback
 *
 * 6. ✅ Update OrderProcessScreen to use ingredient deduction
 *    - Replace deductProductStock() with processSaleWithIngredientDeduction()
 *
 * 7. ✅ Test connection in MainActivity
 *
 * 8. ✅ Test product loading
 *
 * 9. ✅ Test sale processing with ingredient deduction
 *
 * 10. ✅ Update all other screens (inventory transfer, sales reports, etc.)
 *
 * DONE! Your app now uses PostgreSQL with ingredient-based inventory!
 */
