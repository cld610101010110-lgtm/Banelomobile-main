# Android App Setup - REST API Integration

Complete guide to replace Firestore with PostgreSQL REST API in your Android app.

**NO MORE JDBC/Netty errors!** ✅

---

## 📋 Step 1: Update build.gradle Dependencies

### Remove Problematic Dependencies

**REMOVE these lines from `app/build.gradle.kts`:**

```gradle
// ❌ REMOVE THESE:
implementation("com.impossibl.pgjdbc-ng:pgjdbc-ng:0.8.9")
implementation("io.netty:netty-all:4.1.100.Final")

// ❌ ALSO REMOVE (or comment out for now):
implementation("com.google.firebase:firebase-firestore-ktx")
implementation("com.google.firebase:firebase-storage")
// Keep firebase-auth if you're using Firebase Authentication
```

### Add Retrofit Dependencies

**ADD these instead:**

```gradle
dependencies {
    // ... your existing dependencies ...

    // ✅ Retrofit for REST API
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // ✅ OkHttp for networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // ✅ Coroutines (should already have)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Keep your existing dependencies:
    // - Room
    // - Compose
    // - Navigation
    // - Cloudinary
    // etc.
}
```

### Remove Packaging Block

**REMOVE this entire block:**

```gradle
// ❌ DELETE THIS:
packaging {
    resources {
        excludes.addAll(
            listOf(
                "META-INF/INDEX.LIST",
                // ... etc
            )
        )
    }
}
```

You don't need it anymore!

### Sync Gradle

Click **"Sync Now"** in Android Studio.

The project should sync successfully! ✅

---

## 📁 Step 2: Copy API Files to Your Project

Copy these files from `migration/07_android_adapter/` to your app package:

```
migration/07_android_adapter/BaneloApiService.kt
  → app/src/main/java/com/project/dba_delatorre_dometita_ramirez_tan/

migration/07_android_adapter/PostgreSQL_ProductRepository.kt
  → app/src/main/java/com/project/dba_delatorre_dometita_ramirez_tan/
```

---

## 🔧 Step 3: Update ProductRepository

**Option A: Replace Entirely (Recommended)**

1. **Rename your current file:**
   - `ProductRepository.kt` → `ProductRepository_Firestore_OLD.kt`

2. **Rename the new file:**
   - `PostgreSQL_ProductRepository.kt` → `ProductRepository.kt`

3. **Done!** All your existing code will work.

**Option B: Keep Both (For Testing)**

1. Keep both files
2. In your ViewModel, change which one you use:
   ```kotlin
   // OLD:
   val repository = ProductRepository(daoProducts, daoSalesReport)

   // NEW:
   val repository = PostgreSQL_ProductRepository(daoProducts, daoSalesReport)
   ```

---

## 🔌 Step 4: Update MainActivity

