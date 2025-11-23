@echo off
REM ============================================================================
REM QUICKSTART: Firestore to PostgreSQL Migration (WINDOWS VERSION)
REM This runs the ENTIRE migration without downloading Kaggle datasets
REM Uses auto-generated sample data (realistic bakery products)
REM ============================================================================

echo ================================================================
echo.
echo    BANELO MIGRATION QUICKSTART - WINDOWS
echo    (No Kaggle download needed!)
echo.
echo ================================================================
echo.

REM Check if Python is installed
python --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Python not found!
    echo.
    echo Please install Python 3.8+ from: https://www.python.org/downloads/
    echo Make sure to check "Add Python to PATH" during installation
    echo.
    pause
    exit /b 1
)

echo [OK] Python found
echo.

REM Check if PostgreSQL is installed
psql --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] PostgreSQL not found!
    echo.
    echo Please install PostgreSQL from: https://www.postgresql.org/download/windows/
    echo.
    pause
    exit /b 1
)

echo [OK] PostgreSQL found
echo.

REM Step 1: Install Python requirements
echo ================================================================
echo Step 1: Installing Python dependencies...
echo ================================================================
cd migration\01_setup
python -m pip install -r requirements.txt --quiet
if errorlevel 1 (
    echo [ERROR] Failed to install dependencies
    pause
    exit /b 1
)
echo [OK] Dependencies installed
echo.

REM Step 2: Setup PostgreSQL database (manual - requires admin)
echo ================================================================
echo Step 2: Setting up PostgreSQL database...
echo ================================================================
echo.
echo We need to create the database manually on Windows.
echo Please open another Command Prompt as ADMINISTRATOR and run:
echo.
echo   cd "%CD%"
echo   psql -U postgres
echo.
echo Then paste these commands:
echo.
echo   DROP DATABASE IF EXISTS banelo_db;
echo   DROP USER IF EXISTS banelo_user;
echo   CREATE USER banelo_user WITH PASSWORD 'banelo_password_2024';
echo   CREATE DATABASE banelo_db OWNER banelo_user;
echo   GRANT ALL PRIVILEGES ON DATABASE banelo_db TO banelo_user;
echo   \c banelo_db
echo   CREATE EXTENSION IF NOT EXISTS "pgcrypto";
echo   GRANT ALL ON SCHEMA public TO banelo_user;
echo   \q
echo.
echo Press any key after you've done this...
pause >nul

REM Step 3: Generate sample data
echo ================================================================
echo Step 3: Generating sample data (no Kaggle needed)...
echo ================================================================
cd ..\03_transform
python transform_dataset.py
if errorlevel 1 (
    echo [ERROR] Failed to generate data
    pause
    exit /b 1
)
echo.

REM Step 4: Create database schema
echo ================================================================
echo Step 4: Creating database tables...
echo ================================================================
cd ..\02_schema
set PGPASSWORD=banelo_password_2024
psql -U banelo_user -d banelo_db -f schema.sql -q
if errorlevel 1 (
    echo [ERROR] Failed to create schema
    pause
    exit /b 1
)
echo [OK] Schema created
echo.

REM Step 5: Import CSV data
echo ================================================================
echo Step 5: Importing data into PostgreSQL...
echo ================================================================
cd ..\05_csv_import
set PGPASSWORD=banelo_password_2024
psql -U banelo_user -d banelo_db -f import_all.sql
if errorlevel 1 (
    echo [ERROR] Failed to import data
    pause
    exit /b 1
)
echo.

REM Step 6: Generate ML datasets
echo ================================================================
echo Step 6: Generating ML-ready datasets...
echo ================================================================
cd ..\06_ml_datasets
python generate_ml_datasets.py
echo.

REM Verification
echo ================================================================
echo Verifying migration...
echo ================================================================
set PGPASSWORD=banelo_password_2024
psql -U banelo_user -d banelo_db -c "SELECT 'users' as table_name, COUNT(*) as records FROM users UNION ALL SELECT 'products', COUNT(*) FROM products UNION ALL SELECT 'recipes', COUNT(*) FROM recipes UNION ALL SELECT 'recipe_ingredients', COUNT(*) FROM recipe_ingredients UNION ALL SELECT 'sales', COUNT(*) FROM sales UNION ALL SELECT 'waste_logs', COUNT(*) FROM waste_logs ORDER BY table_name;"

echo.
echo ================================================================
echo.
echo    [SUCCESS] MIGRATION COMPLETE!
echo.
echo ================================================================
echo.
echo Your PostgreSQL database is ready!
echo.
echo Next steps:
echo   1. Test queries: set PGPASSWORD=banelo_password_2024 ^&^& psql -U banelo_user -d banelo_db
echo   2. View products: SELECT * FROM products LIMIT 10;
echo   3. Check sales: SELECT * FROM v_sales_by_product;
echo   4. Update your Android app (see migration\07_android_adapter\)
echo.
pause
