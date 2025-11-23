@echo off
REM ============================================================================
REM Generate ALL data for PostgreSQL migration (Windows)
REM Run this to create users, products, recipes, sales, and waste logs
REM ============================================================================

echo ================================================================
echo.
echo    BANELO DATA GENERATION - ALL TABLES
echo.
echo ================================================================
echo.

cd migration\03_transform

echo Step 1: Generating USERS...
python generate_users.py
if errorlevel 1 (
    echo [ERROR] Users generation failed!
    pause
    exit /b 1
)
echo.

echo Step 2: Generating PRODUCTS...
python generate_products.py
if errorlevel 1 (
    echo [ERROR] Products generation failed!
    pause
    exit /b 1
)
echo.

echo Step 3: Generating RECIPES...
python generate_recipes.py
if errorlevel 1 (
    echo [ERROR] Recipes generation failed!
    pause
    exit /b 1
)
echo.

echo Step 4: Generating SALES...
python generate_sales.py
if errorlevel 1 (
    echo [ERROR] Sales generation failed!
    pause
    exit /b 1
)
echo.

echo Step 5: Generating WASTE LOGS...
python generate_waste_logs.py
if errorlevel 1 (
    echo [ERROR] Waste logs generation failed!
    pause
    exit /b 1
)
echo.

echo ================================================================
echo.
echo    [SUCCESS] ALL DATA GENERATED!
echo.
echo ================================================================
echo.
echo CSV files created in: migration\05_csv_import\
echo.
echo Files created:
dir ..\05_csv_import\*.csv
echo.
echo Next steps:
echo   1. Import to PostgreSQL (see below)
echo   2. View data in CSV files or database
echo.
pause
