package com.project.dba_delatorre_dometita_ramirez_tan

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

/**
 * Retrofit API Service for Banelo POS
 *
 * This replaces ALL Firestore calls with REST API calls to your PostgreSQL backend
 *
 * Usage:
 * val api = BaneloApi.service
 * val response = api.getAllProducts()
 */

// ============================================================================
// DATA MODELS
// ============================================================================

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null,
    val message: String? = null
)

data class UserRequest(
    val fname: String,
    val lname: String,
    val mname: String?,
    val username: String,
    val auth_email: String,
    val role: String
)

data class LoginRequest(
    val username: String
)

data class ProductRequest(
    val name: String,
    val category: String,
    val price: Double,
    val quantity: Int,
    val inventory_a: Int,
    val inventory_b: Int,
    val cost_per_unit: Double,
    val image_uri: String?,
    val description: String?,
    val sku: String?
)

data class TransferRequest(
    val firebaseId: String,
    val quantity: Int
)

data class SaleRequest(
    val productFirebaseId: String,
    val quantity: Int,
    val productName: String,
    val category: String,
    val price: Double,
    val paymentMode: String,
    val gcashReferenceId: String?,
    val cashierUsername: String
)

data class RecipeIngredient(
    val ingredientFirebaseId: String,
    val ingredientName: String,
    val quantityNeeded: Double,
    val unit: String
)

data class RecipeRequest(
    val productFirebaseId: String,
    val productName: String,
    val instructions: String,
    val prep_time_minutes: Int,
    val cook_time_minutes: Int,
    val servings: Int,
    val ingredients: List<RecipeIngredient>
)

data class WasteLogRequest(
    val productFirebaseId: String,
    val productName: String,
    val category: String,
    val quantity: Int,
    val reason: String,
    val recordedBy: String
)

data class AuditLogRequest(
    val action: String,
    val table_name: String,
    val record_id: String,
    val user_id: String,
    val changes: Map<String, Any>
)

// ============================================================================
// RETROFIT API INTERFACE
// ============================================================================

interface BaneloApiService {

    // ========================================
    // USERS / ACCOUNTS
    // ========================================

    @GET("api/users")
    suspend fun getAllUsers(): Response<ApiResponse<List<Entity_Users>>>

