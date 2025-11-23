# 📱 Android App Migration Checklist

## ⚠️ Important Note

**Direct PostgreSQL connections from Android are NOT recommended for production.**

### Why?
- Security: Database credentials exposed in APK
- Performance: No connection pooling
- Network: Not optimized for mobile networks
- Battery: Persistent connections drain battery

### Recommended Architecture

```
Android App (Kotlin)
        ↓
   REST API (Node.js/Spring Boot/FastAPI)
        ↓
   PostgreSQL Database
```

**For production, create a backend API server.** This migration guide provides direct PostgreSQL access for **testing and development only**.

---

## 🔧 Migration Steps

### Step 1: Add PostgreSQL Dependency

Edit `app/build.gradle.kts`:

```kotlin
dependencies {
    // Existing dependencies...

    // PostgreSQL JDBC driver
    implementation("org.postgresql:postgresql:42.6.0")

    // Optional: For better coroutine support
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
```

### Step 2: Update Android Manifest

Add internet permission in `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

If using cleartext HTTP (development only):

```xml
<application
    android:usesCleartextTraffic="true"
    ...>
```

### Step 3: Replace Firebase with PostgreSQL

#### Option A: Replace Firestore Completely

1. Copy `PostgresConnection.kt` and `DatabaseAdapter.kt` to your project:
   ```
   app/src/main/java/com/example/banelo/database/
   ```

2. Update connection details in `PostgresConnection.kt`:
   ```kotlin
   private const val DB_HOST = "YOUR_SERVER_IP"  // Your PostgreSQL server
   private const val DB_PORT = "5432"
   private const val DB_NAME = "banelo_db"
   private const val DB_USER = "banelo_user"
   private const val DB_PASSWORD = "banelo_password_2024"
   ```

3. Replace Firestore calls in repositories:

   **BEFORE:**
   ```kotlin
   firestore.collection("products")
       .get()
       .addOnSuccessListener { documents ->
           val products = documents.map { it.toObject<Entity_Products>() }
           _products.value = products
       }
   ```

   **AFTER:**
   ```kotlin
   viewModelScope.launch {
       try {
           val products = DatabaseAdapter.getProducts()
           _products.value = products
       } catch (e: Exception) {
           Log.e("ProductRepo", "Error fetching products", e)
       }
   }
   ```

#### Option B: Hybrid Approach (Gradual Migration)

Keep both Firebase and PostgreSQL during transition:

```kotlin
class ProductRepository {
    suspend fun getProducts(): List<Entity_Products> {
        return try {
            // Try PostgreSQL first
            DatabaseAdapter.getProducts()
        } catch (e: Exception) {
            Log.w("ProductRepo", "PostgreSQL failed, falling back to Firestore", e)
            // Fallback to Firestore
            getProductsFromFirestore()
        }
    }

    private suspend fun getProductsFromFirestore(): List<Entity_Products> {
        return suspendCoroutine { continuation ->
            firestore.collection("products").get()
                .addOnSuccessListener { docs ->
                    continuation.resume(docs.map { it.toObject<Entity_Products>() })
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }
    }
}
```

### Step 4: Update ViewModels

Change from callback-based to coroutine-based:

**BEFORE (Firestore):**
```kotlin
class ProductViewModel : ViewModel() {
    private val _products = MutableStateFlow<List<Entity_Products>>(emptyList())
    val products: StateFlow<List<Entity_Products>> = _products

    fun loadProducts() {
        firestore.collection("products")
            .get()
            .addOnSuccessListener { documents ->
                _products.value = documents.map { it.toObject<Entity_Products>() }
            }
    }
}
```

**AFTER (PostgreSQL):**
```kotlin
class ProductViewModel : ViewModel() {
    private val _products = MutableStateFlow<List<Entity_Products>>(emptyList())
    val products: StateFlow<List<Entity_Products>> = _products

