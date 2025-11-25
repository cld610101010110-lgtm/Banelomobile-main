# 🌐 NETWORK CONNECTIVITY DIAGNOSTIC GUIDE

## Current Configuration
- **API Server IP**: `192.168.254.176`
- **API Server Port**: `3000`
- **Mobile App Expecting**: `http://192.168.254.176:3000/`

## Error You're Seeing
```
Failed to connect to /192.168.254.176:3000
EHOSTUNREACH (No route to host)
```

This means the mobile device **cannot reach** the API server at all.

---

## 🔍 STEP-BY-STEP DIAGNOSTICS

### **STEP 1: Check if API Server is Running**

Open a **new terminal** window and run:

```bash
cd api-backend
node server.js
```

**Expected Output:**
```
✅ Connected to PostgreSQL database: banelo_db
✅ Server running on: http://localhost:3000
```

**If you see an error:**
- `PostgreSQL connection error` → PostgreSQL is not running (see Step 2)
- `Port 3000 already in use` → Server is already running (good!)
- `Cannot find module` → Run `npm install` first

---

### **STEP 2: Check if PostgreSQL is Running**

**Windows:**
1. Press `Windows + R`
2. Type `services.msc` and press Enter
3. Find `postgresql-x64-18` (or similar)
4. Check Status column - should say **"Running"**
5. If not running: Right-click → Start

**Alternative (Command Line):**
```bash
psql -U postgres -d banelo_db -c "SELECT 1"
```

If this works, PostgreSQL is running.

---

### **STEP 3: Verify Computer's Current IP Address**

**CRITICAL**: Your IP might have changed!

**Windows (Command Prompt):**
```bash
ipconfig
```

**Look for:**
```
Wireless LAN adapter Wi-Fi:
   IPv4 Address. . . . . . . . . . . : 192.168.254.176
```

**If IP is DIFFERENT from 192.168.254.176:**
- Note the new IP address
- You'll need to update the mobile app config (see Step 7)

---

### **STEP 4: Test API from Computer's Browser**

Open your browser on the **same computer** running the API:

1. **Test localhost:**
   - Go to: `http://localhost:3000/api/products`
   - Should see JSON response with products

2. **Test IP address:**
   - Go to: `http://192.168.254.176:3000/api/products`
   - Should see the same JSON response

**If localhost works but IP doesn't:**
- Server is running but firewall is blocking external access (see Step 5)

**If neither works:**
- API server is not running or crashed (go back to Step 1)

---

### **STEP 5: Check Windows Firewall**

The firewall might be blocking port 3000.

**Option A: Allow Node.js through Firewall**
1. Windows Search → "Windows Defender Firewall"
2. Click "Allow an app or feature through Windows Defender Firewall"
3. Click "Change settings" button
4. Find "Node.js" in the list
5. Check BOTH "Private" and "Public" boxes
6. Click OK

**Option B: Create Firewall Rule for Port 3000**
1. Windows Search → "Windows Defender Firewall with Advanced Security"
2. Click "Inbound Rules" → "New Rule..."
3. Select "Port" → Next
4. Select "TCP" → Specific local ports: `3000` → Next
5. Select "Allow the connection" → Next
6. Check all profiles (Domain, Private, Public) → Next
7. Name: "Banelo API Server" → Finish

**Test again from Step 4**

---

### **STEP 6: Test from Mobile Device**

On your **mobile phone**:

1. **Ensure phone is on SAME Wi-Fi network as computer**
   - Go to phone Settings → Wi-Fi
   - Check network name matches computer's network

2. **Open phone's browser (Chrome/Safari)**
   - Go to: `http://192.168.254.176:3000/api/products`

**If you see JSON:**
- ✅ Network connection is working!
- Problem is in the mobile app configuration

**If you see "Can't reach this page" or timeout:**
- ❌ Network connection blocked
- Most likely firewall (go back to Step 5)
- Or IP address changed (go back to Step 3)

---

### **STEP 7: Update Mobile App IP (if IP changed)**

If your computer's IP is now **different** from `192.168.254.176`:

1. Open the project in Android Studio

2. Navigate to:
   ```
   app/src/main/java/com/project/.../BaneloApiService.kt
   ```

3. Find line 202:
   ```kotlin
   private const val BASE_URL = "http://192.168.254.176:3000/"
   ```

4. Change to your **new IP address**:
   ```kotlin
   private const val BASE_URL = "http://YOUR_NEW_IP:3000/"
   ```

5. **Build → Rebuild Project**

6. **Run → Run 'app'** to reinstall on your phone

