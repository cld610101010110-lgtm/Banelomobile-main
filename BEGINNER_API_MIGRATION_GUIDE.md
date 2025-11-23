# 🎯 REST API Migration Guide - Step by Step for Beginners

**Goal:** Replace Firestore/Firebase with REST API. After this, your app will have **ZERO Firebase/Firestore code**.

---

## 📋 Overview - What We're Doing

```
BEFORE (Your current setup):
Android App ──> Firestore ──> Firebase

AFTER (New setup):
Android App ──> REST API Server ──> PostgreSQL Database
```

The REST API is already built and running on your computer at `http://localhost:3000`

---

## 🚀 PHASE 1: Start the REST API Backend (5 minutes)

### Step 1.1: Open Command Prompt
- Press `Windows Key + R`
- Type: `cmd`
- Press Enter

### Step 1.2: Navigate to API folder
```bash
cd C:\path\to\Banelomobile\api-backend
```
(Replace with your actual folder path)

### Step 1.3: Install dependencies
```bash
npm install
```
(Wait 2-3 minutes)

### Step 1.4: Start the API Server
```bash
npm start
```

**You should see:**
```
✅ Server running on: http://localhost:3000
✅ Connected to PostgreSQL database: banelo_db
```

**✅ Leave this running** (don't close the command prompt!)

---

## 🔧 PHASE 2: Update Android App (20-30 minutes)

### Step 2.0: Open Android Studio
Open your Banelomobile project in Android Studio

---

### Step 2.1: Create API Service Class

**File location:** Right-click on the main package folder → New → Kotlin Class

**Name it:** `BaneloApiService.kt`

**Paste this entire code:**

```kotlin
package com.project.dba_delatorre_dometita_ramirez_tan

import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import com.google.gson.Gson
import com.google.gson.JsonObject

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
    suspend fun getAllRecipes(): ApiResponse<List<Any>>

    @GET("api/recipes/product/{firebaseId}")
    suspend fun getRecipeForProduct(@Path("firebaseId") firebaseId: String): ApiResponse<Any>

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
        return try {
            val response = call()
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.error ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Log.e("BaneloAPI", "API Error: ${e.message}")
            Result.failure(e)
        }
    }
}
```

**Press Ctrl+S to save**

---

### Step 2.2: Update build.gradle (App level)

**File location:** `app/build.gradle.kts`

**Find this section:**
```kotlin
dependencies {
    // Firebase...
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")

    // ... other stuff
}
```

**Replace the Firebase imports with:**
```kotlin
dependencies {
    // ✅ NEW: Retrofit for REST API
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")

    // ❌ DELETE: Remove these
    // implementation("com.google.firebase:firebase-firestore-ktx")
    // implementation("com.google.firebase:firebase-storage-ktx")
    // implementation("com.google.firebase:firebase-auth-ktx")

    // ... keep other stuff
}
```

**Press Ctrl+S to save**

**Then:** Click "Sync Now" at the top of Android Studio

---

### Step 2.3: Update ProductRepository.kt

**File location:** `app/src/main/java/com/project/.../ProductRepository.kt`

**Replace the ENTIRE file with:**

```kotlin
package com.project.dba_delatorre_dometita_ramirez_tan

import android.util.Log
import com.project.dba_delatorre_dometita_ramirez_tan.BaneloApiService.safeCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProductRepository(
    private val daoProducts: Dao_Products,
    private val daoSalesReport: Dao_SalesReport
) {

    private val tag = "ProductRepository"

    // ============ GET ALL PRODUCTS ============
    suspend fun getAllProducts(): List<Entity_Products> {
        return withContext(Dispatchers.IO) {
            try {
                // Try API first
                val result = safeCall {
                    BaneloApiService.api.getAllProducts()
                }

                if (result.isSuccess) {
                    val products = result.getOrNull() ?: return@withContext emptyList()
                    val entities = products.map { convertToEntity(it) }

                    // Cache in Room for offline access
                    daoProducts.deleteAll()
                    daoProducts.insertAll(entities)

                    Log.d(tag, "✅ Loaded ${entities.size} products from API")
                    entities
                } else {
                    // Fallback to Room cache if API fails
                    Log.w(tag, "⚠️ API failed, using cached products")
                    daoProducts.getAll()
                }
            } catch (e: Exception) {
                Log.e(tag, "❌ Error: ${e.message}")
                daoProducts.getAll()
            }
        }
    }

    // ============ GET PRODUCTS BY CATEGORY ============
    suspend fun getProductsByCategory(category: String): List<Entity_Products> {
        return withContext(Dispatchers.IO) {
            try {
                val result = safeCall {
                    BaneloApiService.api.getProductsByCategory(category)
                }

                if (result.isSuccess) {
                    result.getOrNull()?.map { convertToEntity(it) } ?: emptyList()
                } else {
                    daoProducts.getByCategory(category)
                }
            } catch (e: Exception) {
                Log.e(tag, "❌ Error: ${e.message}")
                daoProducts.getByCategory(category)
            }
        }
    }

    // ============ GET SINGLE PRODUCT ============
    suspend fun getProduct(firebaseId: String): Entity_Products? {
        return withContext(Dispatchers.IO) {
            try {
                val result = safeCall {
                    BaneloApiService.api.getProduct(firebaseId)
                }

                result.getOrNull()?.let { convertToEntity(it) }
            } catch (e: Exception) {
                Log.e(tag, "❌ Error: ${e.message}")
                null
            }
        }
    }

    // ============ INSERT PRODUCT ============
    suspend fun insert(product: Entity_Products) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(tag, "➕ Inserting product: ${product.name}")

                // Image handling (if you still use Cloudinary)
                val cloudinaryImageUrl = if (product.imageUri.isNotEmpty()) {
                    product.imageUri // Or upload to Cloudinary if needed
                } else {
                    ""
                }

                val request = ProductRequest(
                    name = product.name,
                    category = product.category,
                    price = product.price,
                    quantity = product.quantity,
                    inventory_a = product.inventory_a,
                    inventory_b = product.inventory_b,
                    cost_per_unit = product.cost_per_unit,
                    imageUri = cloudinaryImageUrl
                )

                val result = safeCall {
                    BaneloApiService.api.createProduct(request)
                }

                if (result.isSuccess) {
                    Log.d(tag, "✅ Product inserted successfully")
                } else {
                    Log.e(tag, "❌ Insert failed")
                }
            } catch (e: Exception) {
                Log.e(tag, "❌ Error: ${e.message}")
            }
        }
    }

    // ============ UPDATE PRODUCT ============
    suspend fun update(product: Entity_Products) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(tag, "✏️ Updating product: ${product.name}")

                val request = ProductRequest(
                    name = product.name,
                    category = product.category,
                    price = product.price,
                    quantity = product.quantity,
                    inventory_a = product.inventory_a,
                    inventory_b = product.inventory_b,
                    cost_per_unit = product.cost_per_unit,
                    imageUri = product.imageUri
                )

                val result = safeCall {
                    BaneloApiService.api.updateProduct(product.firebaseId, request)
                }

                if (result.isSuccess) {
                    Log.d(tag, "✅ Product updated successfully")
                } else {
                    Log.e(tag, "❌ Update failed")
                }
            } catch (e: Exception) {
                Log.e(tag, "❌ Error: ${e.message}")
            }
        }
    }

    // ============ DELETE PRODUCT ============
    suspend fun delete(firebaseId: String) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(tag, "🗑️ Deleting product: $firebaseId")

                val result = safeCall {
                    BaneloApiService.api.deleteProduct(firebaseId)
                }

                if (result.isSuccess) {
                    daoProducts.deleteById(firebaseId)
                    Log.d(tag, "✅ Product deleted successfully")
                } else {
                    Log.e(tag, "❌ Delete failed")
                }
            } catch (e: Exception) {
                Log.e(tag, "❌ Error: ${e.message}")
            }
        }
    }

    // ============ HELPER: Convert API response to Entity ============
    private fun convertToEntity(response: ProductResponse): Entity_Products {
        return Entity_Products(
            firebaseId = response.firebaseId,
            name = response.name,
            category = response.category,
            price = response.price,
            quantity = response.quantity,
            inventory_a = response.inventory_a,
            inventory_b = response.inventory_b,
            cost_per_unit = response.cost_per_unit,
            imageUri = response.imageUri
        )
    }
}
```

**Press Ctrl+S to save**

---

### Step 2.4: Update UserRepository.kt

**File location:** `app/src/main/java/com/project/.../UserRepository.kt`

**Find and replace the Firestore imports:**

**OLD (remove these):**
```kotlin
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
```

**NEW (add these):**
```kotlin
import com.project.dba_delatorre_dometita_ramirez_tan.BaneloApiService.safeCall
import com.google.gson.JsonObject
```

**Find the login function and replace it with:**
```kotlin
suspend fun loginUser(username: String): Entity_User? {
    return withContext(Dispatchers.IO) {
        try {
            Log.d("UserRepository", "🔓 Logging in user: $username")

            val request = LoginRequest(username = username)
            val result = safeCall {
                BaneloApiService.api.login(request)
            }

            if (result.isSuccess) {
                val user = result.getOrNull()
                Log.d("UserRepository", "✅ Login successful")

                return@withContext user?.let {
                    Entity_User(
                        firebaseId = it.firebaseId,
                        fname = it.fname,
                        lname = it.lname,
                        mname = it.mname,
                        username = it.username,
                        role = it.role,
                        status = it.status
                    )
                }
            } else {
                Log.e("UserRepository", "❌ Login failed")
                null
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "❌ Error: ${e.message}")
            null
        }
    }
}
```

**Find the function that gets all users and replace with:**
```kotlin
suspend fun getAllUsers(): List<Entity_User> {
    return withContext(Dispatchers.IO) {
        try {
            val result = safeCall {
                BaneloApiService.api.getAllUsers()
            }

            if (result.isSuccess) {
                result.getOrNull()?.map { userResponse ->
                    Entity_User(
                        firebaseId = userResponse.firebaseId,
                        fname = userResponse.fname,
                        lname = userResponse.lname,
                        mname = userResponse.mname,
                        username = userResponse.username,
                        role = userResponse.role,
                        status = userResponse.status
                    )
                } ?: emptyList()
            } else {
                Log.e("UserRepository", "❌ Failed to fetch users")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "❌ Error: ${e.message}")
            emptyList()
        }
    }
}
```

**Press Ctrl+S to save**

---

### Step 2.5: Update SalesReportRepository.kt

**File location:** `app/src/main/java/com/project/.../SalesReportRepository.kt`

**Find the function that processes sales and replace with:**

```kotlin
suspend fun processSaleWithIngredientDeduction(
    productFirebaseId: String,
    quantity: Int,
    productName: String,
    category: String,
    price: Double,
    paymentMode: String,
    gcashReferenceId: String? = null,
    cashierUsername: String
): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            Log.d("SalesRepo", "💰 Processing sale for: $productName (Qty: $quantity)")

            val request = SalesRequest(
                productFirebaseId = productFirebaseId,
                quantity = quantity,
                productName = productName,
                category = category,
                price = price,
                paymentMode = paymentMode,
                gcashReferenceId = gcashReferenceId,
                cashierUsername = cashierUsername
            )

            val result = safeCall {
                BaneloApiService.api.processSale(request)
            }

            if (result.isSuccess) {
                Log.d("SalesRepo", "✅ Sale processed successfully")
                true
            } else {
                Log.e("SalesRepo", "❌ Sale processing failed")
                false
            }
        } catch (e: Exception) {
            Log.e("SalesRepo", "❌ Error: ${e.message}")
            false
        }
    }
}
```

**Press Ctrl+S to save**

---

### Step 2.6: Update RecipeRepository.kt

**File location:** `app/src/main/java/com/project/.../RecipeRepository.kt`

**Find and replace the Firestore imports:**

**OLD (remove):**
```kotlin
import com.google.firebase.firestore.FirebaseFirestore
```

**NEW (add):**
```kotlin
import com.project.dba_delatorre_dometita_ramirez_tan.BaneloApiService.safeCall
```

**Find the function that gets all recipes:**

```kotlin
suspend fun getAllRecipes(): List<Any> {
    return withContext(Dispatchers.IO) {
        try {
            val result = safeCall {
                BaneloApiService.api.getAllRecipes()
            }

            result.getOrNull() ?: emptyList()
        } catch (e: Exception) {
            Log.e("RecipeRepository", "❌ Error: ${e.message}")
            emptyList()
        }
    }
}
```

**Press Ctrl+S to save**

---

### Step 2.7: Update AuditRepository.kt

**File location:** `app/src/main/java/com/project/.../AuditRepository.kt`

**Find and replace Firestore references:**

```kotlin
import com.project.dba_delatorre_dometita_ramirez_tan.BaneloApiService.safeCall
import com.google.gson.JsonObject

// Replace this:
// private val firestore = FirebaseFirestore.getInstance()

// With this function:
suspend fun createAuditLog(
    action: String,
    description: String,
    username: String
) {
    withContext(Dispatchers.IO) {
        try {
            val logData = JsonObject().apply {
                addProperty("action", action)
                addProperty("description", description)
                addProperty("username", username)
                addProperty("timestamp", System.currentTimeMillis())
            }

            safeCall {
                BaneloApiService.api.createAuditLog(logData)
            }

            Log.d("AuditRepository", "✅ Audit log created")
        } catch (e: Exception) {
            Log.e("AuditRepository", "❌ Error: ${e.message}")
        }
    }
}
```

**Press Ctrl+S to save**

---

### Step 2.8: Update WasteLogRepository.kt

**File location:** `app/src/main/java/com/project/.../WasteLogRepository.kt`

**Replace Firestore code with:**

```kotlin
import com.project.dba_delatorre_dometita_ramirez_tan.BaneloApiService.safeCall
import com.google.gson.JsonObject

// Remove:
// private val firestore = FirebaseFirestore.getInstance()

// Add this function:
suspend fun createWasteLog(
    productFirebaseId: String,
    quantity: Int,
    reason: String,
    costImpact: Double
) {
    withContext(Dispatchers.IO) {
        try {
            val logData = JsonObject().apply {
                addProperty("productFirebaseId", productFirebaseId)
                addProperty("quantity", quantity)
                addProperty("reason", reason)
                addProperty("costImpact", costImpact)
                addProperty("timestamp", System.currentTimeMillis())
            }

            safeCall {
                BaneloApiService.api.createWasteLog(logData)
            }

            Log.d("WasteLogRepository", "✅ Waste log created")
        } catch (e: Exception) {
            Log.e("WasteLogRepository", "❌ Error: ${e.message}")
        }
    }
}
```

**Press Ctrl+S to save**

---

### Step 2.9: Update MainActivity.kt

**File location:** `app/src/main/java/com/project/.../MainActivity.kt`

**Find the Firebase initialization code:**

**OLD (remove this):**
```kotlin
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

// Inside MainActivity:
// Firebase.firestore.collection(...)
// FirebaseAuth.getInstance()
// FirestoreSetup.runCompleteSetup()
```

**If you see these lines, DELETE them:**
```kotlin
FirestoreSetup.runCompleteSetup()
Firebase.firestore.collection("products").get()
```

**Your MainActivity should now just load the UI without Firebase initialization!**

**Press Ctrl+S to save**

---

## ✅ PHASE 3: Verification (5 minutes)

### Step 3.1: Make sure API is still running
- Check the command prompt from **Step 1.4**
- You should still see the server running message
- If it's not running, go back to Step 1.4

### Step 3.2: Build the Android App
- In Android Studio: **Build** → **Build Project**
- Wait for the build to complete
- You should see **Build successful** at the bottom

### Step 3.3: Verify No Firebase Imports

**In Android Studio, press Ctrl+Shift+F to search globally**

**Search for these (you should find NOTHING):**
- `firebase`
- `firestore`
- `FirebaseAuth`
- `FirebaseFirestore`

If you find any, delete those imports from that file.

### Step 3.4: Run the App
- Click **Run** → **Run 'app'**
- Test on Android Emulator
- Try:
  - ✅ Login
  - ✅ View Products
  - ✅ Create a Product
  - ✅ Process a Sale

---

## 🎉 DONE!

After completing all steps:
- ✅ **No Firebase code** in your app
- ✅ **No Firestore imports** anywhere
- ✅ **REST API** handles all data
- ✅ **PostgreSQL** stores everything
- ✅ **Room database** caches offline access

**Your app is now using PostgreSQL through the REST API! 🚀**

---

## 🐛 Troubleshooting

### Problem: "Cannot resolve symbol BaneloApiService"
- **Solution:** Make sure you created `BaneloApiService.kt` in the correct package folder

### Problem: "API Error: Connection refused"
- **Solution:** Make sure the API server is running (check the command prompt from Step 1.4)

### Problem: "Build failed"
- **Solution:**
  1. Click **Build** → **Clean Project**
  2. Wait 30 seconds
  3. Click **Build** → **Build Project**

### Problem: "Firestore is not initialized"
- **Solution:** You probably didn't remove all Firebase code. Search for "firebase" and delete those imports.

### Problem: "Cannot connect to http://10.0.2.2:3000"
- **Solution:** If using a physical device, change the URL in BaneloApiService.kt to your computer's IP

---

## 📝 Next Steps

1. Test all features
2. If any errors, check the Android Studio logcat (View → Tool Windows → Logcat)
3. Read the error message and try to fix it
4. If stuck, share the error message

**That's it! You're done with the migration!** 🎉
