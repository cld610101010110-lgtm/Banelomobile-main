@echo off
setlocal enabledelayedexpansion

color 0A
echo.
echo ========================================
echo    BANELO API - QUICK DIAGNOSTICS
echo ========================================
echo.

REM Check 1: PostgreSQL Service
echo [1/5] Checking PostgreSQL service...
sc query postgresql-x64-18 | find "RUNNING" >nul 2>&1
if %errorlevel%==0 (
    color 0A
    echo      [32m✓[0m PostgreSQL is RUNNING
) else (
    color 0C
    echo      [31m✗[0m PostgreSQL is NOT RUNNING
    echo.
    echo      Fix: Press Windows + R, type 'services.msc'
    echo           Find 'postgresql-x64-18' and click Start
    echo.
)

echo.

REM Check 2: Node.js
echo [2/5] Checking Node.js installation...
where node >nul 2>&1
if %errorlevel%==0 (
    for /f "tokens=*" %%i in ('node --version') do set NODE_VERSION=%%i
    echo      [32m✓[0m Node.js is installed (!NODE_VERSION!)
) else (
    echo      [31m✗[0m Node.js is NOT installed
    echo.
    echo      Install from: https://nodejs.org/
    echo.
)

echo.

REM Check 3: IP Address
echo [3/5] Your computer's IP addresses:
echo.
for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr /i "IPv4"') do (
    set IP=%%a
    set IP=!IP: =!
    echo      • !IP!
)
echo.

REM Check 4: API Server on localhost
echo [4/5] Checking API server on localhost:3000...
curl -s -o nul -w "%%{http_code}" --connect-timeout 3 http://localhost:3000/api/products >nul 2>&1
if %errorlevel%==0 (
    echo      [32m✓[0m API server is RESPONDING on localhost
) else (
    echo      [31m✗[0m API server is NOT responding on localhost
    echo.
    echo      Fix: Open a new terminal and run:
    echo           cd api-backend
    echo           node server.js
    echo.
)

echo.

REM Check 5: Firewall for port 3000
echo [5/5] Checking Windows Firewall...
netstat -an | findstr ":3000" | findstr "LISTENING" >nul 2>&1
if %errorlevel%==0 (
    echo      [32m✓[0m Port 3000 is LISTENING
) else (
    echo      [31m?[0m Port 3000 is not listening
)

echo.
echo ========================================
echo.

REM Check if curl is available for detailed test
where curl >nul 2>&1
if %errorlevel%==0 (
    echo Running detailed API test...
    echo.
    node test-server.js
) else (
    echo For detailed diagnostics, run:
    echo    node test-server.js
)

echo.
echo ========================================
echo  NEXT STEPS:
echo ========================================
echo.
echo 1. Make sure PostgreSQL is running
echo 2. Start the API server: node server.js
echo 3. Note your IP address from above
echo 4. Update BaneloApiService.kt (line 202) with your IP
echo 5. Test from phone's browser: http://YOUR_IP:3000/api/products
echo.

pause
