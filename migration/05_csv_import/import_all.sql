-- ============================================================================
-- CSV Bulk Import Script for Banelo PostgreSQL Migration
-- Imports all generated CSV data into PostgreSQL
-- ============================================================================

\echo '========================================='
\echo 'BANELO CSV BULK IMPORT'
\echo '========================================='
\echo ''

-- Ensure we're in the right database
\c banelo_db

-- Disable triggers temporarily for faster import
SET session_replication_role = replica;

\echo 'Step 1: Importing USERS...'
\COPY users(id, firebase_id, fname, lname, mname, username, auth_email, role, status, joined_date, created_at, updated_at) FROM 'users.csv' WITH (FORMAT csv, HEADER true);
\echo '✅ Users imported'
\echo ''

\echo 'Step 2: Importing PRODUCTS...'
\COPY products(id, firebase_id, name, category, price, quantity, inventory_a, inventory_b, cost_per_unit, image_uri, description, sku, is_active, created_at, updated_at) FROM 'products.csv' WITH (FORMAT csv, HEADER true);
\echo '✅ Products imported'
\echo ''

\echo 'Step 3: Importing RECIPES...'
\COPY recipes(id, firebase_id, recipe_id, product_firebase_id, product_name, instructions, prep_time_minutes, cook_time_minutes, servings, created_at, updated_at) FROM 'recipes.csv' WITH (FORMAT csv, HEADER true);
\echo '✅ Recipes imported'
\echo ''

\echo 'Step 4: Importing RECIPE_INGREDIENTS...'
\COPY recipe_ingredients(id, firebase_id, recipe_firebase_id, recipe_id, ingredient_firebase_id, ingredient_name, quantity_needed, unit, created_at) FROM 'recipe_ingredients.csv' WITH (FORMAT csv, HEADER true);
\echo '✅ Recipe ingredients imported'
\echo ''

\echo 'Step 5: Importing SALES...'
\COPY sales(id, firebase_id, order_id, product_name, category, quantity, price, order_date, product_firebase_id, payment_mode, gcash_reference_id, cashier_username, created_at) FROM 'sales.csv' WITH (FORMAT csv, HEADER true);
\echo '✅ Sales imported'
\echo ''

\echo 'Step 6: Importing WASTE_LOGS...'
\COPY waste_logs(id, firebase_id, product_firebase_id, product_name, category, quantity, reason, waste_date, recorded_by, cost_impact, created_at) FROM 'waste_logs.csv' WITH (FORMAT csv, HEADER true);
\echo '✅ Waste logs imported'
\echo ''

-- Re-enable triggers
SET session_replication_role = DEFAULT;

\echo '========================================='
\echo 'VERIFICATION'
\echo '========================================='
\echo ''

-- Count records in each table
\echo 'Record counts:'
SELECT 'users' as table_name, COUNT(*) as count FROM users
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

\echo ''
\echo '========================================='
\echo '✅ IMPORT COMPLETE!'
\echo '========================================='
\echo ''
\echo 'Next steps:'
\echo '  1. Verify data: SELECT * FROM v_product_inventory LIMIT 10;'
\echo '  2. Check sales: SELECT * FROM v_sales_by_product LIMIT 10;'
\echo '  3. Run analytics queries'
\echo ''
