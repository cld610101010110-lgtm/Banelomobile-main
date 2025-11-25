# Network Connectivity Troubleshooting Guide

## Problem
Android app can't connect to API server at `192.168.254.176:3000`
```
Error: EHOSTUNREACH (No route to host)
```

## Your Current Setup ✅
- **Server IP**: 192.168.254.176
- **Server Port**: 3000
- **Server Status**: Running on 0.0.0.0:3000
- **App Config**: http://192.168.254.176:3000/

## Root Cause
Most likely **Windows Firewall is blocking** incoming connections on port 3000.

---

## Solution 1: Add Windows Firewall Rule (RECOMMENDED)

### Step 1: Open PowerShell as Administrator
1. Press `Win + X`
2. Click "Windows PowerShell (Admin)" or "Terminal (Admin)"

### Step 2: Add Firewall Rule for Port 3000
Run this command in PowerShell:

```powershell
New-NetFirewallRule -DisplayName "Node.js API Server Port 3000" -Direction Inbound -LocalPort 3000 -Protocol TCP -Action Allow
```

Expected output:
```
Name                  : {GUID}
DisplayName           : Node.js API Server Port 3000
Enabled               : True
```

### Step 3: Verify Firewall Rule
```powershell
Get-NetFirewallRule -DisplayName "Node.js API Server Port 3000" | Format-List
```

---

## Solution 2: Test Network Connectivity

### From Your PC (where server is running):

#### Test 1: Can server bind to the port?
```powershell
netstat -ano | findstr :3000
```
**Expected**: Should show `0.0.0.0:3000` or `[::]:3000` with status LISTENING

#### Test 2: Test from localhost
```powershell
curl http://localhost:3000/api/users
```
**Expected**: Should return API response (even if empty or error, it means server responds)

#### Test 3: Test from your PC's IP
```powershell
curl http://192.168.254.176:3000/api/users
```
**Expected**: Should return same response as Test 2

### From Another Device on Same WiFi:

#### Test 4: Can you ping the server?
On Android device, open a terminal app or use your phone's browser:
```
http://192.168.254.176:3000/api/users
```

---

## Solution 3: Temporary Fix - Disable Firewall (NOT RECOMMENDED)

**⚠️ WARNING**: Only do this for testing! Re-enable afterward!

```powershell
# Disable (temporary testing only)
Set-NetFirewallProfile -Profile Domain,Public,Private -Enabled False

# Test your app connection

# Re-enable (IMPORTANT!)
Set-NetFirewallProfile -Profile Domain,Public,Private -Enabled True
```

---

## Solution 4: Check Network Settings

### Verify Both Devices Are on Same Network:

#### On PC:
```powershell
ipconfig
```
Look for:
- **IPv4 Address**: 192.168.254.176
- **Subnet Mask**: 255.255.255.0
- **Default Gateway**: 192.168.254.254

#### On Android Device:
1. Go to Settings → WiFi
2. Tap on connected network
3. Check IP address (should be `192.168.254.xxx`)
4. Check Gateway (should be `192.168.254.254`)

**If different**: Connect Android to the SAME WiFi network as your PC!

---

## Solution 5: Allow Node.js Through Firewall GUI

If PowerShell doesn't work, use Windows Firewall GUI:

1. Open "Windows Defender Firewall with Advanced Security"
   - Press `Win + R`
   - Type: `wf.msc`
   - Press Enter

2. Click "Inbound Rules" on the left

3. Click "New Rule..." on the right

4. Select "Port" → Next

5. Select "TCP" and enter "3000" in Specific local ports → Next

6. Select "Allow the connection" → Next

7. Check all: Domain, Private, Public → Next

8. Name: "Node.js API Server Port 3000" → Finish

---

## Quick Test After Fix

### On Android Device:
Open Chrome browser and go to:
```
http://192.168.254.176:3000/api/users
```

**Success**: You see JSON data or API response
**Still Failing**: Continue to Advanced Troubleshooting below

---

## Advanced Troubleshooting

### Check if Another Process is Using Port 3000:
```powershell
netstat -ano | findstr :3000
```
If you see multiple entries, another app might be interfering.

### Check Node.js Server Configuration:
In your `server.js`, verify:
```javascript
app.listen(3000, '0.0.0.0', () => {
  console.log('Server running on http://0.0.0.0:3000');
});
```

### Check Router/WiFi Settings:
Some routers have "AP Isolation" enabled which prevents devices from talking to each other.
- Log into router (usually http://192.168.254.254)
- Look for "AP Isolation" or "Client Isolation"
- Disable it if enabled

---

## Common Issues & Fixes

### Issue: "Connection timeout"
**Fix**: Firewall is blocking → Follow Solution 1

### Issue: "Connection refused"
**Fix**: Server isn't running or wrong port → Restart server

### Issue: "No route to host"
**Fix**: Wrong network or firewall → Check Solution 1 & 4

### Issue: Works on emulator but not physical device
**Fix**: Emulator uses different network stack
- Emulator: Use `10.0.2.2:3000`
- Physical device: Use `192.168.254.176:3000`

---

## Quick Commands Summary

```powershell
# 1. Add firewall rule
New-NetFirewallRule -DisplayName "Node.js API Server Port 3000" -Direction Inbound -LocalPort 3000 -Protocol TCP -Action Allow

# 2. Verify firewall rule
Get-NetFirewallRule -DisplayName "Node.js API Server Port 3000"

# 3. Test from PC
curl http://192.168.254.176:3000/api/users

# 4. Check what's using port 3000
netstat -ano | findstr :3000
```

---

## Still Not Working?

If you've tried everything above and it still doesn't work:

1. **Restart both devices** (PC and Android)
2. **Restart your WiFi router**
3. **Check antivirus software** - some antivirus programs block network traffic
4. **Try a different port** - Change server to port 8080 and update app config

### To change port to 8080:

**In server.js:**
```javascript
app.listen(8080, '0.0.0.0', () => {
  console.log('Server running on http://0.0.0.0:8080');
});
```

**In BaneloApiService.kt:**
```kotlin
private const val BASE_URL = "http://192.168.254.176:8080/"
```

**Add firewall rule for 8080:**
```powershell
New-NetFirewallRule -DisplayName "Node.js API Server Port 8080" -Direction Inbound -LocalPort 8080 -Protocol TCP -Action Allow
```

---

## Success Checklist ✅

Once working, you should see:
- ✅ No more "EHOSTUNREACH" errors in Android logs
- ✅ API calls succeed in the app
- ✅ "✅ Audit log saved to API" in logs (not the warning)
- ✅ Products sync from server
- ✅ Login works properly

---

## Need More Help?

Run this diagnostic command and share the output:
```powershell
# Run this and share output
Write-Host "=== Network Diagnostic ===" -ForegroundColor Green
Write-Host "`n1. Server Port Status:" -ForegroundColor Yellow
netstat -ano | findstr :3000
Write-Host "`n2. Firewall Rules for Port 3000:" -ForegroundColor Yellow
Get-NetFirewallRule | Where-Object {$_.DisplayName -like "*3000*"} | Format-Table DisplayName, Enabled, Direction, Action
Write-Host "`n3. Network Interfaces:" -ForegroundColor Yellow
ipconfig | findstr /C:"IPv4" /C:"Subnet"
```
