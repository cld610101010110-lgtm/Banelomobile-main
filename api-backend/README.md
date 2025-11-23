# Banelo POS - REST API Backend

Complete REST API for Banelo POS system with PostgreSQL database.

**This replaces the problematic JDBC driver approach and provides a production-ready solution.**

---

## 🚀 Quick Start (10 Minutes)

### Step 1: Install Node.js

If not installed, download from: https://nodejs.org/
- Download LTS version (recommended)
- Install with default settings

Verify installation:
```cmd
node --version
npm --version
```

### Step 2: Install Dependencies

```cmd
cd api-backend
npm install
```

This installs:
- **express**: Web server framework
- **pg**: PostgreSQL client
- **cors**: Cross-origin resource sharing
- **body-parser**: Parse JSON requests

### Step 3: Verify PostgreSQL is Running

```cmd
psql -U postgres -d banelo_db
```

You should see: `banelo_db=#`

Type `\q` to exit.

### Step 4: Start the API Server

```cmd
npm start
```

You should see:
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🚀 Banelo POS API Server
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ Server running on: http://localhost:3000
📡 API endpoint: http://localhost:3000/api
📊 Database: PostgreSQL (banelo_db)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### Step 5: Test the API

Open a browser and go to:
```
http://localhost:3000/api/products
```

You should see JSON response with your products!

---

## 📋 Available API Endpoints

### Users / Accounts
- `GET /api/users` - Get all users
- `POST /api/users/login` - Login
- `POST /api/users` - Create user
- `PUT /api/users/:firebaseId` - Update user
- `DELETE /api/users/:firebaseId` - Delete user

### Products / Inventory
- `GET /api/products` - Get all products
- `GET /api/products/category/:category` - Get by category
- `GET /api/products/:firebaseId` - Get single product
- `POST /api/products` - Create product
- `PUT /api/products/:firebaseId` - Update product
- `DELETE /api/products/:firebaseId` - Delete product
- `POST /api/products/transfer` - Transfer inventory A→B

### Sales (Ingredient-Based Deduction)
- `GET /api/sales` - Get all sales
- `GET /api/sales/range` - Get sales by date
- `POST /api/sales/process` - **Process sale with ingredient deduction**

### Recipes
- `GET /api/recipes` - Get all recipes with ingredients
- `GET /api/recipes/product/:firebaseId` - Get recipe for product
- `POST /api/recipes` - Create recipe

### Audit Logs
- `GET /api/audit` - Get audit logs
- `POST /api/audit` - Create audit log

### Waste Logs
- `GET /api/waste` - Get waste logs
- `POST /api/waste` - Create waste log (deducts inventory)

### Reports
- `GET /api/reports/sales-summary` - Sales summary by date range
- `GET /api/reports/top-products` - Top selling products
- `GET /api/reports/low-stock` - Low stock alerts

---

## 🔥 Key Feature: Ingredient-Based Deduction

When you process a sale:

**Request:**
```json
POST /api/sales/process
{
  "productFirebaseId": "abc123",
  "quantity": 2,
  "productName": "Chocolate Cake",
  "category": "Pastries",
  "price": 250.00,
  "paymentMode": "Cash",
  "gcashReferenceId": null,
  "cashierUsername": "admin"
}
```

**What Happens:**
1. ✅ Records the sale (product name, price, date)
2. ✅ Finds the recipe for Chocolate Cake
3. ✅ Deducts ingredients from inventory:
   - 1000g Flour (500g × 2)
   - 600g Sugar (300g × 2)
   - 400g Butter (200g × 2)
   - 8 Eggs (4 × 2)
   - 200g Cocoa Powder (100g × 2)
4. ✅ Deducts from Inventory B first, then A
5. ✅ Returns success with ingredients deducted count

**Response:**
```json
{
  "success": true,
  "message": "Sale processed - ingredients deducted",
  "ingredientsDeducted": 5
}
```

---

## 🔧 Configuration

### Database Connection

Edit `server.js` if needed:

```javascript
const pool = new Pool({
    host: 'localhost',
    port: 5432,
    database: 'banelo_db',
    user: 'postgres',
    password: 'admin123',  // Change if different
});
```

### Port

Default: `3000`

To change:
```javascript
const PORT = 3000;  // Change to your preferred port
```

---

## 🧪 Testing API Endpoints

### Using Browser

```
http://localhost:3000/api/products
http://localhost:3000/api/users
http://localhost:3000/api/sales
```

