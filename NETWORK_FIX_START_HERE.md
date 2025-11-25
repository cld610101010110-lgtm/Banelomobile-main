# 🚨 NETWORK CONNECTION ERROR - START HERE

## Your Current Problem

```
Failed to connect to /192.168.254.176:3000
EHOSTUNREACH (No route to host)
```

The mobile app **cannot reach** the API server.

---

## ⚡ QUICK FIX (3 Steps)

### **STEP 1: Start the API Server**

**Option A - Easy Way:**
1. Open File Explorer
2. Navigate to: `Banelomobile-main/api-backend`
3. Double-click: `START_API_SERVER.bat`

**Option B - Manual:**
```bash
cd api-backend
node server.js
```

**You should see:**
```
✅ Connected to PostgreSQL database: banelo_db
✅ Server running on: http://localhost:3000
```

Keep this window open!

---

### **STEP 2: Check Your IP Address**

**Option A - Run Quick Check:**
1. In `api-backend` folder, double-click: `quick-check.bat`
2. Note the IP address shown

**Option B - Manual:**
1. Press `Windows + R`
2. Type `cmd` and press Enter
3. Type `ipconfig` and press Enter
4. Look for "IPv4 Address" under "Wireless LAN adapter Wi-Fi"

**Example:**
```
IPv4 Address. . . . . . . . . . . : 192.168.254.176
                                     ^^^^ This is your IP
```

---

### **STEP 3: Test from Phone's Browser**

1. **Ensure phone is on SAME Wi-Fi** as computer
2. **Open Chrome or Safari on your phone**
3. **Go to:** `http://YOUR_IP:3000/api/products`
   - Replace `YOUR_IP` with the IP from Step 2
   - Example: `http://192.168.254.176:3000/api/products`

**If you see JSON data:**
✅ Connection working! Try logging into the app.

**If you see "Can't reach":**
❌ Firewall is blocking. See detailed guide below.

---

## 📚 Need More Help?

**If Quick Fix didn't work, use these tools:**

### **1. Complete Diagnostics:**
- Read: `NETWORK_DIAGNOSTICS.md`
- Covers all possible issues and solutions

### **2. Automated Testing:**
```bash
cd api-backend
node test-server.js
```

### **3. Quick Windows Check:**
- Run: `api-backend/quick-check.bat`

---

## 🎯 Most Likely Issues

### **Issue 1: API Server Not Running**

**Symptom:** Phone browser shows "Can't reach this page"

**Fix:** Start the server (see Step 1 above)

---

### **Issue 2: IP Address Changed**

**Symptom:** Server running, but phone can't connect

**Fix:**
1. Get current IP (see Step 2 above)
2. Open in Android Studio:
   ```
   app/src/main/java/.../BaneloApiService.kt
   ```
3. Update line 202:
   ```kotlin
   private const val BASE_URL = "http://YOUR_NEW_IP:3000/"
   ```
4. Rebuild and reinstall the app

---

### **Issue 3: Windows Firewall Blocking**

**Symptom:** Works on computer's browser, but not from phone

**Quick Fix:**
1. Windows Search → "Windows Defender Firewall"
2. Click "Allow an app or feature through Windows Defender Firewall"
3. Click "Change settings"
4. Find "Node.js" → Check both "Private" and "Public"
5. Click OK

**Detailed firewall instructions:** See `NETWORK_DIAGNOSTICS.md` Step 5

---

### **Issue 4: PostgreSQL Not Running**

**Symptom:** Server shows database connection error

**Fix:**
1. Press `Windows + R`
2. Type `services.msc` and press Enter
3. Find `postgresql-x64-18`
4. Right-click → Start

---

## ✅ When Everything Works

**Terminal shows:**
```
✅ Connected to PostgreSQL database: banelo_db
✅ Server running on: http://localhost:3000
```

**Phone browser shows:**
```json
{
  "success": true,
  "data": [...]
}
```

**Mobile app:** Can login and process orders

---

## 🆘 Still Stuck?

1. **Run full diagnostics:**
   ```bash
   cd api-backend
   node test-server.js
   ```

2. **Check all prerequisites:**
   - [ ] PostgreSQL running
   - [ ] API server running
   - [ ] IP address matches app config
   - [ ] Phone on same Wi-Fi
   - [ ] Firewall allows Node.js

3. **Read complete guide:** `NETWORK_DIAGNOSTICS.md`

---

## 📌 Remember

- **Keep API server terminal open** while using the app
- **Computer and phone must be on same Wi-Fi**
- **IP address might change** if you restart router
- **Firewall must allow Node.js** on port 3000

---

**All code fixes are complete! This is just a network setup issue.**
