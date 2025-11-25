# 🔧 STEP-BY-STEP FIX GUIDE

## Your Business Logic (Now Correctly Implemented)

### ✅ Ingredients (Flour, Butter, Sugar, etc.)
- **Have real physical stock**
- Stock goes to **Inventory A** when added
- **Transfer** from A → B before selling
- Orders deduct from **B first**, then A
- Always managed in **products table**

### ✅ Products (Beverages, Pastries)
- **ALWAYS have 0 stock** (enforced by API)
- Available quantity = **calculated from ingredient recipes**
- Calculation uses total ingredient quantity (A + B)
- **Cannot have stock** - it's recipe-based

### ✅ When Ordering
- Only **ingredients** are deducted
- **Products are NOT deducted** (they stay at 0)
- **Sales table** records which product was sold
- **Audit trail** logs the transaction

---

## 🚨 Current Issues You're Facing

1. **PostgreSQL not running** → Can't open pgAdmin
2. **Ingredients have 0 stock** in PostgreSQL
3. **Sales fail** with "Insufficient ingredient stock"

---

## 📝 Step-by-Step Fix

### **STEP 1: Start PostgreSQL** ⭐ DO THIS FIRST

**Option A: Using Windows Services**
```
1. Press Windows + R
2. Type: services.msc
3. Find "postgresql-x64-18"
4. Right-click → Start
```

**Option B: Command Prompt (Administrator)**
```cmd
net start postgresql-x64-18
```

**Verify:** Try opening pgAdmin 4 - it should now open successfully.

---

### **STEP 2: Check Database Status**

Run the diagnostic tool:
```bash
cd api-backend
node check_and_fix_db.js
```

This will show you:
- ✅ Which ingredients have stock
- ❌ Which ingredients have 0 stock
- ⚠️  Which products incorrectly have stock (should be 0)
- 📋 Recipe status

---

### **STEP 3: Fix Product Stock (Set Beverages/Pastries to 0)**

**Option A: Using pgAdmin**
1. Open pgAdmin 4
2. Connect to `banelo_db`
3. Tools → Query Tool
4. Run this SQL:
```sql
UPDATE products
SET quantity = 0, inventory_a = 0, inventory_b = 0
WHERE category IN ('Beverages', 'Pastries');
```

**Option B: Using the SQL file**
1. Open pgAdmin 4
2. Tools → Query Tool
3. File → Open → Select `api-backend/fix_product_stock.sql`
4. Click Execute (▶️)

---

### **STEP 4: Add Stock to Ingredients**

You MUST add stock through the **mobile app** so it syncs to PostgreSQL.

**For EACH ingredient (Flour, Butter, Sugar, Eggs, etc.):**

1. Open mobile app
2. Go to **Inventory/Products**
3. Find the ingredient (e.g., "Flour")
4. Click **Edit**
5. Set **Inventory A** (warehouse stock)
   - Example: Flour → 5000g
6. Leave **Inventory B** at 0 (for now)
7. Click **Save**

**Repeat for all ingredients:**
- Flour: 5000g
- Butter: 2000g
- Sugar: 3000g
- Eggs: 100 pcs
- Milk: 3000ml
- etc.

---

### **STEP 5: Transfer Stock to Inventory B**

Before you can sell, you need stock in **Inventory B**.

1. In mobile app, go to **Inventory Transfer** screen
2. Select ingredient (e.g., "Flour")
3. Enter amount to transfer (e.g., 1000)
4. This moves from **A → B**
5. Repeat for each ingredient

**Why?** Sales deduct from Inventory B first (display stock), then A (warehouse).

---

### **STEP 6: Verify Everything is Working**

Run the diagnostic again:
```bash
node check_and_fix_db.js
```

You should see:
```
✅ Flour: Qty:5000 | A:4000 | B:1000
✅ Butter: Qty:2000 | A:1500 | B:500
✅ Almond Croissant (Pastries): 0 stock (correct)
✅ Coffee (Beverages): 0 stock (correct)
```

---

### **STEP 7: Start the API Server**

```bash
cd api-backend
node server.js
```

You should see:
```
✅ Connected to PostgreSQL database: banelo_db
✅ Server running on: http://localhost:3000
```

---

### **STEP 8: Test an Order**

1. Open mobile app
2. Go to **Order Process**
3. Add a product (e.g., "Almond Croissant")
4. Complete the order

**Expected Results:**
- ✅ Sale succeeds
- ✅ Only ingredients are deducted
- ✅ Products (Beverages/Pastries) remain at 0 stock
- ✅ Sale appears in audit trail
- ✅ Sales table shows the product sold

**If it fails:**
- Check API logs for the error
- Verify ingredients have stock in Inventory B
- Run `node check_and_fix_db.js` again

---

## 🔍 Troubleshooting

### "Insufficient stock for [ingredient]"
- **Cause:** Ingredient has 0 stock or insufficient stock in PostgreSQL
- **Fix:** Add stock through mobile app (Step 4)

### "Sale failed - Check ingredient stock!"
- **Cause:** API validation blocked the sale (not enough ingredients)
- **Fix:** Add more stock to ingredients, transfer to Inventory B

### pgAdmin won't open
- **Cause:** PostgreSQL service not running
- **Fix:** Start PostgreSQL service (Step 1)

### Ingredients still show 0 stock after adding
- **Cause:** Stock update didn't sync to PostgreSQL
- **Fix:**
  1. Check API is running
  2. Check mobile app can connect to API
  3. Verify `BaneloApiService.kt` BASE_URL is correct
  4. Add stock again through mobile app

### Products (Beverages/Pastries) show stock
- **Cause:** Old data before the fix
- **Fix:** Run the SQL script (Step 3)

---

## 📊 All Fixes Applied (3 Commits)

### Commit 1: Initial bug fixes
- Fixed `Math.floor()` → `Math.ceil()` for fractional quantities
- Added `AuditHelper.logSale()` to log sales in audit trail

### Commit 2: Stock validation
- Added validation to prevent sales with insufficient stock
- Transaction now rolls back if ingredients are insufficient
- Better error messages on mobile app

### Commit 3: Recipe-based product logic
- **Beverages and Pastries ALWAYS forced to 0 stock**
- API enforces this on create AND update
- Created diagnostic tools (`check_and_fix_db.js`)
- Created SQL fix script (`fix_product_stock.sql`)

---

## ✅ Summary

**Your System Now Works Like This:**

1. **Add Ingredients** → Stock goes to Inventory A
2. **Transfer** → Move from A to B (display stock)
3. **Create Recipes** → Link products to ingredients
4. **Order Products** → System calculates available quantity from ingredients
5. **Complete Sale** → Only ingredients deducted, product recorded in sales
6. **Audit Trail** → Everything logged

**Products (Beverages/Pastries) = 0 stock ALWAYS** ✅
**Ingredients = Real stock (A + B)** ✅
**Sales deduct only ingredients** ✅
**Audit trail logs all transactions** ✅

---

Need help? The API now shows detailed logs for every sale processed!
