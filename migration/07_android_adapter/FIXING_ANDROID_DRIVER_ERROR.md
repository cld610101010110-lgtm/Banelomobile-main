# Fix for PostgreSQL JDBC Driver Error on Android

## Problem
The standard PostgreSQL JDBC driver uses `MethodHandle.invoke` which is not available on Android, causing build errors.

## Solution: Use Android-Compatible Driver

### Option A: Use REST API (Recommended for Production)

Instead of direct database connection, create a simple backend API. But for testing, we'll use Option B.

### Option B: Use pgjdbc-ng (Android-Compatible Driver)

Update your `app/build.gradle`:

## Step 1: Remove the Standard Driver

**REMOVE this line:**
```gradle
implementation("org.postgresql:postgresql:42.7.1")  // ❌ Doesn't work on Android
```

## Step 2: Add Android-Compatible Alternatives

**Option 2A: Use Ktor Client + REST API (Best Practice)**

This is the recommended approach. Your Android app calls a simple API, which then talks to PostgreSQL.

Add to `app/build.gradle`:
```gradle
dependencies {
    // Ktor for HTTP client
    implementation("io.ktor:ktor-client-android:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
}
```

**Option 2B: Direct Connection with PgJDBC-NG**

If you must connect directly (for local testing):

```gradle
dependencies {
    // PgJDBC-NG - Android compatible PostgreSQL driver
    implementation("com.impossibl.pgjdbc-ng:pgjdbc-ng:0.8.9")

    // Required dependencies
    implementation("io.netty:netty-all:4.1.100.Final")
}
```

Then update your `PostgreSQLAdapter.kt`:

```kotlin
// Change the driver class name
Class.forName("com.impossibl.postgres.jdbc.PGDriver")  // Instead of org.postgresql.Driver
```

And update the JDBC URL format:
```kotlin
private val JDBC_URL = "jdbc:pgsql://$DB_HOST:$DB_PORT/$DB_NAME"  // Note: pgsql not postgresql
```

## Step 3: Quick Fix for Testing (Simplest)

For immediate testing, the easiest solution is to create a simple Node.js REST API:

### Create Simple Backend (5 minutes):

1. Install Node.js if not installed
2. Create a folder: `banelo-api`
3. Create `server.js`:

```javascript
const express = require('express');
const { Pool } = require('pg');

const app = express();
app.use(express.json());

const pool = new Pool({
    host: 'localhost',
    port: 5432,
    database: 'banelo_db',
    user: 'postgres',
    password: 'admin123'
});

// Get all products
app.get('/api/products', async (req, res) => {
    const result = await pool.query('SELECT * FROM products WHERE is_active = true');
    res.json(result.rows);
});

// Process sale with ingredient deduction
app.post('/api/sales/process', async (req, res) => {
    const { productFirebaseId, quantity, sale } = req.body;

    const client = await pool.connect();
    try {
        await client.query('BEGIN');

        // Insert sale
        await client.query(
            'INSERT INTO sales (id, product_name, quantity, price, order_date, product_firebase_id, payment_mode) VALUES (gen_random_uuid(), $1, $2, $3, $4, (SELECT id FROM products WHERE firebase_id = $5), $6)',
            [sale.productName, sale.quantity, sale.price, sale.orderDate, productFirebaseId, sale.paymentMode]
        );

        // Get recipe ingredients
        const ingredients = await client.query(`
            SELECT ri.ingredient_firebase_id, ri.quantity_needed, p.firebase_id
            FROM recipes r
            JOIN recipe_ingredients ri ON r.id = ri.recipe_firebase_id
            JOIN products p ON ri.ingredient_firebase_id = p.id
            WHERE r.product_firebase_id = (SELECT id FROM products WHERE firebase_id = $1)
        `, [productFirebaseId]);

        // Deduct each ingredient
        for (const ing of ingredients.rows) {
            const totalNeeded = ing.quantity_needed * quantity;
            await client.query(`
                UPDATE products
                SET quantity = quantity - $1,
                    inventory_b = GREATEST(0, inventory_b - $1),
                    inventory_a = GREATEST(0, inventory_a - GREATEST(0, $1 - inventory_b))
                WHERE firebase_id = $2
            `, [totalNeeded, ing.firebase_id]);
        }

        await client.query('COMMIT');
        res.json({ success: true, message: 'Sale processed' });
    } catch (err) {
        await client.query('ROLLBACK');
        res.status(500).json({ error: err.message });
    } finally {
        client.release();
    }
});

app.listen(3000, () => console.log('API running on http://localhost:3000'));
```

4. Install dependencies:
```bash
npm init -y
npm install express pg
```

5. Run:
```bash
node server.js
```

Then in your Android app, use Retrofit or Ktor to call `http://10.0.2.2:3000/api/products`

## Recommended Approach

For a production app, **use the REST API approach**. Direct database connections from mobile apps are:
- ❌ Security risk (database credentials in app)
- ❌ No connection pooling
- ❌ Network issues on mobile
- ❌ Hard to update logic without app update

With REST API:
- ✅ Secure (no database credentials in app)
- ✅ Better performance
- ✅ Easier to maintain
- ✅ Industry standard

## Which Option Should You Choose?

**For Learning/Testing:** Use Option 2B (pgjdbc-ng) if you want to keep direct connection

**For Production:** Use the REST API approach (Option 2A or the Node.js example)

Let me know which approach you prefer and I'll help you implement it!