### Using curl

```cmd
# Get all products
curl http://localhost:3000/api/products

# Create product
curl -X POST http://localhost:3000/api/products ^
  -H "Content-Type: application/json" ^
  -d "{\"name\":\"Test Cake\",\"category\":\"Pastries\",\"price\":100,\"quantity\":50,\"inventory_a\":30,\"inventory_b\":20,\"cost_per_unit\":40}"

# Process sale
curl -X POST http://localhost:3000/api/sales/process ^
  -H "Content-Type: application/json" ^
  -d "{\"productFirebaseId\":\"abc123\",\"quantity\":1,\"productName\":\"Chocolate Cake\",\"category\":\"Pastries\",\"price\":250,\"paymentMode\":\"Cash\",\"cashierUsername\":\"admin\"}"
```

### Using Postman

1. Download Postman: https://www.postman.com/downloads/
2. Import the API endpoints
3. Test each endpoint

---

## 🐛 Troubleshooting

### "Error connecting to PostgreSQL"

**Check if PostgreSQL is running:**
```cmd
psql -U postgres
```

**Start PostgreSQL if not running:**
```cmd
net start postgresql-x64-18
```

### "EADDRINUSE: address already in use"

**Port 3000 is already in use. Options:**

1. Stop other app using port 3000
2. Change PORT in server.js to different number (e.g., 3001)

### "Cannot find module 'express'"

**Dependencies not installed:**
```cmd
npm install
```

### "Database does not exist"

**Create database:**
```cmd
psql -U postgres
CREATE DATABASE banelo_db;
\q
```

---

## 🔒 Security Notes

**For Development:**
- ✅ Works as-is for local testing

**For Production:**
- ⚠️ Move database credentials to environment variables
- ⚠️ Add authentication/API keys
- ⚠️ Enable HTTPS
- ⚠️ Add rate limiting
- ⚠️ Use proper error handling
- ⚠️ Add input validation

Example `.env` file:
```
DB_HOST=localhost
DB_PORT=5432
DB_NAME=banelo_db
DB_USER=postgres
DB_PASSWORD=admin123
PORT=3000
```

---

## 📱 Connecting from Android

### Android Emulator

Use: `http://10.0.2.2:3000`

Already configured in `BaneloApiService.kt`

### Physical Android Device

1. Find your computer's IP:
   ```cmd
   ipconfig
   ```
   Look for "IPv4 Address" (e.g., 192.168.1.100)

2. Update `BaneloApiService.kt`:
   ```kotlin
   private const val BASE_URL = "http://192.168.1.100:3000/"
   ```

3. Make sure both devices are on same WiFi network

---

## 🚀 Deployment

### Option 1: Run on Your Computer

Keep the API running on your development computer:
```cmd
npm start
```

Your Android app connects to it via network.

### Option 2: Deploy to Cloud

Deploy to:
- **Heroku**: Free tier available
- **Railway**: Easy PostgreSQL + Node.js deployment
- **Render**: Simple deployment
- **DigitalOcean**: VPS with PostgreSQL

Example for Railway:
1. Push code to GitHub
2. Connect Railway to your repo
3. Railway auto-detects Node.js
4. Add PostgreSQL plugin
5. Deploy!

---

## ✅ Benefits Over JDBC Approach

| Aspect | JDBC (Old) | REST API (New) |
|--------|-----------|---------------|
| Setup | ❌ Complex, driver errors | ✅ Simple, npm install |
| Android Build | ❌ Netty conflicts | ✅ No conflicts |
| Security | ❌ DB credentials in app | ✅ Credentials on server |
| Maintenance | ❌ Hard to update | ✅ Easy server updates |
| Production Ready | ❌ Not recommended | ✅ Industry standard |
| Performance | ❌ Direct connections | ✅ Connection pooling |

---

## 📝 Next Steps

1. ✅ Start API server (`npm start`)
2. ✅ Test endpoints in browser
3. ✅ Update Android app build.gradle (see Android guide)
4. ✅ Copy `BaneloApiService.kt` and `PostgreSQL_ProductRepository.kt` to your app
5. ✅ Replace Firestore calls with API calls
6. ✅ Test sales with ingredient deduction
7. ✅ Deploy to production when ready

---

**API is ready! Now update your Android app to use it.** 🎉

See: `migration/07_android_adapter/ANDROID_SETUP_GUIDE.md`
