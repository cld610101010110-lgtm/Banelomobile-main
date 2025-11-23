# 📱 API Quick Reference Card

**Base URL:** `http://10.0.2.2:3000` (Android Emulator)

---

## Products
| Action | Endpoint | Method | Android Code |
|--------|----------|--------|---|
| Get All | `/api/products` | GET | `BaneloApiService.api.getAllProducts()` |
| Get by Category | `/api/products/category/{cat}` | GET | `BaneloApiService.api.getProductsByCategory("Pastries")` |
| Get Single | `/api/products/{id}` | GET | `BaneloApiService.api.getProduct(id)` |
| Create | `/api/products` | POST | `BaneloApiService.api.createProduct(request)` |
| Update | `/api/products/{id}` | PUT | `BaneloApiService.api.updateProduct(id, request)` |
| Delete | `/api/products/{id}` | DELETE | `BaneloApiService.api.deleteProduct(id)` |

---

## Users & Login
| Action | Endpoint | Method | Android Code |
|--------|----------|--------|---|
| Login | `/api/users/login` | POST | `BaneloApiService.api.login(LoginRequest(username))` |
| Get All | `/api/users` | GET | `BaneloApiService.api.getAllUsers()` |
| Create | `/api/users` | POST | `BaneloApiService.api.createUser(jsonData)` |
| Update | `/api/users/{id}` | PUT | `BaneloApiService.api.updateUser(id, jsonData)` |

---

## Sales (Ingredient-Based)
| Action | Endpoint | Method | Android Code |
|--------|----------|--------|---|
| Process Sale | `/api/sales/process` | POST | `BaneloApiService.api.processSale(SalesRequest(...))` |
| Get All Sales | `/api/sales` | GET | `BaneloApiService.api.getAllSales()` |

**What happens when you process a sale:**
1. ✅ Records the sale
2. ✅ Finds recipe for product
3. ✅ Deducts ingredients from inventory
4. ✅ Returns count of ingredients deducted

---

## Recipes
| Action | Endpoint | Method | Android Code |
|--------|----------|--------|---|
| Get All | `/api/recipes` | GET | `BaneloApiService.api.getAllRecipes()` |
| Get for Product | `/api/recipes/product/{id}` | GET | `BaneloApiService.api.getRecipeForProduct(id)` |

---

## Audit Logs
| Action | Endpoint | Method | Android Code |
|--------|----------|--------|---|
| Get All | `/api/audit` | GET | `BaneloApiService.api.getAuditLogs()` |
| Create | `/api/audit` | POST | `BaneloApiService.api.createAuditLog(jsonData)` |

---

## Waste Logs
| Action | Endpoint | Method | Android Code |
|--------|----------|--------|---|
| Get All | `/api/waste` | GET | `BaneloApiService.api.getWasteLogs()` |
| Create | `/api/waste` | POST | `BaneloApiService.api.createWasteLog(jsonData)` |

---

## Error Handling Template

```kotlin
val result = safeCall {
    BaneloApiService.api.getAllProducts()
}

if (result.isSuccess) {
    val products = result.getOrNull()
    Log.d("TAG", "✅ Success: ${products?.size} products")
} else {
    Log.e("TAG", "❌ Error: ${result.exceptionOrNull()?.message}")
}
```

---

## Testing the API (Before Android Changes)

### In Command Prompt:
```bash
# Get all products
curl http://localhost:3000/api/products

# Get users
curl http://localhost:3000/api/users
```

### In Browser:
```
http://localhost:3000/api/products
http://localhost:3000/api/users
http://localhost:3000/api/sales
```

---

## Data Classes You'll Use

### ProductRequest
```kotlin
ProductRequest(
    name = "Chocolate Cake",
    category = "Pastries",
    price = 250.0,
    quantity = 10,
    inventory_a = 5,
    inventory_b = 5,
    cost_per_unit = 100.0,
    imageUri = "http://..."
)
```

### SalesRequest
```kotlin
SalesRequest(
    productFirebaseId = "abc123",
    quantity = 2,
    productName = "Chocolate Cake",
    category = "Pastries",
    price = 250.0,
    paymentMode = "Cash",
    gcashReferenceId = null,
    cashierUsername = "admin"
)
```

### LoginRequest
```kotlin
LoginRequest(username = "admin")
```

---

## Gotchas to Remember

⚠️ **Always use `http://10.0.2.2:3000` for Android Emulator**
- Not `localhost:3000` ❌
- Not `127.0.0.1:3000` ❌
- Use `10.0.2.2:3000` ✅

⚠️ **For Physical Device:**
- Find your computer's IP: `ipconfig` in cmd
- Use `http://192.168.x.x:3000` (your actual IP)
- Make sure device is on same WiFi

⚠️ **API Server must be running**
- Keep command prompt open with `npm start`
- Without it, all API calls will fail

---

Done! Copy-paste the code snippets from the main guide into your Android files.