---

## 🚨 QUICK CHECKLIST

Before testing the app, verify ALL of these:

- [ ] PostgreSQL service is running
- [ ] API server is running (`node server.js` in terminal)
- [ ] Server shows: `Server running on: http://localhost:3000`
- [ ] Browser test `http://localhost:3000/api/products` works
- [ ] Computer's current IP matches the one in BaneloApiService.kt (line 202)
- [ ] Firewall allows Node.js or port 3000
- [ ] Phone is on same Wi-Fi network as computer
- [ ] Phone's browser can access `http://YOUR_IP:3000/api/products`

---

## 🔧 COMMON SOLUTIONS

### **Problem: IP Address Keeps Changing**

If your computer's IP changes frequently, you have 3 options:

**Option 1: Set Static IP (Recommended)**
1. Windows Settings → Network & Internet → Wi-Fi
2. Click your network → Hardware properties
3. Click "Edit" next to IP assignment
4. Change from "Automatic (DHCP)" to "Manual"
5. Turn on IPv4
6. Set IP: `192.168.254.176`
7. Subnet: `255.255.255.0`
8. Gateway: `192.168.254.1` (your router's IP)
9. DNS: `8.8.8.8`
10. Save

**Option 2: Use Emulator Instead**
- Change BASE_URL to `http://10.0.2.2:3000/`
- Run app in Android Emulator instead of physical device

**Option 3: Use Dynamic DNS or Local Domain**
- More complex, not recommended for development

---

### **Problem: Port 3000 Already in Use**

If you see "Port 3000 is already in use":

**Find and kill the process:**

Windows:
```bash
netstat -ano | findstr :3000
taskkill /PID <PID_NUMBER> /F
```

Then restart: `node server.js`

---

### **Problem: Server Crashes on Startup**

Check the error message. Common causes:

1. **PostgreSQL not running** → Start PostgreSQL (Step 2)
2. **Wrong database credentials** → Check `api-backend/server.js` database config
3. **Missing dependencies** → Run `npm install` in api-backend folder

---

## 📊 Test Script (Windows)

Save this as `test-connection.bat` and run it:

```batch
@echo off
echo ================================
echo Banelo API Connection Test
echo ================================
echo.

echo [1] Checking if PostgreSQL is running...
sc query postgresql-x64-18 | find "RUNNING"
if %errorlevel%==0 (
    echo ✅ PostgreSQL is running
) else (
    echo ❌ PostgreSQL is NOT running
    echo    Start it: services.msc → postgresql-x64-18 → Start
)
echo.

echo [2] Checking your IP address...
ipconfig | findstr IPv4
echo.

echo [3] Testing API on localhost...
curl -s http://localhost:3000/api/products >nul 2>&1
if %errorlevel%==0 (
    echo ✅ API is responding on localhost
) else (
    echo ❌ API is NOT responding on localhost
    echo    Start it: cd api-backend ^&^& node server.js
)
echo.

echo [4] Testing API on IP address...
curl -s http://192.168.254.176:3000/api/products >nul 2>&1
if %errorlevel%==0 (
    echo ✅ API is accessible via IP
) else (
    echo ❌ API is NOT accessible via IP
    echo    Check firewall settings
)
echo.

echo ================================
echo Test Complete
echo ================================
pause
```

---

## ✅ EXPECTED WORKING STATE

When everything is configured correctly:

1. **Terminal shows:**
   ```
   ✅ Connected to PostgreSQL database: banelo_db
   ✅ Server running on: http://localhost:3000
   ```

2. **Browser (http://192.168.254.176:3000/api/products) shows:**
   ```json
   {
     "success": true,
     "data": [
       {"name": "Almond Croissant", ...},
       ...
     ]
   }
   ```

3. **Mobile app can login and fetch data**

---

## 🆘 IF STILL NOT WORKING

If you've tried all steps and it's still not working:

1. **Restart everything:**
   - Stop API server (Ctrl+C)
   - Restart PostgreSQL service
   - Restart your computer
   - Start API server again
   - Rebuild and reinstall mobile app

2. **Check if antivirus is blocking:**
   - Temporarily disable antivirus
   - Test connection
   - If it works, add exception for Node.js

3. **Try with computer's firewall completely off:**
   - Windows Search → "Windows Defender Firewall"
   - Turn Windows Defender Firewall on or off
   - Turn off for Private networks (temporarily!)
   - Test connection
   - **Remember to turn it back on!**

4. **Verify network configuration:**
   - Ping test from phone to computer
   - Check router settings
   - Ensure no VPN is active on either device
