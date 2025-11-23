#!/bin/bash
##############################################################################
# QUICKSTART: Firestore to PostgreSQL Migration
# This runs the ENTIRE migration without downloading Kaggle datasets
# Uses auto-generated sample data (realistic bakery products)
##############################################################################

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║                                                                ║"
echo "║   BANELO MIGRATION QUICKSTART                                  ║"
echo "║   (No Kaggle download needed!)                                 ║"
echo "║                                                                ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Check if PostgreSQL is installed
if ! command -v psql &> /dev/null; then
    echo "❌ PostgreSQL not found!"
    echo ""
    echo "Install it first:"
    echo "  Ubuntu/Debian: sudo apt install postgresql postgresql-contrib"
    echo "  macOS: brew install postgresql"
    echo ""
    exit 1
fi

echo "✅ PostgreSQL found"
echo ""

# Step 1: Install Python requirements
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Step 1: Installing Python dependencies..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
cd migration/01_setup
pip3 install -r requirements.txt -q
echo "✅ Dependencies installed"
echo ""

# Step 2: Setup PostgreSQL database
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Step 2: Setting up PostgreSQL database..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
bash setup_postgres.sh
echo ""

# Step 3: Generate sample data
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Step 3: Generating sample data (no Kaggle needed)..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
cd ../03_transform
python3 transform_dataset.py
echo ""

# Step 4: Create database schema
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Step 4: Creating database tables..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
cd ../02_schema
PGPASSWORD=banelo_password_2024 psql -U banelo_user -d banelo_db -f schema.sql -q
echo "✅ Schema created"
echo ""

# Step 5: Import CSV data
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Step 5: Importing data into PostgreSQL..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
cd ../05_csv_import
PGPASSWORD=banelo_password_2024 psql -U banelo_user -d banelo_db -f import_all.sql
echo ""

# Step 6: Generate ML datasets
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Step 6: Generating ML-ready datasets..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
cd ../06_ml_datasets
python3 generate_ml_datasets.py
echo ""

# Verification
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Verifying migration..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
PGPASSWORD=banelo_password_2024 psql -U banelo_user -d banelo_db -c "
SELECT
    'users' as table_name, COUNT(*) as records FROM users
UNION ALL
SELECT 'products', COUNT(*) FROM products
UNION ALL
SELECT 'recipes', COUNT(*) FROM recipes
UNION ALL
SELECT 'recipe_ingredients', COUNT(*) FROM recipe_ingredients
UNION ALL
SELECT 'sales', COUNT(*) FROM sales
UNION ALL
SELECT 'waste_logs', COUNT(*) FROM waste_logs
ORDER BY table_name;
"

echo ""
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║                                                                ║"
echo "║   ✅ MIGRATION COMPLETE!                                       ║"
echo "║                                                                ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""
echo "🎉 Your PostgreSQL database is ready!"
echo ""
echo "Next steps:"
echo "  1. Test queries: PGPASSWORD=banelo_password_2024 psql -U banelo_user -d banelo_db"
echo "  2. View products: SELECT * FROM products LIMIT 10;"
echo "  3. Check sales: SELECT * FROM v_sales_by_product;"
echo "  4. Update your Android app (see migration/07_android_adapter/)"
echo ""
