# Banelo POS API - Quick Startup Guide

## Prerequisites Checklist

Before starting the API server, ensure:

- ✅ PostgreSQL 18 is installed on Windows
- ✅ Database `banelo_db` exists with all imported data
- ✅ Node.js is installed (version 18 or higher)
- ✅ You're in the `api-backend` directory

---

## Step 1: Start PostgreSQL (Windows)

### Option A: Using Services

1. Press `Win + R`
2. Type `services.msc` and press Enter
3. Find `postgresql-x64-18`
4. Right-click → Start (if not running)

### Option B: Using Command Prompt (as Administrator)

```cmd
net start postgresql-x64-18
```

### Verify PostgreSQL is Running

```cmd
psql -U postgres -d banelo_db
```

You should see: `banelo_db=#`

Type `\q` to exit.

---

## Step 2: Install Dependencies (First Time Only)

```cmd
cd api-backend
npm install
```

You should see:
```
added 114 packages
found 0 vulnerabilities
```

---

## Step 3: Start the API Server

```cmd
npm start
```

### Expected Output:

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🚀 Banelo POS API Server
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ Server running on: http://localhost:3000
📡 API endpoint: http://localhost:3000/api
📊 Database: PostgreSQL (banelo_db)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Available endpoints:
  Users:     GET/POST/PUT/DELETE /api/users
  Products:  GET/POST/PUT/DELETE /api/products
  Sales:     GET/POST /api/sales
  Recipes:   GET/POST /api/recipes
  Audit:     GET/POST /api/audit
  Waste:     GET/POST /api/waste
  Reports:   GET /api/reports/*
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

**NO errors should appear!**

---

## Step 4: Test the API

### Method 1: Browser

Open your browser and visit:

```
http://localhost:3000/api/products
```

You should see JSON response with all your products!

### Method 2: Command Prompt

```cmd
curl http://localhost:3000/api/products
```

---

## Common Issues & Solutions

### Issue: "Error connecting to PostgreSQL"

**Error Message:**
```
❌ Error connecting to PostgreSQL: Error: connect ECONNREFUSED 127.0.0.1:5432
```

**Solution:**
PostgreSQL is not running. Start it using Step 1 above.

**Verify:**
```cmd
psql -U postgres
```

### Issue: "EADDRINUSE: address already in use"

**Problem:** Port 3000 is already being used by another application.

**Solution A:** Stop the other application using port 3000

**Solution B:** Change the port in `server.js`:
```javascript
const PORT = 3001;  // Change to any available port
```

Then update Android app's `BaneloApiService.kt`:
```kotlin
private const val BASE_URL = "http://10.0.2.2:3001/"  // Match your port
```

### Issue: "Cannot find module 'express'"

**Problem:** Dependencies not installed.

**Solution:**
```cmd
cd api-backend
npm install
```

### Issue: "Database does not exist"

**Problem:** Database `banelo_db` hasn't been created.

**Solution:**
```cmd
psql -U postgres
CREATE DATABASE banelo_db;
\q
```

Then run the data import scripts from `migration/05_csv_import/`

### Issue: Port 5432 Connection Refused

**Check if PostgreSQL is listening:**
```cmd
netstat -an | findstr 5432
```

**Expected:**
```
TCP    0.0.0.0:5432           0.0.0.0:0              LISTENING
TCP    [::]:5432              [::]:0                 LISTENING
```

**If not shown:** PostgreSQL service is not running. Start it using Step 1.

---

## Verifying Everything Works

### Test 1: Get All Products

```cmd
curl http://localhost:3000/api/products
```

**Expected:** JSON with 71 products

### Test 2: Get All Users

```cmd
curl http://localhost:3000/api/users
```

**Expected:** JSON with 20 users

### Test 3: Process a Test Sale

```cmd
curl -X POST http://localhost:3000/api/sales/process ^
  -H "Content-Type: application/json" ^
  -d "{\"productFirebaseId\":\"PROD001\",\"quantity\":1,\"productName\":\"Test Product\",\"category\":\"Pastries\",\"price\":100,\"paymentMode\":\"Cash\",\"cashierUsername\":\"admin\"}"
```

**Expected:**
```json
{
  "success": true,
  "message": "Sale processed - ingredients deducted",
  "ingredientsDeducted": 5
}
```

---

## Development Workflow

### Start Your Dev Environment

1. **Terminal 1 - API Server:**
   ```cmd
   cd api-backend
   npm start
   ```
   Leave this running.

2. **Terminal 2 - Android Studio:**
   - Open your Android project
   - Run the app
   - It will connect to `http://10.0.2.2:3000`

### Stopping the Server

Press `Ctrl + C` in the terminal running `npm start`

### Restarting After Code Changes

If you modify `server.js`:
1. Press `Ctrl + C` to stop
2. Run `npm start` again

No need to reinstall dependencies unless you change `package.json`.

---

## Next Steps

After the API server is running successfully:

1. ✅ Follow `ANDROID_SETUP_GUIDE.md` to update your Android app
2. ✅ Replace Firestore dependencies with Retrofit
3. ✅ Copy `BaneloApiService.kt` to your app
4. ✅ Replace `ProductRepository.kt` with `PostgreSQL_ProductRepository.kt`
5. ✅ Test the full integration

---

## Production Deployment

For production use, consider:

1. **Environment Variables:**
   ```javascript
   const pool = new Pool({
       host: process.env.DB_HOST || 'localhost',
       port: process.env.DB_PORT || 5432,
       database: process.env.DB_NAME || 'banelo_db',
       user: process.env.DB_USER || 'postgres',
       password: process.env.DB_PASSWORD || 'admin123',
   });
   ```

2. **Deploy to cloud:**
   - Heroku (free tier)
   - Railway (easy PostgreSQL)
   - Render (simple deployment)
   - DigitalOcean (VPS)

3. **Add security:**
   - API authentication tokens
   - Rate limiting
   - HTTPS/SSL
   - Input validation
   - CORS restrictions

---

## Keeping the Server Running (Production)

### Windows Service (Advanced)

Use **node-windows** to run as a Windows service:

```cmd
npm install -g node-windows
```

### PM2 (Recommended)

```cmd
npm install -g pm2
pm2 start server.js --name banelo-api
pm2 startup
pm2 save
```

---

## Troubleshooting Checklist

Run through this checklist if you have issues:

- [ ] PostgreSQL service is running (`services.msc`)
- [ ] Can connect to database (`psql -U postgres -d banelo_db`)
- [ ] Database has data (`SELECT COUNT(*) FROM products;`)
- [ ] Port 3000 is available (`netstat -an | findstr 3000`)
- [ ] Port 5432 is listening (`netstat -an | findstr 5432`)
- [ ] Node.js is installed (`node --version`)
- [ ] Dependencies are installed (`npm install`)
- [ ] No firewall blocking ports 3000 or 5432
- [ ] Correct database credentials in `server.js`

---

**You're all set! The API server should be running smoothly.** 🚀

For Android app integration, see: `ANDROID_SETUP_GUIDE.md`