Add API connection test in `MainActivity.kt`:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Cloudinary (keep your existing code)
        CloudinaryHelper.initialize(this)

        // ✅ ADD THIS: Test API connection
        lifecycleScope.launch {
            try {
                val response = BaneloApi.service.getAllProducts()

                if (response.isSuccessful && response.body()?.success == true) {
                    val count = response.body()?.data?.size ?: 0
                    Log.d("MainActivity", "✅ API Connected! Found $count products")
                    Toast.makeText(
                        this@MainActivity,
                        "Connected to database: $count products",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Log.e("MainActivity", "❌ API Error: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "❌ Connection failed: ${e.message}")
                Toast.makeText(
                    this@MainActivity,
                    "Warning: Database connection failed. Using offline mode.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        setContent {
            // ... your existing UI code
        }
    }
}
```

---

## 🛒 Step 5: Update Order Processing

In `OrderProcessScreen.kt` (or wherever you process sales):

**BEFORE (Firestore):**
```kotlin
// OLD CODE ❌
productRepository.deductProductStock(product.firebaseId, quantity)
productRepository.insertSalesReport(sale)
```

**AFTER (REST API with Ingredient Deduction):**
```kotlin
// NEW CODE ✅
val result = productRepository.processSaleWithIngredientDeduction(
    product = selectedProduct,
    quantity = quantity,
    paymentMode = selectedPaymentMode,  // "Cash", "GCash", or "Card"
    gcashReferenceId = gcashRefId  // Only if GCash
)

result.onSuccess {
    Log.d("OrderProcess", "✅ Sale processed - ingredients deducted!")

    Toast.makeText(
        context,
        "Order completed! Ingredients deducted from inventory.",
        Toast.LENGTH_SHORT
    ).show()

    // Clear cart, navigate to success screen, etc.
    navController.navigate("order_success")

}.onFailure { error ->
    Log.e("OrderProcess", "❌ Error: ${error.message}")

    Toast.makeText(
        context,
        "Error processing order: ${error.message}",
        Toast.LENGTH_LONG
    ).show()
}
```

---

## 📊 Step 6: Update Other Screens

### Inventory List Screen

Already works! Just calls:
```kotlin
viewModel.loadProducts()  // Uses repository.getAll()
```

### Add/Edit Product Screen

Already works! Calls:
```kotlin
productRepository.insert(product)  // or update(product)
```

### Sales Report Screen

Update to fetch from API:
```kotlin
// In your SalesReportViewModel:
fun loadSales() {
    viewModelScope.launch {
        val sales = productRepository.getAllSales()
        _salesState.value = sales
    }
}
```

### Inventory Transfer Screen

Already works! Calls:
```kotlin
val result = productRepository.transferInventory(product.firebaseId, quantity)
```

---

## 🧪 Step 7: Test the App

### Before Running App

1. **Start the API server:**
   ```cmd
   cd api-backend
   npm start
   ```

   You should see: "✅ Server running on: http://localhost:3000"

2. **Verify PostgreSQL is running:**
   ```cmd
   psql -U postgres -d banelo_db
   \dt
   \q
   ```

### Run the App

1. Click **Run** in Android Studio
2. Check the Logcat for:
   ```
   ✅ API Connected! Found 71 products
   ```

3. You should see a Toast message: "Connected to database: 71 products"

### Test Features

✅ **Load Products**
   - Go to inventory list
   - Should see all products from PostgreSQL

✅ **Add Product**
   - Add a new product
   - Check it appears in the list

✅ **Process Sale**
   - Select a product with a recipe (e.g., Chocolate Cake)
   - Complete the sale
   - Check Logcat for "ingredients deducted" message

✅ **Check Inventory**
   - Go to ingredient list
   - Verify ingredients were deducted
   - Example: Flour quantity should be reduced

✅ **Transfer Inventory**
   - Transfer from warehouse to display
   - Should update immediately

---

## 🔍 Troubleshooting

### "Connection refused" or "Failed to connect"

**Cause:** API server not running or wrong URL

**Fix:**
1. Make sure API is running: `npm start` in api-backend folder
2. Check URL in `BaneloApiService.kt`:
   - Emulator: `http://10.0.2.2:3000/`
   - Physical device: `http://YOUR_IP:3000/`

### "Unresolved reference: BaneloApi"

**Cause:** Didn't copy BaneloApiService.kt

**Fix:**
Copy `BaneloApiService.kt` to your app package

### Products not loading

**Cause:** Database empty or API error

**Fix:**
1. Check API in browser: `http://localhost:3000/api/products`
2. Should see JSON with products
3. If empty, reimport data (see main migration guide)

### Gradle sync failed

**Cause:** Typo in dependencies

**Fix:**
1. Make sure Retrofit dependencies are correct (see Step 1)
2. Remove old pgjdbc-ng and netty dependencies
3. Click "Sync Now"

### "NetworkOnMainThreadException"

**Cause:** Calling API from main thread

**Fix:**
Always use coroutines:
```kotlin
viewModelScope.launch {
    // API calls here
}
```

---

## 📱 For Physical Device Testing

If testing on real phone (not emulator):

### Step 1: Find Your Computer's IP

```cmd
ipconfig
```

Look for "IPv4 Address" (e.g., 192.168.1.100)

### Step 2: Update API Base URL

In `BaneloApiService.kt`:

```kotlin
object BaneloApi {
    // Change from emulator IP to your actual IP
    private const val BASE_URL = "http://192.168.1.100:3000/"  // Your IP here
    // ...
}
```

### Step 3: Ensure Same WiFi

- Computer and phone must be on same WiFi network
- Firewall must allow port 3000

### Step 4: Test Connection

On phone browser, visit:
```
http://192.168.1.100:3000/api/products
```

Should see JSON response.

---

## 🚀 Production Deployment

### Current Setup (Development)

✅ API runs on your computer
✅ Android app connects via local network
✅ Good for testing

### Production Setup (Recommended)

For a real production app:

1. **Deploy API to cloud:**
   - Heroku (free tier)
   - Railway (easy PostgreSQL)
   - DigitalOcean (VPS)
   - Render (simple deployment)

2. **Update BASE_URL:**
   ```kotlin
   private const val BASE_URL = "https://your-api.herokuapp.com/"
   ```

3. **Add security:**
   - API authentication
   - HTTPS (SSL)
   - Rate limiting

---

## ✅ Migration Checklist

- [ ] Removed pgjdbc-ng and netty dependencies
- [ ] Added Retrofit dependencies
- [ ] Removed packaging block
- [ ] Gradle sync successful
- [ ] Copied BaneloApiService.kt to project
- [ ] Copied PostgreSQL_ProductRepository.kt to project
- [ ] Renamed/replaced ProductRepository.kt
- [ ] Added API test in MainActivity
- [ ] Updated order processing with ingredient deduction
- [ ] Started API server (`npm start`)
- [ ] App runs without errors
- [ ] Products load from API
- [ ] Sales processing works
- [ ] Ingredients deducted correctly
- [ ] Inventory transfers work

---

## 📊 What You Get

### ✅ All Features Working:
- **Users/Login** - Via REST API
- **Products CRUD** - Via REST API
- **Inventory Management** - Dual inventory (A/B)
- **Sales with Ingredient Deduction** - Proper recipe-based system
- **Recipes** - Products with ingredient lists
- **Waste Logging** - Deducts from inventory
- **Reports** - Sales summary, top products, low stock
- **Audit Trail** - All actions logged
- **Offline Mode** - Room database fallback

### ❌ No More:
- JDBC driver errors
- Netty conflicts
- META-INF duplicate files
- Build failures
- Android compatibility issues

---

**You're done!** Your app now uses PostgreSQL via REST API with proper ingredient-based inventory management. 🎉

Need help? Check the troubleshooting section or the main migration guide.
