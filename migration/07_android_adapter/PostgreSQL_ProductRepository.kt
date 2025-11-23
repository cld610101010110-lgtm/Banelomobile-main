package com.project.dba_delatorre_dometita_ramirez_tan

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ProductRepository - PostgreSQL Version
 *
 * COMPLETE REPLACEMENT for your current Firestore ProductRepository
 *
 * This handles:
 * - All product CRUD operations
 * - Inventory transfers
 * - Sales with ingredient-based deduction
 * - Waste logging
 * - Cloudinary image uploads (kept from original)
 *
 * Copy this file to replace your current ProductRepository.kt
 */
class PostgreSQL_ProductRepository(
    private val daoProducts: Dao_Products,
    private val daoSalesReport: Dao_SalesReport
) {

    private val api = BaneloApi.service

    // ============================================================================
    // PRODUCT CRUD
    // ============================================================================

    /**
     * Get all products
     * Fetches from API and syncs to Room for offline access
     */
    suspend fun getAll(): List<Entity_Products> = withContext(Dispatchers.IO) {
        try {
            Log.d("ProductRepo", "📡 Fetching products from API...")

            val response = api.getAllProducts()

            if (response.isSuccessful && response.body()?.success == true) {
                val products = response.body()?.data ?: emptyList()
                Log.d("ProductRepo", "✅ Loaded ${products.size} products from API")

                // Sync to Room for offline access
                if (products.isNotEmpty()) {
                    daoProducts.insertProducts(products)
                    Log.d("ProductRepo", "✅ Synced to Room database")
                }

                products
            } else {
                Log.e("ProductRepo", "❌ API error: ${response.body()?.error}")
                // Fallback to Room
                daoProducts.getAllProducts()
            }

        } catch (e: Exception) {
            Log.e("ProductRepo", "❌ Network error: ${e.message}", e)
            // Fallback to Room
            daoProducts.getAllProducts()
        }
    }

    /**
     * Get products by category
     */
    suspend fun getProductsByCategory(category: String): List<Entity_Products> = withContext(Dispatchers.IO) {
        try {
            val response = api.getProductsByCategory(category)

            if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.data ?: emptyList()
            } else {
                daoProducts.getProductsByCategory(category)
            }

        } catch (e: Exception) {
            Log.e("ProductRepo", "Error: ${e.message}")
            daoProducts.getProductsByCategory(category)
        }
    }

    /**
     * Insert new product
     */
    suspend fun insert(product: Entity_Products) = withContext(Dispatchers.IO) {
        try {
            Log.d("ProductRepo", "➕ Inserting product: ${product.name}")

            // Upload image to Cloudinary if exists (keep your existing image upload logic)
            val cloudinaryImageUrl = if (product.imageUri.isNotEmpty()) {
                uploadImageToCloudinary(product.imageUri)
            } else {
                ""
            }

            // Create request
            val request = ProductRequest(
                name = product.name,
                category = product.category,
                price = product.price,
                quantity = product.quantity,
                inventory_a = product.inventoryA,
                inventory_b = product.inventoryB,
                cost_per_unit = product.costPerUnit,
                image_uri = cloudinaryImageUrl,
                description = "",
                sku = "SKU-${System.currentTimeMillis()}"
            )

            // Insert via API
            val response = api.createProduct(request)

            if (response.isSuccessful && response.body()?.success == true) {
                val createdProduct = response.body()?.data
                Log.d("ProductRepo", "✅ Product inserted: ${createdProduct?.firebase_id}")

                // Sync to Room
                createdProduct?.let {
                    daoProducts.insertProduct(it.copy(imageUri = cloudinaryImageUrl))
                }
            } else {
                Log.e("ProductRepo", "❌ Insert failed: ${response.body()?.error}")
                throw Exception(response.body()?.error ?: "Failed to insert product")
            }

        } catch (e: Exception) {
            Log.e("ProductRepo", "❌ Insert error: ${e.message}", e)
            throw e
        }
    }

    /**
     * Update product
     */
    suspend fun update(product: Entity_Products) = withContext(Dispatchers.IO) {
        try {
            Log.d("ProductRepo", "📝 Updating product: ${product.name}")

            // Handle image upload (keep your existing logic)
            val cloudinaryImageUrl = when {
                product.imageUri.isEmpty() -> ""
                product.imageUri.startsWith("https://res.cloudinary.com") -> product.imageUri
                product.imageUri.startsWith("content://") || product.imageUri.startsWith("file://") -> {
                    uploadImageToCloudinary(product.imageUri)
                }
                else -> product.imageUri
            }

            // Create request
            val request = ProductRequest(
                name = product.name,
                category = product.category,
                price = product.price,
                quantity = product.quantity,
                inventory_a = product.inventoryA,
                inventory_b = product.inventoryB,
                cost_per_unit = product.costPerUnit,
                image_uri = cloudinaryImageUrl,
                description = ""
            )

            // Update via API
            val response = api.updateProduct(product.firebaseId, request)

            if (response.isSuccessful && response.body()?.success == true) {
                Log.d("ProductRepo", "✅ Product updated")

                // Sync to Room
                daoProducts.updateProduct(product.copy(imageUri = cloudinaryImageUrl))
            } else {
                Log.e("ProductRepo", "❌ Update failed: ${response.body()?.error}")
                throw Exception(response.body()?.error ?: "Failed to update product")
            }

        } catch (e: Exception) {
            Log.e("ProductRepo", "❌ Update error: ${e.message}", e)
            throw e
        }
    }

    /**
     * Delete product (soft delete)
     */
    suspend fun delete(product: Entity_Products) = withContext(Dispatchers.IO) {
        try {
            Log.d("ProductRepo", "🗑️ Deleting product: ${product.name}")

            // Delete image from Cloudinary if exists
            if (product.imageUri.isNotEmpty()) {
                deleteImageFromCloudinary(product.imageUri)
            }

            // Delete via API
            val response = api.deleteProduct(product.firebaseId)

            if (response.isSuccessful && response.body()?.success == true) {
                Log.d("ProductRepo", "✅ Product deleted")

                // Delete from Room
                daoProducts.deleteProduct(product)
            } else {
                Log.e("ProductRepo", "❌ Delete failed: ${response.body()?.error}")
                throw Exception(response.body()?.error ?: "Failed to delete product")
            }

        } catch (e: Exception) {
            Log.e("ProductRepo", "❌ Delete error: ${e.message}", e)
            throw e
        }
    }

    // ============================================================================
    // INVENTORY OPERATIONS
    // ============================================================================

    /**
     * Transfer inventory from A (warehouse) to B (display)
     */
    suspend fun transferInventory(productFirebaseId: String, quantity: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d("ProductRepo", "🔄 Transferring inventory: $quantity units")

            val request = TransferRequest(productFirebaseId, quantity)
            val response = api.transferInventory(request)

            if (response.isSuccessful && response.body()?.success == true) {
                Log.d("ProductRepo", "✅ Inventory transferred")

                // Refresh products from API
                getAll()

                Result.success(Unit)
            } else {
                val error = response.body()?.error ?: "Transfer failed"
                Log.e("ProductRepo", "❌ Transfer error: $error")
                Result.failure(Exception(error))
            }

        } catch (e: Exception) {
            Log.e("ProductRepo", "❌ Transfer error: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ============================================================================
    // SALES OPERATIONS - INGREDIENT-BASED DEDUCTION
    // ============================================================================

    /**
     * Process sale with ingredient-based inventory deduction
     *
     * This is the KEY method that replaces deductProductStock()
     * It deducts INGREDIENTS, not the product itself!
     */
    suspend fun processSaleWithIngredientDeduction(
        product: Entity_Products,
        quantity: Int,
        paymentMode: String,
        gcashReferenceId: String = ""
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d("ProductRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d("ProductRepo", "💰 Processing sale with ingredient deduction")
            Log.d("ProductRepo", "Product: ${product.name}, Qty: $quantity")

            val currentUser = UserSession.getCurrentUser()
            val cashierUsername = currentUser?.username ?: "system"

            // Create sale request
            val saleRequest = SaleRequest(
                productFirebaseId = product.firebaseId,
                quantity = quantity,
                productName = product.name,
                category = product.category,
                price = product.price,
                paymentMode = paymentMode,
                gcashReferenceId = if (paymentMode == "GCash") gcashReferenceId else null,
                cashierUsername = cashierUsername
            )

            // Process via API - this handles:
            // 1. Recording the sale
            // 2. Finding the recipe
            // 3. Deducting each ingredient from inventory
            val response = api.processSaleWithIngredientDeduction(saleRequest)

            if (response.isSuccessful && response.body()?.success == true) {
                val message = response.body()?.message ?: "Sale processed"
                Log.d("ProductRepo", "✅ $message")
                Log.d("ProductRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")

                // Refresh products to show updated inventory
                getAll()

                Result.success(Unit)
            } else {
                val error = response.body()?.error ?: "Sale processing failed"
                Log.e("ProductRepo", "❌ Sale error: $error")
                Log.d("ProductRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
                Result.failure(Exception(error))
            }

        } catch (e: Exception) {
            Log.e("ProductRepo", "❌ Sale error: ${e.message}", e)
            Log.d("ProductRepo", "━━━━━━━━━━━━━━━━━━━━━━━━")
            Result.failure(e)
        }
    }

    /**
     * Get all sales
     */
    suspend fun getAllSales(): List<Entity_SalesReport> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAllSales()

            if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.data ?: emptyList()
            } else {
                daoSalesReport.getAllSales()
            }

        } catch (e: Exception) {
            Log.e("ProductRepo", "Error fetching sales: ${e.message}")
            daoSalesReport.getAllSales()
        }
    }

    /**
     * Clear sales (local only)
     */
    suspend fun clearSales() {
        daoSalesReport.clearSalesReport()
    }

    // ============================================================================
    // IMAGE UPLOAD (Keep your existing Cloudinary logic)
    // ============================================================================

    private suspend fun uploadImageToCloudinary(imageUri: String): String {
        return try {
            if (imageUri.isEmpty()) return ""

            Log.d("ProductRepo", "📤 Uploading to Cloudinary...")
            val uri = android.net.Uri.parse(imageUri)
            val downloadUrl = CloudinaryHelper.uploadImage(uri)
            Log.d("ProductRepo", "✅ Upload successful: $downloadUrl")
            downloadUrl

        } catch (e: Exception) {
            Log.e("ProductRepo", "❌ Cloudinary upload failed: ${e.message}", e)
            ""
        }
    }

    private suspend fun deleteImageFromCloudinary(imageUrl: String): Boolean {
        return try {
            if (imageUrl.isEmpty() || !imageUrl.contains("cloudinary.com")) return false

            Log.d("ProductRepo", "🗑️ Deleting image from Cloudinary...")
            val success = CloudinaryHelper.deleteImage(imageUrl)
            if (success) {
                Log.d("ProductRepo", "✅ Image deleted")
            }
            success

        } catch (e: Exception) {
            Log.e("ProductRepo", "❌ Cloudinary delete failed: ${e.message}", e)
            false
        }
    }

    // ============================================================================
    // CONNECTION TEST
    // ============================================================================

    suspend fun testConnection(): String {
        return try {
            val response = api.getAllProducts()
            if (response.isSuccessful) {
                val count = response.body()?.data?.size ?: 0
                "✅ Connected to API! Found $count products"
            } else {
                "❌ API error: ${response.message()}"
            }
        } catch (e: Exception) {
            "❌ Connection error: ${e.message}"
        }
    }
}

/**
 * HOW TO USE THIS IN YOUR APP:
 *
 * 1. Copy this file to your app package
 *
 * 2. In OrderProcessScreen or wherever you process sales, replace:
 *
 *    OLD:
 *    productRepository.deductProductStock(product.firebaseId, quantity)
 *    productRepository.insertSalesReport(sale)
 *
 *    NEW:
 *    val result = productRepository.processSaleWithIngredientDeduction(
 *        product = selectedProduct,
 *        quantity = quantity,
 *        paymentMode = "Cash" // or "GCash" or "Card"
 *    )
 *
 *    result.onSuccess {
 *        Toast.makeText(context, "Sale processed! Ingredients deducted.", Toast.LENGTH_SHORT).show()
 *    }.onFailure { error ->
 *        Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_LONG).show()
 *    }
 *
 * 3. The rest of your code stays the same!
 *    - viewModel.loadProducts() still works
 *    - Product cards still display
 *    - Everything works as before, just using PostgreSQL instead of Firestore
 */
