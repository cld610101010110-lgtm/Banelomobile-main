# PostgreSQL Migration Guide for Banelo POS

Complete guide to migrate your Banelo Android app from Firebase Firestore to PostgreSQL with ingredient-based inventory management.

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [What Changed](#what-changed)
3. [Setup Instructions](#setup-instructions)
4. [Code Migration](#code-migration)
5. [Testing](#testing)
6. [Troubleshooting](#troubleshooting)

---

## Overview

### Migration Summary

**FROM:** Firebase Firestore (Cloud NoSQL)
**TO:** PostgreSQL 18.1 (Local/Self-hosted SQL)

### Key Benefits

✅ **Full data ownership** - Your data stays on your server
✅ **No monthly costs** - No Firebase pricing limits
✅ **Ingredient-based inventory** - Proper recipe management
✅ **Better data relationships** - SQL foreign keys and constraints
✅ **Offline-first ready** - Can work without internet

### Ingredient-Based Inventory System

**OLD Behavior (Firestore):**
- When "Chocolate Cake" is sold → Deduct 1 from "Chocolate Cake" inventory

**NEW Behavior (PostgreSQL):**
- When "Chocolate Cake" is sold → Deduct ingredients:
  - 500g Flour
  - 300g Sugar
  - 200g Butter
  - 4 Eggs
  - 100g Cocoa Powder

This matches your real POS process!

---

## What Changed

### Database Location

| Aspect | Firebase | PostgreSQL |
|--------|----------|-----------|
| Location | Google Cloud | Your Computer/Server |
| Access | Internet Required | Local Network |
| Cost | Pay per read/write | Free (your hardware) |

### Data Structure

**Same table names and fields!** We kept your existing structure:
- `products` (Pastries, Beverages, Ingredients)
- `users` (Staff, Managers)
- `sales` (Transaction records)
- `recipes` (Product formulas)
- `recipe_ingredients` (Ingredient lists)
- `waste_logs` (Waste tracking)

### Code Changes Required

1. Add PostgreSQL JDBC dependency to `build.gradle`
2. Copy adapter files to your project
3. Update Repository classes to use `PostgreSQLAdapter`
4. Update ViewModel/UI to handle coroutines (already using them!)

---

## Setup Instructions

### Step 1: Verify PostgreSQL is Running

Open Command Prompt:

```cmd
psql -U postgres
```

Enter password: `admin123`

You should see:
```
postgres=#
```

Type `\l` to list databases - you should see `banelo_db`.

Type `\q` to exit.

### Step 2: Verify Data Imported

```cmd
psql -U postgres -d banelo_db
```

```sql
SELECT COUNT(*) FROM products;  -- Should show 71
SELECT COUNT(*) FROM recipes;   -- Should show 67
SELECT COUNT(*) FROM sales;     -- Should show 10000
\q
```

### Step 3: Add PostgreSQL JDBC to build.gradle

Open `app/build.gradle` and add in the `dependencies` block:

```gradle
dependencies {
    // ... existing dependencies ...

    // PostgreSQL JDBC Driver
    implementation 'org.postgresql:postgresql:42.7.1'
}
```

Click **"Sync Now"** in Android Studio.

### Step 4: Copy Adapter Files

Copy these files from `migration/07_android_adapter/` to your main app:

```
migration/07_android_adapter/PostgreSQLAdapter.kt
  → app/src/main/java/com/project/dba_delatorre_dometita_ramirez_tan/
```

### Step 5: Update AndroidManifest.xml

Add internet permission (if not already present):

```xml
<manifest ...>
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### Step 6: Configure Network Security (Android 9+)

Create `app/src/main/res/xml/network_security_config.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">10.0.2.2</domain>
        <domain includeSubdomains="true">localhost</domain>
    </domain-config>
</network-security-config>
```

Reference it in `AndroidManifest.xml`:

```xml
<application
    ...
    android:networkSecurityConfig="@xml/network_security_config">
```

---

## Code Migration

### Option A: Full Migration (Recommended)

Replace ProductRepository's Firestore calls with PostgreSQL.

#### BEFORE (Firestore):

```kotlin
class ProductRepository(
    private val daoProducts: Dao_Products
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val productsCollection = firestore.collection("products")

    suspend fun getAll(): List<Entity_Products> {
        return withContext(Dispatchers.IO) {
            val snapshot = productsCollection.get().await()
            snapshot.documents.mapNotNull { doc ->
                Entity_Products(
                    firebaseId = doc.id,
                    name = doc.getString("name") ?: "",
                    // ... other fields
                )
            }
        }
    }
}
```

#### AFTER (PostgreSQL):

```kotlin
class ProductRepository(
    private val daoProducts: Dao_Products
) {
    // Remove Firestore
    // private val firestore = FirebaseFirestore.getInstance() ❌

    suspend fun getAll(): List<Entity_Products> {
        return try {
            // Get from PostgreSQL
            val products = PostgreSQLAdapter.getAllProducts()

            // Sync to Room for offline access
            if (products.isNotEmpty()) {
                daoProducts.insertProducts(products)
            }

            products
        } catch (e: Exception) {
            android.util.Log.e("ProductRepo", "PostgreSQL failed, using Room: ${e.message}")
            // Fallback to Room
            daoProducts.getAllProducts()
        }
    }

    suspend fun insert(product: Entity_Products) {
        // Insert to PostgreSQL
        val firebaseId = PostgreSQLAdapter.insertProduct(product)

        // Sync to Room
        daoProducts.insertProduct(product.copy(firebaseId = firebaseId))
    }

    suspend fun update(product: Entity_Products) {
        PostgreSQLAdapter.updateProduct(product)
        daoProducts.updateProduct(product)
    }
}
```

### Option B: Hybrid Mode (Safe Testing)

Keep Firestore but add PostgreSQL alongside.

```kotlin
class ProductRepository(
    private val daoProducts: Dao_Products
) {
    private val firestore = FirebaseFirestore.getInstance()  // Keep for now
    private var usePostgres = true  // Toggle here

    suspend fun getAll(): List<Entity_Products> {
        return if (usePostgres) {
            try {
                PostgreSQLAdapter.getAllProducts()
            } catch (e: Exception) {
                Log.w("ProductRepo", "PostgreSQL failed, falling back to Firestore")
                // Fallback to Firestore
                getFromFirestore()
            }
        } else {
            getFromFirestore()
        }
    }

    private suspend fun getFromFirestore(): List<Entity_Products> {
        // ... existing Firestore code
    }
}
```

### Ingredient-Based Sale Processing

The most important change: **Replace `deductProductStock()` with `processSaleWithIngredientDeduction()`**

#### BEFORE (in OrderProcessScreen.kt or similar):

```kotlin
// Old way: Deduct the product itself
productRepository.deductProductStock(product.firebaseId, quantity)
productRepository.insertSalesReport(sale)
```

#### AFTER:

```kotlin
// New way: Deduct ingredients, not the product
val result = PostgreSQLAdapter.processSaleWithIngredientDeduction(
    productFirebaseId = product.firebaseId,
    quantitySold = quantity,
    sale = Entity_SalesReport(
        productName = product.name,
        category = product.category,
        quantity = quantity,
        price = product.price,
        orderDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
        productFirebaseId = product.firebaseId,
        paymentMode = paymentMode,  // "Cash", "GCash", "Card"
        gcashReferenceId = gcashRef
    )
)

result.onSuccess {
    Toast.makeText(context, "Sale processed! Ingredients deducted.", Toast.LENGTH_SHORT).show()
}.onFailure { error ->
    Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_LONG).show()
}
```

---

## Testing

### Step 1: Test Connection

Add to MainActivity.onCreate():

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Test PostgreSQL connection
        lifecycleScope.launch {
            val result = PostgreSQLAdapter.testConnection()
            result.onSuccess { message ->
                Log.d("MainActivity", message)
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
            }.onFailure { error ->
                Log.e("MainActivity", "PostgreSQL connection failed", error)
                Toast.makeText(
                    this@MainActivity,
                    "PostgreSQL Error: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
```

### Step 2: Test Product Fetching

In your ProductViewModel or Screen:

```kotlin
viewModelScope.launch {
    try {
        val products = PostgreSQLAdapter.getAllProducts()
        Log.d("Products", "Loaded ${products.size} products from PostgreSQL")
        _productsState.value = products
    } catch (e: Exception) {
        Log.e("Products", "Error: ${e.message}", e)
    }
}
```

### Step 3: Test Sale with Ingredient Deduction

Before testing sales, check that the product has a recipe:

```sql
-- In psql
SELECT r.product_name, COUNT(ri.id) as ingredient_count
FROM recipes r
LEFT JOIN recipe_ingredients ri ON r.id = ri.recipe_firebase_id
WHERE r.product_name = 'Chocolate Cake'
GROUP BY r.product_name;
```

Should show: `Chocolate Cake | 5`

Then make a test sale in your app and check ingredients were deducted:

```sql
-- Check ingredient inventory BEFORE sale
SELECT name, inventory_a, inventory_b, quantity
FROM products
WHERE category = 'Ingredients';

-- Make a sale in the app (e.g., 1 Chocolate Cake)

-- Check inventory AFTER sale - should be reduced!
SELECT name, inventory_a, inventory_b, quantity
FROM products
WHERE category = 'Ingredients';
```

---

## Troubleshooting

### "No suitable driver found for jdbc:postgresql"

**Solution:**
1. Make sure `implementation 'org.postgresql:postgresql:42.7.1'` is in build.gradle
2. Sync Gradle
3. Build → Clean Project
4. Build → Rebuild Project

### "Connection refused" or "Cannot connect"

**For Android Emulator:**
- Use `DB_HOST = "10.0.2.2"` (already set in PostgreSQLAdapter.kt)

**For Physical Device:**
1. Find your computer's IP:
   ```cmd
   ipconfig
   ```
   Look for "IPv4 Address" (e.g., 192.168.1.100)

2. Update `PostgreSQLAdapter.kt`:
   ```kotlin
   private const val DB_HOST = "192.168.1.100"  // Your actual IP
   ```

3. Configure PostgreSQL to accept remote connections:
   - Edit `C:\Program Files\PostgreSQL\18\data\postgresql.conf`:
     ```
     listen_addresses = '*'
     ```
   - Edit `C:\Program Files\PostgreSQL\18\data\pg_hba.conf`, add:
     ```
     host    all             all             0.0.0.0/0               md5
     ```
   - Restart PostgreSQL

4. Check Windows Firewall allows port 5432

### "Password authentication failed"

**Solution:**
- Verify password in `PostgreSQLAdapter.kt` matches your PostgreSQL password
- Default in our setup: `admin123`

### No ingredients deducted when selling product

**Check if recipe exists:**
```sql
SELECT * FROM recipes WHERE product_name = 'Your Product Name';
```

If empty, the product has no recipe. Add one or regenerate data:
```cmd
cd C:\Users\rommel\Downloads\Banelomobile-maintest\Banelomobile-main
python FIXED_generate_recipes.py
```

### App crashes with "NetworkOnMainThreadException"

**Solution:**
- Always call PostgreSQL operations inside coroutines:
  ```kotlin
  viewModelScope.launch {
      PostgreSQLAdapter.getAllProducts()
  }
  ```
- Never call from main thread directly

---

## Next Steps

1. ✅ **Test connection** - Verify app can reach PostgreSQL
2. ✅ **Test product loading** - See products from database
3. ✅ **Test ingredient deduction** - Make a sale, check ingredients reduced
4. ✅ **Migrate all screens** - Update inventory, sales report, etc.
5. ✅ **Remove Firestore** - Clean up old Firebase code
6. ✅ **Deploy to production** - Set up PostgreSQL on server

---

## Production Deployment Notes

### Current Setup (Development)
- PostgreSQL on your development computer
- Android app connects directly to database
- OK for testing

### Production Setup (Recommended)
- **Add REST API backend** (Node.js/Express, Spring Boot, etc.)
- Android app → REST API → PostgreSQL
- More secure (no database credentials in app)
- Better for multiple devices

### Sample REST API Structure
```
POST /api/sales/process-with-ingredients
{
  "productFirebaseId": "abc123",
  "quantity": 2,
  "paymentMode": "Cash"
}
```

This can be your next phase after successful migration!

---

## Support

If you encounter issues:
1. Check PostgreSQL is running: `psql -U postgres`
2. Check logs in Android Studio Logcat
3. Test connection with `PostgreSQLAdapter.testConnection()`
4. Verify data in database: `psql -U postgres -d banelo_db`

---

**Migration completed!** 🎉

Your Banelo POS now uses PostgreSQL with proper ingredient-based inventory management.