    @POST("api/users/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<Entity_Users>>

    @POST("api/users")
    suspend fun createUser(@Body request: UserRequest): Response<ApiResponse<Entity_Users>>

    @PUT("api/users/{firebaseId}")
    suspend fun updateUser(
        @Path("firebaseId") firebaseId: String,
        @Body request: UserRequest
    ): Response<ApiResponse<Entity_Users>>

    @DELETE("api/users/{firebaseId}")
    suspend fun deleteUser(@Path("firebaseId") firebaseId: String): Response<ApiResponse<String>>

    // ========================================
    // PRODUCTS / INVENTORY
    // ========================================

    @GET("api/products")
    suspend fun getAllProducts(): Response<ApiResponse<List<Entity_Products>>>

    @GET("api/products/category/{category}")
    suspend fun getProductsByCategory(@Path("category") category: String): Response<ApiResponse<List<Entity_Products>>>

    @GET("api/products/{firebaseId}")
    suspend fun getProduct(@Path("firebaseId") firebaseId: String): Response<ApiResponse<Entity_Products>>

    @POST("api/products")
    suspend fun createProduct(@Body request: ProductRequest): Response<ApiResponse<Entity_Products>>

    @PUT("api/products/{firebaseId}")
    suspend fun updateProduct(
        @Path("firebaseId") firebaseId: String,
        @Body request: ProductRequest
    ): Response<ApiResponse<Entity_Products>>

    @DELETE("api/products/{firebaseId}")
    suspend fun deleteProduct(@Path("firebaseId") firebaseId: String): Response<ApiResponse<String>>

    @POST("api/products/transfer")
    suspend fun transferInventory(@Body request: TransferRequest): Response<ApiResponse<String>>

    // ========================================
    // SALES
    // ========================================

    @GET("api/sales")
    suspend fun getAllSales(): Response<ApiResponse<List<Entity_SalesReport>>>

    @GET("api/sales/range")
    suspend fun getSalesByDateRange(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<ApiResponse<List<Entity_SalesReport>>>

    @POST("api/sales/process")
    suspend fun processSaleWithIngredientDeduction(@Body request: SaleRequest): Response<ApiResponse<Map<String, Any>>>

    // ========================================
    // RECIPES
    // ========================================

    @GET("api/recipes")
    suspend fun getAllRecipes(): Response<ApiResponse<List<Entity_Recipe>>>

    @GET("api/recipes/product/{firebaseId}")
    suspend fun getRecipeByProduct(@Path("firebaseId") firebaseId: String): Response<ApiResponse<Entity_Recipe>>

    @POST("api/recipes")
    suspend fun createRecipe(@Body request: RecipeRequest): Response<ApiResponse<Entity_Recipe>>

    // ========================================
    // AUDIT LOGS
    // ========================================

    @GET("api/audit")
    suspend fun getAuditLogs(@Query("limit") limit: Int = 100): Response<ApiResponse<List<Entity_AuditLog>>>

    @POST("api/audit")
    suspend fun createAuditLog(@Body request: AuditLogRequest): Response<ApiResponse<String>>

    // ========================================
    // WASTE LOGS
    // ========================================

    @GET("api/waste")
    suspend fun getWasteLogs(): Response<ApiResponse<List<Entity_WasteLog>>>

    @POST("api/waste")
    suspend fun createWasteLog(@Body request: WasteLogRequest): Response<ApiResponse<String>>

    // ========================================
    // REPORTS / DASHBOARD
    // ========================================

    @GET("api/reports/sales-summary")
    suspend fun getSalesSummary(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<ApiResponse<List<Map<String, Any>>>>

    @GET("api/reports/top-products")
    suspend fun getTopProducts(@Query("limit") limit: Int = 10): Response<ApiResponse<List<Map<String, Any>>>>

    @GET("api/reports/low-stock")
    suspend fun getLowStockProducts(@Query("threshold") threshold: Int = 10): Response<ApiResponse<List<Entity_Products>>>
}

// ============================================================================
// RETROFIT CLIENT SINGLETON
// ============================================================================

object BaneloApi {

    // Base URL - Change this based on your setup
    // For Android Emulator: use 10.0.2.2
    // For Physical Device: use your computer's IP (e.g., 192.168.1.100)
    private const val BASE_URL = "http://10.0.2.2:3000/"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val service: BaneloApiService by lazy {
        retrofit.create(BaneloApiService::class.java)
    }

    /**
     * Change base URL for physical device
     * Call this in MainActivity if testing on real phone
     */
    fun setBaseUrl(url: String): BaneloApiService {
        return Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BaneloApiService::class.java)
    }
}

// ============================================================================
// EXAMPLE USAGE
// ============================================================================

/**
 * Replace Firestore calls with API calls:
 *
 * BEFORE (Firestore):
 * ```kotlin
 * val firestore = FirebaseFirestore.getInstance()
 * val snapshot = firestore.collection("products").get().await()
 * val products = snapshot.documents.map { it.toObject<Entity_Products>() }
 * ```
 *
 * AFTER (REST API):
 * ```kotlin
 * val response = BaneloApi.service.getAllProducts()
 * if (response.isSuccessful && response.body()?.success == true) {
 *     val products = response.body()?.data ?: emptyList()
 * }
 * ```
 *
 * Example in Repository:
 * ```kotlin
 * class ProductRepository {
 *     suspend fun getAll(): List<Entity_Products> {
 *         return try {
 *             val response = BaneloApi.service.getAllProducts()
 *             if (response.isSuccessful && response.body()?.success == true) {
 *                 response.body()?.data ?: emptyList()
 *             } else {
 *                 emptyList()
 *             }
 *         } catch (e: Exception) {
 *             Log.e("ProductRepo", "Error: ${e.message}")
 *             emptyList()
 *         }
 *     }
 * }
 * ```
 *
 * Example Processing Sale:
 * ```kotlin
 * val saleRequest = SaleRequest(
 *     productFirebaseId = product.firebaseId,
 *     quantity = 2,
 *     productName = product.name,
 *     category = product.category,
 *     price = product.price,
 *     paymentMode = "Cash",
 *     gcashReferenceId = null,
 *     cashierUsername = UserSession.getCurrentUser()?.username ?: "system"
 * )
 *
 * val response = BaneloApi.service.processSaleWithIngredientDeduction(saleRequest)
 * if (response.isSuccessful) {
 *     // Sale processed, ingredients deducted!
 *     Toast.makeText(context, "Sale completed!", Toast.LENGTH_SHORT).show()
 * }
 * ```
 */
