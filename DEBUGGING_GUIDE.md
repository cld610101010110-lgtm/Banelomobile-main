# 🔍 DEBUGGING GUIDE: Sales Failing & Audit Trail Issues

## Your Reported Issues

1. ✅ Recipe and ingredients working properly
2. ❌ Sales fail with "Check ingredient stock" message
3. ❌ No sales appearing in audit trail
4. ❌ Overview not fetching data
5. ❌ Inconsistent availability when switching screens

---

## 🚀 Step-by-Step Debugging

### **STEP 1: Start PostgreSQL** ⭐ CRITICAL
```
Windows + R → services.msc → postgresql-x64-18 → Start
```

### **STEP 2: Start API Server**
```bash
cd api-backend
node server.js
```

**You should see:**
```
✅ Connected to PostgreSQL database: banelo_db
✅ Server running on: http://localhost:3000
```

**Keep this terminal open** - you'll see logs here!

---

### **STEP 3: Check Current Database Status**

Open pgAdmin 4 → banelo_db → Query Tool

Run this query (or use the SQL file):
```sql
-- Check recent sales
SELECT product_name, quantity, price, order_date, cashier_username
FROM sales
WHERE order_date > NOW() - INTERVAL '24 hours'
ORDER BY order_date DESC
LIMIT 10;

-- Check audit trail
SELECT action, description, date_time, status
FROM audit_logs
WHERE action = 'SALE_TRANSACTION'
ORDER BY date_time DESC
LIMIT 10;

-- Check ingredient stock
SELECT name, quantity, inventory_a, inventory_b
FROM products
WHERE category = 'Ingredients'
ORDER BY name;
```

**Or use the prepared file:**
```
File → Open → api-backend/check_sales_and_audit.sql
Click Execute (▶️)
```

---

### **STEP 4: Process a Test Sale**

1. **Open mobile app**
2. **Go to Order Process**
3. **Add a product to cart** (e.g., "Almond Croissant")
4. **Complete the order**

---

### **STEP 5: Check the API Logs**

**Look at the API server terminal. You should see:**

#### **If Sale Succeeds:** ✅
```
💰 Processing sale with ingredient deduction
Product: Almond Croissant, Quantity: 1
✅ Sale recorded
📋 Found 5 ingredients in recipe
✅ All ingredients have sufficient stock
📦 Deducting: Flour - 200 g
   Before - A: 5000, B: 1000
   Deducted 200 from Inventory B
   After - A: 5000, B: 800, Total: 5800
... (more ingredients)
✅ Transaction committed successfully
```

#### **If Sale Fails:** ❌
```
💰 Processing sale with ingredient deduction
Product: Almond Croissant, Quantity: 1
✅ Sale recorded
📋 Found 5 ingredients in recipe
📦 Deducting: Flour - 400 g
   Before - A: 0, B: 0
❌ Error: Insufficient stock for Flour. Need: 400, Available: 0
```

---

### **STEP 6: Check Mobile App Logs**

Open Android Studio → Logcat → Filter by "ProductRepository"

