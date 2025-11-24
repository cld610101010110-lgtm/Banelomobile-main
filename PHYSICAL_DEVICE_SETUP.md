# 📱 Physical Device Setup Guide

## Issue: Connection Timeout on Physical Device

If you see this error:
```
failed to connect to /10.0.2.2 (port 3000) after 10000ms
SocketTimeoutException
```

This means you're using a **physical Android device**, and `10.0.2.2` only works for emulators.

---

## ✅ Solution: 3 Steps

### **Step 1: Find Your Computer's IP Address**

**Windows:**
```bash
ipconfig
```
Look for "IPv4 Address" - example: `192.168.254.100`

**Mac:**
```bash
ifconfig | grep "inet " | grep -v 127.0.0.1
```

**Linux:**
```bash
ip addr show | grep "inet " | grep -v 127.0.0.1
```

Your IP will look like:
- `192.168.1.XXX`
- `192.168.254.XXX`
- `10.0.XXX.XXX`

### **Step 2: Update BaneloApiService.kt**

File: `app/src/main/java/com/project/dba_delatorre_dometita_ramirez_tan/BaneloApiService.kt`

**Find line 162:**
```kotlin
private const val BASE_URL = "http://10.0.2.2:3000/"
```

**Change to (replace with YOUR computer's IP):**
```kotlin
private const val BASE_URL = "http://192.168.254.100:3000/"  // ← Use YOUR IP!
```

### **Step 3: Ensure Both Devices on Same Network**

✅ Computer and phone must be on the **same WiFi network**
✅ Computer firewall must allow port 3000
✅ API server must be running: `node server.js`

---

## 🔥 Windows Firewall (if needed)

If connection still fails, allow port 3000 through firewall:

**PowerShell (run as Administrator):**
```powershell
New-NetFirewallRule -DisplayName "Node API Server" -Direction Inbound -LocalPort 3000 -Protocol TCP -Action Allow
```

**Or manually:**
1. Open Windows Defender Firewall
2. Advanced Settings → Inbound Rules
3. New Rule → Port → TCP → 3000 → Allow

---

## 🧪 Testing

### **1. Test API from Computer:**
```bash
curl http://localhost:3000/api/users
```
Should return JSON with users.

### **2. Test API from Phone:**
Open browser on phone and go to:
```
http://YOUR_COMPUTER_IP:3000/api/users
```
Example: `http://192.168.254.100:3000/api/users`

Should show JSON data.

### **3. If browser works but app doesn't:**
- Rebuild the Android app (Clean + Rebuild)
- Check BaneloApiService.kt has correct IP
- Check network_security_config.xml allows your IP range

---

## 📝 Quick Reference

| Device Type | BASE_URL to Use |
|-------------|----------------|
| **Android Emulator** | `http://10.0.2.2:3000/` |
| **Physical Device** | `http://YOUR_COMPUTER_IP:3000/` |
| **Production** | `https://your-api.com/` |

---

## ⚠️ Troubleshooting

### "Still can't connect"

**Check 1: Both on same WiFi?**
```bash
# Computer
ipconfig  # Windows
ifconfig  # Mac/Linux

# Phone
Settings → WiFi → Your Network → IP Address
```
First 3 numbers should match (e.g., both `192.168.254.xxx`)

**Check 2: API Server Running?**
```bash
node server.js
# Should show: ✅ Server running on: http://localhost:3000
```

**Check 3: Firewall Blocking?**
```bash
# Test from another device on same network
curl http://YOUR_COMPUTER_IP:3000/api/users
```

**Check 4: Correct IP in Code?**
```kotlin
// In BaneloApiService.kt line 162
private const val BASE_URL = "http://192.168.254.XXX:3000/"  // ← Check this!
```

---

## 🚀 For Emulator Users

If you're using an **Android Emulator**, use:
```kotlin
private const val BASE_URL = "http://10.0.2.2:3000/"
```
No other changes needed!

---

## 🔒 Security Note

The `network_security_config.xml` has been updated to allow HTTP for:
- ✅ `10.0.2.2` (Emulator)
- ✅ `192.168.1.x` (Common home network)
- ✅ `192.168.254.x` (Your current network)
- ✅ `10.0.x.x` (Some corporate networks)

This is **safe for development**. For production, use HTTPS.

---

## 💡 Pro Tip: Dynamic URL Selection

For easier switching between emulator and device, you can use:

```kotlin
object BaneloApiService {
    private const val USE_EMULATOR = false  // ← Toggle this

    private const val COMPUTER_IP = "192.168.254.100"  // ← Your computer's IP

    private val BASE_URL = if (USE_EMULATOR) {
        "http://10.0.2.2:3000/"
    } else {
        "http://$COMPUTER_IP:3000/"
    }

    // ... rest of code
}
```

Then just toggle `USE_EMULATOR` when switching between devices!
