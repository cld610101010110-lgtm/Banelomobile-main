# ============================================
# Banelo Mobile App - Network Fix Script
# ============================================
# This script fixes the network connectivity
# issue by adding Windows Firewall rules
# ============================================

Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "🔧 Banelo Network Connectivity Fix" -ForegroundColor Green
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan

# Check if running as Administrator
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

if (-not $isAdmin) {
    Write-Host "❌ ERROR: This script must be run as Administrator!" -ForegroundColor Red
    Write-Host ""
    Write-Host "How to run as Administrator:" -ForegroundColor Yellow
    Write-Host "1. Press Win + X" -ForegroundColor White
    Write-Host "2. Click 'Windows PowerShell (Admin)' or 'Terminal (Admin)'" -ForegroundColor White
    Write-Host "3. Navigate to this directory and run the script again" -ForegroundColor White
    Write-Host ""
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host ""
Write-Host "✅ Running with Administrator privileges" -ForegroundColor Green
Write-Host ""

# ============================================
# Step 1: Check current configuration
# ============================================
Write-Host "📋 Step 1: Checking current configuration..." -ForegroundColor Yellow
Write-Host ""

# Check if port 3000 is in use
Write-Host "   Checking if port 3000 is in use..." -ForegroundColor White
$port3000 = netstat -ano | Select-String ":3000"
if ($port3000) {
    Write-Host "   ✅ Port 3000 is in use (server is running)" -ForegroundColor Green
    $port3000 | ForEach-Object { Write-Host "      $_" -ForegroundColor Gray }
} else {
    Write-Host "   ⚠️  Port 3000 is not in use (server may not be running)" -ForegroundColor Yellow
}
Write-Host ""

# Check IP address
Write-Host "   Your IP Address:" -ForegroundColor White
$ipConfig = ipconfig | Select-String "IPv4.*192\.168\."
$ipConfig | ForEach-Object {
    Write-Host "      $_" -ForegroundColor Gray
    if ($_ -match "192\.168\.254\.176") {
        Write-Host "      ✅ Matches server configuration (192.168.254.176)" -ForegroundColor Green
    }
}
Write-Host ""

# Check existing firewall rules
Write-Host "   Checking existing firewall rules for port 3000..." -ForegroundColor White
$existingRule = Get-NetFirewallRule -DisplayName "Node.js API Server Port 3000" -ErrorAction SilentlyContinue
if ($existingRule) {
    Write-Host "   ⚠️  Firewall rule already exists!" -ForegroundColor Yellow
    Write-Host ""
    $choice = Read-Host "   Do you want to recreate it? (Y/N)"
    if ($choice -eq "Y" -or $choice -eq "y") {
        Write-Host "   🗑️  Removing existing rule..." -ForegroundColor Yellow
        Remove-NetFirewallRule -DisplayName "Node.js API Server Port 3000" -ErrorAction SilentlyContinue
        Write-Host "   ✅ Existing rule removed" -ForegroundColor Green
    } else {
        Write-Host "   ℹ️  Keeping existing rule" -ForegroundColor Cyan
        Write-Host ""
        Read-Host "Press Enter to exit"
        exit 0
    }
} else {
    Write-Host "   ℹ️  No existing firewall rule found" -ForegroundColor Cyan
}
Write-Host ""

# ============================================
# Step 2: Add Windows Firewall Rule
# ============================================
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "🔥 Step 2: Adding Windows Firewall Rule..." -ForegroundColor Yellow
Write-Host ""

try {
    $rule = New-NetFirewallRule `
        -DisplayName "Node.js API Server Port 3000" `
        -Direction Inbound `
        -LocalPort 3000 `
        -Protocol TCP `
        -Action Allow `
        -Profile Domain,Private,Public `
        -Description "Allow inbound TCP connections on port 3000 for Banelo Mobile App API server"

    Write-Host "✅ Firewall rule created successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "   Rule Details:" -ForegroundColor White
    Write-Host "   - Name: Node.js API Server Port 3000" -ForegroundColor Gray
    Write-Host "   - Port: 3000" -ForegroundColor Gray
    Write-Host "   - Protocol: TCP" -ForegroundColor Gray
    Write-Host "   - Direction: Inbound" -ForegroundColor Gray
    Write-Host "   - Action: Allow" -ForegroundColor Gray
    Write-Host "   - Profiles: Domain, Private, Public" -ForegroundColor Gray
    Write-Host ""
} catch {
    Write-Host "❌ Failed to create firewall rule!" -ForegroundColor Red
    Write-Host "   Error: $_" -ForegroundColor Red
    Write-Host ""
    Read-Host "Press Enter to exit"
    exit 1
}

# ============================================
# Step 3: Verify the rule
# ============================================
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "✔️  Step 3: Verifying firewall rule..." -ForegroundColor Yellow
Write-Host ""

$verifyRule = Get-NetFirewallRule -DisplayName "Node.js API Server Port 3000" -ErrorAction SilentlyContinue
if ($verifyRule -and $verifyRule.Enabled -eq $true) {
    Write-Host "✅ Firewall rule is active and enabled!" -ForegroundColor Green
} else {
    Write-Host "⚠️  Warning: Rule exists but may not be enabled" -ForegroundColor Yellow
}
Write-Host ""

# ============================================
# Step 4: Test connectivity
# ============================================
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "🧪 Step 4: Testing connectivity..." -ForegroundColor Yellow
Write-Host ""

Write-Host "   Testing localhost (127.0.0.1:3000)..." -ForegroundColor White
try {
    $localhostTest = Invoke-WebRequest -Uri "http://127.0.0.1:3000/api/users" -TimeoutSec 3 -ErrorAction Stop
    Write-Host "   ✅ Localhost test PASSED" -ForegroundColor Green
} catch {
    if ($_.Exception.Response.StatusCode) {
        Write-Host "   ✅ Server is responding (HTTP $($_.Exception.Response.StatusCode.value__))" -ForegroundColor Green
    } else {
        Write-Host "   ⚠️  Server not responding on localhost" -ForegroundColor Yellow
        Write-Host "      Make sure your Node.js server is running!" -ForegroundColor Yellow
    }
}
Write-Host ""

Write-Host "   Testing from IP address (192.168.254.176:3000)..." -ForegroundColor White
try {
    $ipTest = Invoke-WebRequest -Uri "http://192.168.254.176:3000/api/users" -TimeoutSec 3 -ErrorAction Stop
    Write-Host "   ✅ IP address test PASSED" -ForegroundColor Green
    Write-Host "   ✅ Android device should now be able to connect!" -ForegroundColor Green
} catch {
    if ($_.Exception.Response.StatusCode) {
        Write-Host "   ✅ Server is responding (HTTP $($_.Exception.Response.StatusCode.value__))" -ForegroundColor Green
        Write-Host "   ✅ Android device should now be able to connect!" -ForegroundColor Green
    } else {
        Write-Host "   ⚠️  Could not reach server via IP address" -ForegroundColor Yellow
        Write-Host "      This might be okay - try from your Android device" -ForegroundColor Yellow
    }
}
Write-Host ""

# ============================================
# Step 5: Final instructions
# ============================================
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "📱 Step 5: Testing from Android Device" -ForegroundColor Yellow
Write-Host ""
Write-Host "   Next steps:" -ForegroundColor White
Write-Host "   1. Make sure your Android device is on the SAME WiFi network" -ForegroundColor Cyan
Write-Host "   2. Open Chrome on your Android device" -ForegroundColor Cyan
Write-Host "   3. Go to: http://192.168.254.176:3000/api/users" -ForegroundColor Cyan
Write-Host "   4. You should see JSON data or an API response" -ForegroundColor Cyan
Write-Host ""
Write-Host "   If that works:" -ForegroundColor White
Write-Host "   ✅ Open your Banelo Mobile app" -ForegroundColor Green
Write-Host "   ✅ Try logging in" -ForegroundColor Green
Write-Host "   ✅ The app should now connect to the server!" -ForegroundColor Green
Write-Host ""

Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "✅ Network fix complete!" -ForegroundColor Green
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""

# ============================================
# Troubleshooting tips
# ============================================
Write-Host "💡 Troubleshooting Tips:" -ForegroundColor Yellow
Write-Host ""
Write-Host "   Still not working? Try these:" -ForegroundColor White
Write-Host "   • Restart your Node.js server (Ctrl+C, then run again)" -ForegroundColor Gray
Write-Host "   • Restart your Android device" -ForegroundColor Gray
Write-Host "   • Check if antivirus is blocking connections" -ForegroundColor Gray
Write-Host "   • Verify both devices are on WiFi (not mobile data)" -ForegroundColor Gray
Write-Host "   • Check router settings for 'AP Isolation' (disable it)" -ForegroundColor Gray
Write-Host ""
Write-Host "   For detailed troubleshooting:" -ForegroundColor White
Write-Host "   • Read NETWORK_TROUBLESHOOTING.md in this directory" -ForegroundColor Cyan
Write-Host ""

Read-Host "Press Enter to exit"