**You should see:**
```
ProductRepository: ━━━━━━━━━━━━━━━━━━━━━━━━
ProductRepository: 💰 Processing sale...
ProductRepository: Product: Almond Croissant (Pastries)
ProductRepository: Quantity: 1
ProductRepository: Payment: Cash
ProductRepository: ✅ Sale processed successfully!  // OR ❌ Sale failed: [error message]
ProductRepository: ━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## 🐛 Common Issues & Fixes

### **Issue 1: "Insufficient stock for [ingredient]"**

**Cause:** Ingredient has insufficient stock in **Inventory B**

**Check:**
```sql
SELECT name, inventory_a, inventory_b
FROM products
WHERE category = 'Ingredients';
```

**Fix:**
1. Go to **Inventory Transfer** screen
2. Transfer stock from A → B
3. Try sale again

**Why?** Sales deduct from **Inventory B first**, then A. If B is empty, it needs stock in A to cover the amount.

---

### **Issue 2: Sales Failing But Ingredients Have Stock**

**Possible Causes:**
1. **Stock is in Inventory A, but B is 0**
   - Fix: Transfer A → B

2. **Recipe needs more than available**
   - Example: Recipe needs 500g, but you only have 300g total
   - Check recipe: `SELECT * FROM recipe_ingredients WHERE ingredient_name = 'Flour';`

3. **Multiple simultaneous orders depleted stock**
   - Refresh data and check actual stock

---

### **Issue 3: No Sales in Audit Trail**

**Cause:** Audit log only created if sale **succeeds**

**Code logic:**
```kotlin
if (success) {
    AuditHelper.logSale(...)  // Only logs on success
}
```

**Fix:** First fix why sales are failing (see Issues 1-2), then sales will log.

**Verify audit is working:**
```sql
SELECT * FROM audit_logs
WHERE action != 'SALE_TRANSACTION'
ORDER BY date_time DESC
LIMIT 10;
```

If you see LOGIN, LOGOUT, PRODUCT_ADD, etc., audit system works. Just no sales because they're failing.

---

### **Issue 4: Inconsistent Availability**

**Cause:** Data not refreshing when navigating between screens

**Current behavior:**
- OrderProcessScreen calculates available quantities based on `viewModel3.productList`
- When navigating, the list might be stale

**Temporary workaround:**
1. On OrderProcess screen, **pull down to refresh** (if implemented)
2. Or go to **Inventory List** → **Refresh** → back to Order Process

**Proper fix:** (I'll implement in next commit)
- Force data refresh on screen entry
- Clear cached calculations

---

### **Issue 5: Overview Not Working**

**Need more info:** What does "not working" mean?
- Not loading data?
- Showing 0 for everything?
- Crashing?

**To check:**
1. Go to Overview screen
2. Check Android Studio Logcat
3. Look for errors

**Send me the error message and I'll fix it!**

---

## 📊 Diagnostic Checklist

Before saying "sales are failing," verify:

- [ ] PostgreSQL is running
- [ ] API server is running (`node server.js`)
- [ ] Mobile app can connect to API
- [ ] Ingredients have stock in **Inventory B** (not just A)
- [ ] Recipe exists for the product
- [ ] Ingredient stock >= recipe requirements

---

## 🎯 Most Likely Issue

Based on your description:
> "Recipe and ingredients working properly but sales fail"

**Diagnosis:** Stock is in **Inventory A** but **Inventory B is 0**.

**Evidence:**
- You said you added stock (goes to A)
- But didn't transfer to B yet
- Sales deduct from B first
- If B = 0, sale fails even if A has stock

**Solution:**
1. Go to **Inventory Transfer** screen
2. Transfer each ingredient from A → B:
   - Flour: 1000g
   - Butter: 500g
   - Sugar: 1000g
   - Eggs: 20 pcs
   - etc.
3. Try sale again

---

## 📝 SQL Queries for Quick Checks

### Check Ingredients Ready for Sale:
```sql
SELECT name, inventory_b
FROM products
WHERE category = 'Ingredients'
ORDER BY name;
```
**All ingredients should have inventory_b > 0!**

### Check Last 10 Sales:
```sql
SELECT product_name, quantity, order_date
FROM sales
ORDER BY order_date DESC
LIMIT 10;
```

### Check If Audit Logs Work:
```sql
SELECT COUNT(*) as total_logs, COUNT(DISTINCT action) as different_actions
FROM audit_logs;
```

---

## 🚨 If Still Failing

**Send me:**
1. API server logs (copy the terminal output when sale fails)
2. Mobile app Logcat (filter by "ProductRepository")
3. Result of this SQL query:
```sql
SELECT name, inventory_a, inventory_b FROM products WHERE category = 'Ingredients';
```

I'll diagnose the exact issue!

---

## ✅ Expected Working Flow

1. **Add Ingredient Stock** → Goes to Inventory A ✅
2. **Transfer A → B** → Some stock moves to Inventory B ✅
3. **Process Sale** → API checks Inventory B, deducts ✅
4. **Audit Trail** → Sale logged ✅
5. **Sales Table** → Transaction recorded ✅

If ANY step fails, the whole process fails!