    fun loadProducts() {
        viewModelScope.launch {
            try {
                _products.value = DatabaseAdapter.getProducts()
            } catch (e: Exception) {
                Log.e("ProductVM", "Error loading products", e)
            }
        }
    }
}
```

### Step 5: Handle Authentication

PostgreSQL doesn't have built-in authentication like Firebase Auth.

**Options:**

1. **Keep Firebase Auth** (recommended for now)
   - Continue using Firebase Auth for user authentication
   - Store user data in PostgreSQL `users` table
   - Link Firebase UID to PostgreSQL user records

2. **Implement Custom Auth**
   - Create backend API with JWT authentication
   - Store hashed passwords in PostgreSQL
   - Use libraries like bcrypt for password hashing

### Step 6: Update Data Models

No changes needed! Your existing entities (`Entity_Products`, etc.) work as-is.

### Step 7: Test Thoroughly

Create a test class:

```kotlin
class DatabaseMigrationTest {
    @Test
    fun testPostgreSQLConnection() = runBlocking {
        val connection = PostgresConnection.getConnection()
        assertNotNull(connection)
        PostgresConnection.closeConnection(connection)
    }

    @Test
    fun testGetProducts() = runBlocking {
        val products = DatabaseAdapter.getProducts()
        assertTrue(products.isNotEmpty())
    }
}
```

---

## 🚀 Production Recommendation: Build REST API

### Recommended Stack

**Option 1: Node.js + Express**
```javascript
// server.js
const express = require('express');
const { Pool } = require('pg');

const app = express();
const pool = new Pool({
    host: 'localhost',
    database: 'banelo_db',
    user: 'banelo_user',
    password: 'banelo_password_2024'
});

app.get('/api/products', async (req, res) => {
    const result = await pool.query('SELECT * FROM products WHERE is_active = true');
    res.json(result.rows);
});

app.listen(3000);
```

**Option 2: Spring Boot (Kotlin)**
```kotlin
@RestController
@RequestMapping("/api/products")
class ProductController(private val productRepository: ProductRepository) {

    @GetMapping
    fun getAllProducts(): List<Product> {
        return productRepository.findAll()
    }
}
```

### Android Retrofit Client

```kotlin
// API Interface
interface BaneloApi {
    @GET("api/products")
    suspend fun getProducts(): List<Entity_Products>

    @POST("api/sales")
    suspend fun addSale(@Body sale: Entity_SalesReport): Response<String>
}

// Repository
class ProductRepository(private val api: BaneloApi) {
    suspend fun getProducts(): List<Entity_Products> {
        return api.getProducts()
    }
}
```

---

## 📋 Migration Priority Order

1. **Week 1: Setup & Testing**
   - [ ] Setup PostgreSQL database
   - [ ] Import data using CSV scripts
   - [ ] Test database connectivity
   - [ ] Create test endpoints

2. **Week 2: Read Operations**
   - [ ] Migrate product listing
   - [ ] Migrate sales reports
   - [ ] Migrate user management
   - [ ] Test read performance

3. **Week 3: Write Operations**
   - [ ] Migrate sales transactions
   - [ ] Migrate inventory updates
   - [ ] Migrate waste logging
   - [ ] Test data integrity

4. **Week 4: Testing & Deployment**
   - [ ] End-to-end testing
   - [ ] Performance testing
   - [ ] User acceptance testing
   - [ ] Production deployment

---

## 🔍 Common Issues & Solutions

### Issue: "Connection refused"
**Solution:** Check PostgreSQL is accessible:
```bash
# On server
sudo ufw allow 5432
sudo systemctl status postgresql

# Edit postgresql.conf
listen_addresses = '*'

# Edit pg_hba.conf
host all all 0.0.0.0/0 md5
```

### Issue: "Class not found: org.postgresql.Driver"
**Solution:** Ensure PostgreSQL dependency is in build.gradle:
```kotlin
implementation("org.postgresql:postgresql:42.6.0")
```

### Issue: "Network on main thread"
**Solution:** Always use coroutines for database calls:
```kotlin
viewModelScope.launch {
    // Database calls here
}
```

---

## ✅ Testing Checklist

- [ ] Can connect to PostgreSQL from Android
- [ ] Can fetch products successfully
- [ ] Can add new sales transactions
- [ ] Can update inventory
- [ ] Can record waste logs
- [ ] Room database still works (offline)
- [ ] App works without internet (offline-first)
- [ ] No crashes on network errors
- [ ] Performance is acceptable (< 2s response time)
- [ ] Data is consistent between clients

---

## 🎯 Next Steps After Migration

1. **Optimize queries** with proper indexes
2. **Implement caching** to reduce database calls
3. **Add offline support** using Room as cache layer
4. **Monitor performance** with analytics
5. **Plan backend API** for production

---

**Questions? Issues?**
Check the main README or create an issue in the migration repository.
