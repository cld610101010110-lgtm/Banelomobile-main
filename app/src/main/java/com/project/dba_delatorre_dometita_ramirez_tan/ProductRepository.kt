package com.project.dba_delatorre_dometita_ramirez_tan

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProductRepository(
    private val daoProducts: Dao_Products,
    private val daoSalesReport: Dao_SalesReport
) {
    private val tag = "ProductRepository"

    // ============ IMAGE UPLOAD (TO CLOUDINARY) ============
    // Accept nullable imageUri and handle gracefully inside the function.
    private suspend fun uploadImageToCloudinary(imageUri: String?): String {
        return try {
            if (imageUri.isNullOrEmpty()) {
                Log.w(tag, "⚠️ No image provided for upload")
                return ""
            }

            Log.d(tag, "━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(tag, "📤 Uploading to Cloudinary...")
            Log.d(tag, "Input URI: $imageUri")

            val uri = Uri.parse(imageUri)
            val downloadUrl = CloudinaryHelper.uploadImage(uri)

            Log.d(tag, "✅ Upload successful!")
            Log.d(tag, "🔗 Cloudinary URL: $downloadUrl")
            Log.d(tag, "━━━━━━━━━━━━━━━━━━━━━━━━")

            downloadUrl ?: ""
        } catch (e: Exception) {
            Log.e(tag, "━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.e(tag, "❌ Cloudinary upload failed!")
            Log.e(tag, "Error: ${e.message}", e)
            Log.e(tag, "━━━━━━━━━━━━━━━━━━━━━━━━")
            ""
        }
    }

    private suspend fun deleteImageFromCloudinary(imageUrl: String?): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (imageUrl.isNullOrEmpty()) {
                    Log.d(tag, "⚠️ No image to delete")
                    return@withContext false
                }

                if (!imageUrl.contains("cloudinary.com")) {
                    Log.d(tag, "⚠️ Not a Cloudinary URL, skipping delete")
                    return@withContext false
                }

                Log.d(tag, "━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(tag, "🗑️ Deleting image from Cloudinary...")
                Log.d(tag, "Image URL: $imageUrl")

                val success = CloudinaryHelper.deleteImage(imageUrl)

                if (success) {
                    Log.d(tag, "✅ Image deleted successfully!")
                } else {
                    Log.w(tag, "⚠️ Image deletion failed or not found")
                }

                Log.d(tag, "━━━━━━━━━━━━━━━━━━━━━━━━")

                success
            } catch (e: Exception) {
                Log.e(tag, "━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.e(tag, "❌ Cloudinary delete failed!")
                Log.e(tag, "Error: ${e.message}", e)
                Log.e(tag, "━━━━━━━━━━━━━━━━━━━━━━━━")
                false
            }
        }
    }

    // ============ INSERT PRODUCT (via API) ============

    suspend fun insert(product: Entity_Products) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(tag, "➕ Inserting product: ${product.name}")

                // Step 1: Upload image if exists (only when non-null & non-empty)
                val cloudinaryImageUrl = if (!product.image_uri.isNullOrEmpty()) {
                    uploadImageToCloudinary(product.image_uri)
                } else {
                    ""
                }

                // Step 2: Create request
                val request = ProductRequest(
                    name = product.name,
                    category = product.category,
                    price = product.price,
                    quantity = product.quantity,
                    inventory_a = product.inventoryA,
                    inventory_b = product.inventoryB,
                    cost_per_unit = product.costPerUnit,
                    image_uri = cloudinaryImageUrl
                )

                // Step 3: Call API
                val result = BaneloApiService.safeCall {
                    BaneloApiService.api.createProduct(request)
                }

                if (result.isSuccess) {
                    val response = result.getOrNull()
                    Log.d(tag, "✅ Product inserted via API with ID: ${response?.firebaseId}")
                } else {
                    Log.e(tag, "❌ Insert failed: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(tag, "❌ Insert failed: ${e.message}", e)
            }
        }
    }

    // ============ FETCH ALL PRODUCTS ============

    suspend fun getAll(): List<Entity_Products> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(tag, "━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(tag, "📡 Fetching products from API...")

                val result = BaneloApiService.safeCall {
                    BaneloApiService.api.getAllProducts()
                }

                if (result.isSuccess) {
                    val products = result.getOrNull() ?: return@withContext emptyList()
                    Log.d(tag, "✅ API returned ${products.size} products")

                    val entities = products.map { convertToEntity(it) }

                    // Cache in Room for offline access
                    daoProducts.clearAllProducts()
                    daoProducts.insertProducts(entities)

                    Log.d(tag, "✅ Synced to Room database")
                    Log.d(tag, "━━━━━━━━━━━━━━━━━━━━━━━━")
                    entities
                } else {
                    Log.w(tag, "⚠️ API failed, using cached products")
                    Log.d(tag, "━━━━━━━━━━━━━━━━━━━━━━━━")
                    daoProducts.getAllProducts()
                }
            } catch (e: Exception) {
                Log.e(tag, "❌ Error: ${e.message}", e)
                daoProducts.getAllProducts()
            }
        }
    }

    // ============ GET PRODUCTS BY CATEGORY ============

    suspend fun getProductsByCategory(category: String): List<Entity_Products> {
        return withContext(Dispatchers.IO) {
            try {
                val result = BaneloApiService.safeCall {
                    BaneloApiService.api.getProductsByCategory(category)
                }

                if (result.isSuccess) {
                    val products = result.getOrNull() ?: return@withContext emptyList()
                    products.map { convertToEntity(it) }
                } else {
                    Log.w(tag, "⚠️ API failed, using cached products")
                    daoProducts.getByCategory(category)
                }
            } catch (e: Exception) {
                Log.e(tag, "❌ Error: ${e.message}", e)
                daoProducts.getByCategory(category)
            }
        }
    }

    // ============ GET SINGLE PRODUCT ============

    suspend fun getProduct(firebaseId: String): Entity_Products? {
        return withContext(Dispatchers.IO) {
            try {
                val result = BaneloApiService.safeCall {
                    BaneloApiService.api.getProduct(firebaseId)
                }

                result.getOrNull()?.let { convertToEntity(it) }
            } catch (e: Exception) {
                Log.e(tag, "❌ Error: ${e.message}", e)
                null
            }
        }
    }

    // ============ UPDATE PRODUCT ============

    suspend fun update(product: Entity_Products) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(tag, "━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(tag, "✏️ Updating product: ${product.name}")

                // Handle image upload if needed (null-safe)
                val cloudinaryImageUrl = when {
                    product.image_uri.isNullOrEmpty() -> {
                        // No image provided
                        ""
                    }
                    product.image_uri!!.startsWith("https://res.cloudinary.com") -> {
                        // Already a Cloudinary URL — keep it
                        product.image_uri
                    }
                    product.image_uri!!.startsWith("content://") ||
                            product.image_uri!!.startsWith("file://") ||
                            product.image_uri!!.contains("/data/user/") -> {
                        Log.d(tag, "🆕 Local image detected - uploading...")
                        try {
                            uploadImageToCloudinary(product.image_uri)
                        } catch (e: Exception) {
                            Log.e(tag, "❌ Image upload failed during update: ${e.message}", e)
                            ""
                        }
                    }
                    else -> {
                        // Some other external URL — keep it
                        product.image_uri
                    }
                }

                val request = ProductRequest(
                    name = product.name,
                    category = product.category,
                    price = product.price,
                    quantity = product.quantity,
                    inventory_a = product.inventoryA,
                    inventory_b = product.inventoryB,
                    cost_per_unit = product.costPerUnit,
                    image_uri = cloudinaryImageUrl
                )

                val result = BaneloApiService.safeCall {
                    BaneloApiService.api.updateProduct(product.firebaseId, request)
                }

                if (result.isSuccess) {
                    Log.d(tag, "✅ Product updated successfully")
                } else {
                    Log.e(tag, "❌ Update failed: ${result.exceptionOrNull()?.message}")
                }
                Log.d(tag, "━━━━━━━━━━━━━━━━━━━━━━━━")
            } catch (e: Exception) {
                Log.e(tag, "❌ Error: ${e.message}", e)
            }
        }
    }

    // ============ DELETE PRODUCT ============

    suspend fun delete(product: Entity_Products) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(tag, "🗑️ Deleting product: ${product.name}")

                // Delete image from Cloudinary first (only if present)
                if (!product.image_uri.isNullOrEmpty()) {
                    Log.d(tag, "🖼️ Deleting product image...")
                    try {
                        deleteImageFromCloudinary(product.image_uri)
                    } catch (e: Exception) {
                        Log.e(tag, "❌ Failed to delete image during product delete: ${e.message}", e)
                    }
                }

                // Delete from API
                val result = BaneloApiService.safeCall {
                    BaneloApiService.api.deleteProduct(product.firebaseId)
                }

                if (result.isSuccess) {
                    // Delete from Room
                    daoProducts.deleteProduct(product)
                    Log.d(tag, "✅ Product deleted successfully!")
                } else {
                    Log.e(tag, "❌ Delete failed: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(tag, "❌ Error: ${e.message}", e)
            }
        }
    }

    // ============ DEDUCT PRODUCT STOCK (DUAL INVENTORY) ============

    suspend fun deductProductStock(productFirebaseId: String, quantity: Int) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(tag, "━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(tag, "📉 Deducting product stock...")
                Log.d(tag, "Product Firebase ID: $productFirebaseId")
                Log.d(tag, "Quantity to deduct: $quantity")

                val product = daoProducts.getProductByFirebaseId(productFirebaseId)

                if (product == null) {
                    Log.w(tag, "⚠️ Product not found!")
                    Log.d(tag, "━━━━━━━━━━━━━━━━━━━━━━━━")
                    return@withContext
                }

                Log.d(tag, "📦 Product found: ${product.name}")
                Log.d(tag, "   Before - Inventory A: ${product.inventoryA}, Inventory B: ${product.inventoryB}")

                var remainingToDeduct = quantity
                var newInventoryA = product.inventoryA
                var newInventoryB = product.inventoryB

                // Deduct from Inventory B first
                if (newInventoryB > 0) {
                    val deductFromB = minOf(remainingToDeduct, newInventoryB)
                    newInventoryB -= deductFromB
                    remainingToDeduct -= deductFromB
                    Log.d(tag, "   Deducted $deductFromB from Inventory B")
                }

                // If still need more, deduct from Inventory A
                if (remainingToDeduct > 0 && newInventoryA > 0) {
                    val deductFromA = minOf(remainingToDeduct, newInventoryA)
                    newInventoryA -= deductFromA
                    remainingToDeduct -= deductFromA
                    Log.d(tag, "   Deducted $deductFromA from Inventory A")
                }

                val newQuantity = newInventoryA + newInventoryB

                Log.d(tag, "   After - Inventory A: $newInventoryA, Inventory B: $newInventoryB, Total: $newQuantity")

                // Update Room
                val updatedProduct = product.copy(
                    quantity = newQuantity,
                    inventoryA = newInventoryA,
                    inventoryB = newInventoryB
                )
                daoProducts.updateProduct(updatedProduct)

                // Update API
                val request = ProductRequest(
                    name = updatedProduct.name,
                    category = updatedProduct.category,
                    price = updatedProduct.price,
                    quantity = updatedProduct.quantity,
                    inventory_a = updatedProduct.inventoryA,
                    inventory_b = updatedProduct.inventoryB,
                    cost_per_unit = updatedProduct.costPerUnit,
                    image_uri = updatedProduct.image_uri ?: ""
                )

                val apiResult = BaneloApiService.safeCall {
                    BaneloApiService.api.updateProduct(productFirebaseId, request)
                }

                // ✅ Log success for local operation and API sync status
                Log.d(tag, "✅ Stock deducted successfully (local)")
                if (apiResult.isSuccess) {
                    Log.d(tag, "✅ API synced successfully")
                } else {
                    Log.w(tag, "⚠️ API sync failed: ${apiResult.exceptionOrNull()?.message}")
                    Log.w(tag, "⚠️ Changes saved locally, will retry sync later")
                }
                Log.d(tag, "━━━━━━━━━━━━━━━━━━━━━━━━")
            } catch (e: Exception) {
                Log.e(tag, "━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.e(tag, "❌ Failed to deduct stock!")
                Log.e(tag, "Error: ${e.message}", e)
                Log.e(tag, "━━━━━━━━━━━━━━━━━━━━━━━━")
            }
        }
    }

    // ============ TRANSFER INVENTORY (A → B) ============

    suspend fun transferInventory(productFirebaseId: String, quantity: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(tag, "━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(tag, "🔄 Transferring inventory A → B...")
                Log.d(tag, "Product Firebase ID: $productFirebaseId")
                Log.d(tag, "Quantity to transfer: $quantity")

                val product = daoProducts.getProductByFirebaseId(productFirebaseId)

                if (product == null) {
                    Log.w(tag, "⚠️ Product not found!")
                    Log.d(tag, "━━━━━━━━━━━━━━━━━━━━━━━━")
                    return@withContext Result.failure(Exception("Product not found"))
                }

                if (product.inventoryA < quantity) {
                    Log.w(tag, "⚠️ Insufficient stock in Inventory A")
                    Log.d(tag, "   Available: ${product.inventoryA}, Requested: $quantity")
                    Log.d(tag, "━━━━━━━━━━━━━━━━━━━━━━━━")
                    return@withContext Result.failure(Exception("Insufficient stock in Inventory A"))
                }

                Log.d(tag, "📦 Product: ${product.name}")
                Log.d(tag, "   Before - Inventory A: ${product.inventoryA}, Inventory B: ${product.inventoryB}")

                val newInventoryA = product.inventoryA - quantity
                val newInventoryB = product.inventoryB + quantity

                Log.d(tag, "   After - Inventory A: $newInventoryA, Inventory B: $newInventoryB")

                // Update Room
                val updatedProduct = product.copy(
                    inventoryA = newInventoryA,
                    inventoryB = newInventoryB
                )
                daoProducts.updateProduct(updatedProduct)

                // Update API
                val request = ProductRequest(
                    name = updatedProduct.name,
                    category = updatedProduct.category,
                    price = updatedProduct.price,
                    quantity = updatedProduct.quantity,
                    inventory_a = newInventoryA,
                    inventory_b = newInventoryB,
                    cost_per_unit = updatedProduct.costPerUnit,
                    image_uri = updatedProduct.image_uri ?: ""
                )

                val result = BaneloApiService.safeCall {
                    BaneloApiService.api.updateProduct(productFirebaseId, request)
                }

                if (result.isSuccess) {
                    Log.d(tag, "✅ Inventory transferred successfully")
                    Log.d(tag, "━━━━━━━━━━━━━━━━━━━━━━━━")
                    Result.success(Unit)
                } else {
                    Result.failure(result.exceptionOrNull() ?: Exception("Transfer failed"))
                }
            } catch (e: Exception) {
                Log.e(tag, "━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.e(tag, "❌ Failed to transfer inventory!")
                Log.e(tag, "Error: ${e.message}", e)
                Log.e(tag, "━━━━━━━━━━━━━━━━━━━━━━━━")
                Result.failure(e)
            }
        }
    }

    // ============ SALES OPERATIONS ============

    suspend fun getAllSales(): List<Entity_SalesReport> {
        return daoSalesReport.getAllSales()
    }

    suspend fun clearSales() {
        daoSalesReport.clearSalesReport()
    }

    suspend fun processSale(
        product: Entity_Products,
        quantity: Int,
        paymentMode: String,
        gcashReferenceId: String?,
        cashierUsername: String
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(tag, "━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(tag, "💰 Processing sale...")
                Log.d(tag, "Product: ${product.name} (${product.category})")
                Log.d(tag, "Quantity: $quantity")
                Log.d(tag, "Payment: $paymentMode")

                val request = SalesRequest(
                    productFirebaseId = product.firebaseId,
                    quantity = quantity,
                    productName = product.name,
                    category = product.category,
                    price = product.price,
                    paymentMode = paymentMode,
                    gcashReferenceId = gcashReferenceId,
                    cashierUsername = cashierUsername
                )

                val result = BaneloApiService.safeCall {
                    BaneloApiService.api.processSale(request)
                }

                if (result.isSuccess) {
                    Log.d(tag, "✅ Sale processed successfully!")
                    Log.d(tag, "━━━━━━━━━━━━━━━━━━━━━━━━")
                    Result.success(Unit)
                } else {
                    val error = result.exceptionOrNull()
                    Log.e(tag, "❌ Sale failed: ${error?.message}")
                    Log.e(tag, "━━━━━━━━━━━━━━━━━━━━━━━━")
                    Result.failure(error ?: Exception("Unknown error"))
                }

            } catch (e: Exception) {
                Log.e(tag, "❌ Exception in processSale: ${e.message}", e)
                Log.e(tag, "━━━━━━━━━━━━━━━━━━━━━━━━")
                Result.failure(e)
            }
        }
    }

    // ============ HELPER: Convert API response to Entity ============

    private fun convertToEntity(response: ProductResponse): Entity_Products {
        return Entity_Products(
            firebaseId = response.firebaseId ?: "",
            name = response.name ?: "",
            category = response.category ?: "",
            price = response.price ?: 0.0,
            quantity = response.quantity ?: 0,
            inventoryA = response.inventory_a ?: 0,
            inventoryB = response.inventory_b ?: 0,
            costPerUnit = response.cost_per_unit ?: 0.0,
            image_uri = response.image_uri // nullable allowed
        )
    }
}
