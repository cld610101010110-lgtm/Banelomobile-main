package com.project.dba_delatorre_dometita_ramirez_tan

import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ============ DATA CLASSES (for API requests/responses) ============

data class ProductRequest(
    val name: String,
    val category: String,
    val price: Double,
    val quantity: Int,
    val inventory_a: Int,
    val inventory_b: Int,
    val cost_per_unit: Double,
    val imageUri: String = ""
)

data class ProductResponse(
    val firebaseId: String,
    val name: String,
    val category: String,
    val price: Double,
    val quantity: Int,
    val inventory_a: Int,
    val inventory_b: Int,
    val cost_per_unit: Double,
    val imageUri: String
)

data class SalesRequest(
    val productFirebaseId: String,
    val quantity: Int,
    val productName: String,
    val category: String,
    val price: Double,
    val paymentMode: String,
    val gcashReferenceId: String? = null,
    val cashierUsername: String
)

data class LoginRequest(
    val username: String
)

data class UserResponse(
    val firebaseId: String,
    val fname: String,
    val lname: String,
    val mname: String,
    val username: String,
    val role: String,
    val status: String
)

data class RecipeResponse(
    val firebaseId: String,
    val productFirebaseId: String,
    val productName: String
)

data class RecipeIngredientResponse(
    val firebaseId: String,
    val ingredientFirebaseId: String,
    val ingredientName: String,
    val quantityNeeded: Double,
    val unit: String
)

// ============ RETROFIT API INTERFACE ============

interface BaneloApiInterface {

    // PRODUCTS
    @GET("api/products")
    suspend fun getAllProducts(): ApiResponse<List<ProductResponse>>

    @GET("api/products/category/{category}")
    suspend fun getProductsByCategory(@Path("category") category: String): ApiResponse<List<ProductResponse>>

    @GET("api/products/{firebaseId}")
    suspend fun getProduct(@Path("firebaseId") firebaseId: String): ApiResponse<ProductResponse>

    @POST("api/products")
    suspend fun createProduct(@Body product: ProductRequest): ApiResponse<ProductResponse>

    @PUT("api/products/{firebaseId}")
    suspend fun updateProduct(
        @Path("firebaseId") firebaseId: String,
        @Body product: ProductRequest
    ): ApiResponse<ProductResponse>

    @DELETE("api/products/{firebaseId}")
    suspend fun deleteProduct(@Path("firebaseId") firebaseId: String): ApiResponse<Any>

    // SALES (with ingredient deduction)
    @POST("api/sales/process")
    suspend fun processSale(@Body sale: SalesRequest): ApiResponse<Any>

    @GET("api/sales")
    suspend fun getAllSales(): ApiResponse<List<Any>>

    // USERS
    @POST("api/users/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<UserResponse>

    @GET("api/users")
    suspend fun getAllUsers(): ApiResponse<List<UserResponse>>

    @POST("api/users")
    suspend fun createUser(@Body user: JsonObject): ApiResponse<UserResponse>

    @PUT("api/users/{firebaseId}")
    suspend fun updateUser(
        @Path("firebaseId") firebaseId: String,
        @Body user: JsonObject
    ): ApiResponse<UserResponse>

    // RECIPES
    @GET("api/recipes")
    suspend fun getAllRecipes(): ApiResponse<List<RecipeResponse>>

    @GET("api/recipes/product/{firebaseId}")
    suspend fun getRecipeForProduct(@Path("firebaseId") firebaseId: String): ApiResponse<RecipeResponse>

    // AUDIT LOGS
    @GET("api/audit")
    suspend fun getAuditLogs(): ApiResponse<List<Any>>

    @POST("api/audit")
    suspend fun createAuditLog(@Body log: JsonObject): ApiResponse<Any>

    // WASTE LOGS
    @GET("api/waste")
    suspend fun getWasteLogs(): ApiResponse<List<Any>>

    @POST("api/waste")
    suspend fun createWasteLog(@Body log: JsonObject): ApiResponse<Any>
}

// ============ API RESPONSE WRAPPER ============

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null
)

// ============ RETROFIT SINGLETON ============

object BaneloApiService {

    // ⚠️ FOR ANDROID EMULATOR: use http://10.0.2.2:3000
    // ⚠️ FOR PHYSICAL DEVICE: use http://YOUR_COMPUTER_IP:3000 (e.g., 192.168.1.100)
    private const val BASE_URL = "http://10.0.2.2:3000/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: BaneloApiInterface = retrofit.create(BaneloApiInterface::class.java)

    suspend fun <T> safeCall(call: suspend () -> ApiResponse<T>): Result<T> {
        return withContext(Dispatchers.IO) {
            try {
                val response = call()
                if (response.success && response.data != null) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(response.error ?: "Unknown error"))
                }
            } catch (e: Exception) {
                Log.e("BaneloAPI", "API Error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
}
