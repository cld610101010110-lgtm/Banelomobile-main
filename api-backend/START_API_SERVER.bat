@echo off
color 0B
echo.
echo ========================================
echo   STARTING BANELO API SERVER
echo ========================================
echo.

REM Step 1: Check PostgreSQL
echo [1/3] Checking PostgreSQL...
sc query postgresql-x64-18 | find "RUNNING" >nul 2>&1
if %errorlevel%==0 (
    echo      [32m✓[0m PostgreSQL is running
) else (
    echo      [33m![0m PostgreSQL is not running - attempting to start...
    net start postgresql-x64-18 >nul 2>&1
    if %errorlevel%==0 (
        echo      [32m✓[0m PostgreSQL started successfully
        timeout /t 3 /nobreak >nul
    ) else (
        echo      [31m✗[0m Failed to start PostgreSQL
        echo.
        echo      Please start it manually:
        echo      1. Press Windows + R
        echo      2. Type 'services.msc' and press Enter
        echo      3. Find 'postgresql-x64-18' and click Start
        echo.
        pause
        exit /b 1
    )
)

echo.

REM Step 2: Check Node.js
echo [2/3] Checking Node.js...
where node >nul 2>&1
if %errorlevel%==0 (
    for /f "tokens=*" %%i in ('node --version') do set NODE_VERSION=%%i
    echo      [32m✓[0m Node.js found (!NODE_VERSION!)
) else (
    echo      [31m✗[0m Node.js not found
    echo.
    echo      Please install Node.js from: https://nodejs.org/
    pause
    exit /b 1
)

echo.

REM Step 3: Show IP addresses
echo [3/3] Your computer's IP addresses:
echo.
for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr /i "IPv4"') do (
    set IP=%%a
    set IP=!IP: =!
    echo      • !IP!
)

echo.
echo ========================================
echo   STARTING SERVER...
echo ========================================
echo.
echo The server will start now. You should see:
echo   [32m✓[0m Connected to PostgreSQL database: banelo_db
echo   [32m✓[0m Server running on: http://localhost:3000
echo.
echo Keep this window open while using the app!
echo.
echo ========================================
echo.

REM Start the server
node server.js

REM If server exits, pause so user can see the error
echo.
echo ========================================
echo   SERVER STOPPED
echo ========================================
echo.
pause
